package com.example.demo.service;

import com.example.demo.dto.ScheduleBlockRequest;
import com.example.demo.dto.SectionCreateRequest;
import com.example.demo.dto.SectionResponse;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.AcademicPeriodRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService {

    private static final Set<String> VALID_DAYS = Set.of("L", "M", "W", "J", "V");

    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<SectionResponse> getSections(Long periodId) {
        verifyPeriodExists(periodId);
        return sectionRepository.findByAcademicPeriodId(periodId).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByTeacher(Long teacherId) {
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + teacherId));
        return sectionRepository.findByTeacherId(teacherId).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse getSection(Long periodId, Long sectionId) {
        return mapToResponse(findSection(periodId, sectionId));
    }

    @Transactional
    public SectionResponse createSection(Long periodId, SectionCreateRequest request) {
        AcademicPeriod period = verifyPeriodIsOpen(periodId);
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + request.getTeacherId()));

        if (sectionRepository.existsBySubjectIdAndAcademicPeriodId(subject.getId(), periodId)) {
            throw new DuplicateResourceException("Subject already has a section in academic period " + period.getYear() + "-" + period.getSemester() + ".");
        }
        validateTeacherActive(teacher);
        Set<ScheduleBlock> blocks = resolveAndValidateSchedule(subject, teacher.getId(), periodId, null, request.getScheduleBlocks());

        Section section = Section.builder()
                .subject(subject)
                .academicPeriod(period)
                .teacher(teacher)
                .capacity(request.getCapacity())
                .scheduleBlocks(blocks)
                .build();
        return mapToResponse(sectionRepository.save(section));
    }

    @Transactional
    public SectionResponse updateSection(Long periodId, Long sectionId, SectionCreateRequest request) {
        Section section = findSection(periodId, sectionId);
        verifyPeriodIsOpen(periodId);
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + request.getTeacherId()));

        if (sectionRepository.existsBySubjectIdAndAcademicPeriodId(subject.getId(), periodId)
                && !section.getSubject().getId().equals(subject.getId())) {
            throw new DuplicateResourceException("Subject already has a section in this academic period.");
        }
        validateTeacherActive(teacher);
        if (request.getCapacity() < enrollmentRepository.countBySectionIdAndStatus(sectionId, EnrollmentStatus.ACTIVE)) {
            throw new BusinessRuleException("Section capacity cannot be lower than its current enrollment count.");
        }
        Set<ScheduleBlock> blocks = resolveAndValidateSchedule(subject, teacher.getId(), periodId, sectionId, request.getScheduleBlocks());

        section.setSubject(subject);
        section.setTeacher(teacher);
        section.setCapacity(request.getCapacity());
        section.setScheduleBlocks(blocks);
        return mapToResponse(sectionRepository.save(section));
    }

    @Transactional
    public void deleteSection(Long periodId, Long sectionId) {
        Section section = findSection(periodId, sectionId);
        verifyPeriodIsOpen(periodId);
        if (enrollmentRepository.existsBySectionId(sectionId)) {
            throw new BusinessRuleException("Section cannot be physically deleted because it has enrollment history.");
        }
        sectionRepository.delete(section);
    }

    @Transactional(readOnly = true)
    public Section findSection(Long periodId, Long sectionId) {
        verifyPeriodExists(periodId);
        return sectionRepository.findById(sectionId)
                .filter(section -> section.getAcademicPeriod().getId().equals(periodId))
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId + " in academic period " + periodId));
    }

    private AcademicPeriod verifyPeriodIsOpen(Long periodId) {
        AcademicPeriod period = verifyPeriodExists(periodId);
        if (period.getStatus() != AcademicPeriodStatus.OPEN) {
            throw new BusinessRuleException("Sections can only be managed while the academic period is OPEN.");
        }
        return period;
    }

    private AcademicPeriod verifyPeriodExists(Long periodId) {
        return academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found with ID: " + periodId));
    }

    private void validateTeacherActive(Teacher teacher) {
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE teachers can be assigned to new sections.");
        }
    }

    private Set<ScheduleBlock> resolveAndValidateSchedule(
            Subject subject,
            Long teacherId,
            Long periodId,
            Long sectionId,
            List<ScheduleBlockRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException("A section must have at least one schedule block.");
        }
        if (subject.getTotalWeeklyHours() % 2 != 0 || requests.size() != subject.getTotalWeeklyHours() / 2) {
            throw new BusinessRuleException("The number of schedule blocks must equal half of the subject's total weekly TEL hours.");
        }

        Set<ScheduleBlock> blocks = new HashSet<>();
        for (ScheduleBlockRequest request : requests) {
            String day = request.getDayCode() == null ? "" : request.getDayCode().trim().toUpperCase();
            Integer module = request.getModule();
            if (!VALID_DAYS.contains(day) || module == null || module < 1 || module > 6) {
                throw new BusinessRuleException("Schedule blocks require a valid day (L, M, W, J, V) and module (1-6).");
            }
            ScheduleBlock block = new ScheduleBlock(day, module);
            if (!blocks.add(block)) {
                throw new BusinessRuleException("A section cannot have duplicate schedule blocks.");
            }
        }

        for (ScheduleBlock block : blocks) {
            if (sectionRepository.existsScheduleConflict(
                    teacherId,
                    periodId,
                    AcademicPeriodStatus.OPEN,
                    block.getDayCode(),
                    block.getModule(),
                    sectionId)) {
                throw new BusinessRuleException("The teacher already has a section scheduled for " + block.getDayCode() + block.getModule() + ".");
            }
        }
        return blocks;
    }

    private SectionResponse mapToResponse(Section section) {
        List<ScheduleBlockRequest> blocks = section.getScheduleBlocks().stream()
                .map(block -> new ScheduleBlockRequest(block.getDayCode(), block.getModule()))
                .sorted((first, second) -> {
                    int dayOrder = first.getDayCode().compareTo(second.getDayCode());
                    return dayOrder != 0 ? dayOrder : first.getModule().compareTo(second.getModule());
                })
                .collect(Collectors.toList());

        return SectionResponse.builder()
                .id(section.getId())
                .subjectId(section.getSubject().getId())
                .subjectCode(section.getSubject().getCode())
                .subjectName(section.getSubject().getName())
                .academicPeriodId(section.getAcademicPeriod().getId())
                .academicPeriodCode(section.getAcademicPeriod().getYear() + "-" + section.getAcademicPeriod().getSemester())
                .academicPeriodStatus(section.getAcademicPeriod().getStatus())
                .teacherId(section.getTeacher().getId())
                .teacherName(section.getTeacher().getFullName())
                .capacity(section.getCapacity())
                .scheduleBlocks(blocks)
                .build();
    }
}