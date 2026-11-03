package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final BigDecimal PASSING_GRADE = new BigDecimal("4.0");

    private final EnrollmentRepository enrollmentRepository;
    private final StudentMatriculationRepository matriculationRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public void setMatriculation(Long periodId, MatriculationRequest request) {
        AcademicPeriod period = findPeriod(periodId);
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + request.getStudentId()));
        StudentMatriculation matriculation = matriculationRepository
                .findByStudentIdAndAcademicPeriodId(student.getId(), periodId)
                .orElseGet(() -> StudentMatriculation.builder()
                        .student(student)
                        .academicPeriod(period)
                        .build());
        matriculation.setMatriculated(request.getMatriculated());
        matriculationRepository.save(matriculation);
    }

    @Transactional(readOnly = true)
    public List<SectionOfferingResponse> getOffer(Long studentId, Long periodId) {
        Student student = findStudent(studentId);
        findPeriod(periodId);
        return sectionRepository.findByAcademicPeriodId(periodId).stream()
                .filter(section -> section.getSubject().getStudyPlan().getId().equals(student.getStudyPlan().getId()))
                .map(section -> {
                    long enrolled = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVE);
                    return SectionOfferingResponse.builder()
                            .section(mapSection(section))
                            .enrolledCount(enrolled)
                            .availableSeats(Math.max(0, section.getCapacity() - enrolled))
                            .build();
                })
                .toList();
    }

    @Transactional
    public List<EnrollmentResponse> enroll(Long periodId, EnrollmentCreateRequest request) {
        AcademicPeriod period = findOpenPeriod(periodId);
        Student student = findStudent(request.getStudentId());
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new BusinessRuleException("Only REGULAR students can enroll in academic subjects.");
        }
        if (!matriculationRepository.existsByStudentIdAndAcademicPeriodIdAndMatriculatedTrue(student.getId(), periodId)) {
            throw new BusinessRuleException("Student must be matriculated in the academic period before enrolling.");
        }

        List<Long> requestedIds = request.getSectionIds();
        if (requestedIds == null || requestedIds.isEmpty() || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new BusinessRuleException("Select one or more distinct sections.");
        }

        List<Section> sections = requestedIds.stream()
                .map(this::findSection)
                .toList();
        for (Section section : sections) {
            if (!section.getAcademicPeriod().getId().equals(periodId)) {
                throw new BusinessRuleException("Every selected section must belong to the requested academic period.");
            }
            if (!section.getSubject().getStudyPlan().getId().equals(student.getStudyPlan().getId())) {
                throw new BusinessRuleException("Students can only enroll in subjects from their assigned study plan.");
            }
        }

        validateSelectionSize(student, sections, periodId);
        validateNoScheduleConflicts(sections, enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId));

        List<Enrollment> enrollments = new ArrayList<>();
        Set<Long> selectedSubjectIds = new HashSet<>();
        for (Section section : sections) {
            Subject subject = section.getSubject();
            if (!selectedSubjectIds.add(subject.getId())) {
                throw new BusinessRuleException("A subject cannot be enrolled more than once in the same period.");
            }
            validateSubjectEligibility(student, subject, periodId);

            long occupied = enrollmentRepository.countBySectionIdAndStatus(section.getId(), EnrollmentStatus.ACTIVE);
            Enrollment existing = enrollmentRepository
                    .findByStudentIdAndSubjectIdAndAcademicPeriodId(student.getId(), subject.getId(), periodId)
                    .orElse(null);
            if (existing != null && existing.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new BusinessRuleException("Subject " + subject.getCode() + " is already enrolled in this period.");
            }
            if (existing == null && occupied >= section.getCapacity()) {
                throw new BusinessRuleException("Section " + subject.getCode() + " has no available seats.");
            }

            Enrollment enrollment = existing != null ? existing : Enrollment.builder()
                    .student(student)
                    .subject(subject)
                    .academicPeriod(period)
                    .build();
            enrollment.setSection(section);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            if (enrollment.getEnrolledAt() == null) {
                enrollment.setEnrolledAt(LocalDateTime.now());
            }
            enrollment.setCancelledAt(null);
            enrollment.setFinalGrade(null);
            enrollments.add(enrollment);
        }

        enrollmentRepository.saveAll(enrollments);
        return enrollments.stream().map(this::mapEnrollment).toList();
    }

    @Transactional
    public EnrollmentResponse cancel(Long periodId, Long studentId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndStudentId(enrollmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId + " for student " + studentId));
        if (!enrollment.getAcademicPeriod().getId().equals(periodId)) {
            throw new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId + " in academic period " + periodId);
        }
        findOpenPeriod(enrollment.getAcademicPeriod().getId());
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("Only active enrollments can be cancelled.");
        }
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setCancelledAt(LocalDateTime.now());
        enrollment.setFinalGrade(null);
        return mapEnrollment(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollments(Long studentId, Long periodId) {
        findStudent(studentId);
        findPeriod(periodId);
        return enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId).stream()
                .map(this::mapEnrollment)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getAcademicHistory(Long studentId) {
        findStudent(studentId);
        return enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).stream()
                .filter(enrollment -> enrollment.getFinalGrade() != null)
                .map(this::mapEnrollment)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getSectionEnrollments(Long periodId, Long sectionId, Long teacherId) {
        verifyTeacherOwnsSection(periodId, sectionId, teacherId);
        return enrollmentRepository.findByAcademicPeriodIdAndSectionIdAndStatus(
                        periodId, sectionId, EnrollmentStatus.ACTIVE).stream()
                .map(this::mapEnrollment)
                .toList();
    }

    @Transactional
    public EnrollmentResponse recordFinalGrade(Long periodId, Long sectionId, Long enrollmentId, FinalGradeRequest request) {
        findOpenPeriod(periodId);
        verifyTeacherOwnsSection(periodId, sectionId, request.getTeacherId());
        Enrollment enrollment = enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(enrollmentId, periodId, sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found in the specified section and period."));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("Grades can only be recorded for active enrollments.");
        }
        BigDecimal grade = request.getGrade();
        if (grade == null || grade.compareTo(BigDecimal.ONE) < 0 || grade.compareTo(new BigDecimal("7.0")) > 0
                || grade.scale() > 1) {
            throw new BusinessRuleException("Final grade must be between 1.0 and 7.0 with at most one decimal place.");
        }
        enrollment.setFinalGrade(grade);
        return mapEnrollment(enrollmentRepository.save(enrollment));
    }

    private void validateSelectionSize(Student student, List<Section> sections, Long periodId) {
        Set<Long> passedIds = new HashSet<>(enrollmentRepository.findPassedSubjectIds(
                student.getId(), EnrollmentStatus.ACTIVE, PASSING_GRADE));
        long remaining = subjectRepository.findByStudyPlanId(student.getStudyPlan().getId()).stream()
                .filter(subject -> !passedIds.contains(subject.getId()))
                .count();
        int minimum = remaining < 3 ? 1 : 3;
        Set<Long> activeSubjectIds = enrollmentRepository.findByStudentIdAndAcademicPeriodId(student.getId(), periodId).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(enrollment -> enrollment.getSubject().getId())
                .collect(Collectors.toSet());
        Set<Long> selectedIds = sections.stream().map(section -> section.getSubject().getId()).collect(Collectors.toSet());
        selectedIds.addAll(activeSubjectIds);
        if (selectedIds.size() < minimum || selectedIds.size() > 6) {
            throw new BusinessRuleException("Students must have " + minimum + " to 6 active subjects per period; selection would result in " + selectedIds.size() + ".");
        }

        Integer lowestUnselectedSemester = sectionRepository.findByAcademicPeriodId(periodId).stream()
                .filter(section -> section.getSubject().getStudyPlan().getId().equals(student.getStudyPlan().getId()))
                .filter(section -> !passedIds.contains(section.getSubject().getId()))
            .filter(section -> !selectedIds.contains(section.getSubject().getId()))
            .filter(section -> section.getSubject().getPrerequisites().stream().allMatch(prerequisite -> passedIds.contains(prerequisite.getId())))
                .map(section -> section.getSubject().getSemester())
                .min(Integer::compareTo)
                .orElse(null);
        if (lowestUnselectedSemester != null && sections.stream()
                .anyMatch(section -> section.getSubject().getSemester() > lowestUnselectedSemester)) {
            throw new BusinessRuleException("A student cannot enroll in a higher semester while lower-semester subjects remain available.");
        }
    }

    private void validateSubjectEligibility(Student student, Subject subject, Long periodId) {
        if (enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeGreaterThanEqualAndStatus(
                student.getId(), subject.getId(), PASSING_GRADE, EnrollmentStatus.ACTIVE) > 0) {
            throw new BusinessRuleException("Approved subject " + subject.getCode() + " cannot be enrolled again.");
        }
        if (enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeLessThanAndStatus(
                student.getId(), subject.getId(), PASSING_GRADE, EnrollmentStatus.ACTIVE) >= 2) {
            throw new BusinessRuleException("Subject " + subject.getCode() + " has already been failed twice.");
        }
        for (Subject prerequisite : subject.getPrerequisites()) {
            if (enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeGreaterThanEqualAndStatus(
                    student.getId(), prerequisite.getId(), PASSING_GRADE, EnrollmentStatus.ACTIVE) == 0) {
                throw new BusinessRuleException("Prerequisite " + prerequisite.getCode() + " has not been approved.");
            }
        }
        if (!subjectRepository.findByStudyPlanId(student.getStudyPlan().getId()).stream()
                .anyMatch(planSubject -> planSubject.getId().equals(subject.getId()))) {
            throw new BusinessRuleException("Subject does not belong to the student's study plan.");
        }
        if (sectionRepository.findByAcademicPeriodId(periodId).stream()
                .noneMatch(section -> section.getSubject().getId().equals(subject.getId()))) {
            throw new BusinessRuleException("Subject is not offered in this academic period.");
        }
    }

    private void validateNoScheduleConflicts(List<Section> selected, List<Enrollment> existingEnrollments) {
        List<ScheduleBlock> occupied = existingEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .flatMap(enrollment -> enrollment.getSection().getScheduleBlocks().stream())
                .collect(Collectors.toCollection(ArrayList::new));
        for (Section section : selected) {
            for (ScheduleBlock block : section.getScheduleBlocks()) {
                if (occupied.contains(block)) {
                    throw new BusinessRuleException("Selected sections have conflicting schedules at " + block.getDayCode() + block.getModule() + ".");
                }
                occupied.add(block);
            }
        }
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
    }

    private AcademicPeriod findPeriod(Long periodId) {
        return academicPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found with ID: " + periodId));
    }

    private AcademicPeriod findOpenPeriod(Long periodId) {
        AcademicPeriod period = findPeriod(periodId);
        if (period.getStatus() != AcademicPeriodStatus.OPEN) {
            throw new BusinessRuleException("Enrollments and cancellations are only allowed while the academic period is OPEN.");
        }
        return period;
    }

    private Section findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));
    }

    private Section verifyTeacherOwnsSection(Long periodId, Long sectionId, Long teacherId) {
        Section section = sectionRepository.findById(sectionId)
                .filter(value -> value.getAcademicPeriod().getId().equals(periodId))
                .orElseThrow(() -> new ResourceNotFoundException("Section not found in academic period " + periodId + "."));
        if (!section.getTeacher().getId().equals(teacherId)) {
            throw new BusinessRuleException("Only the teacher assigned to the section can view its roster or record grades.");
        }
        return section;
    }

    private EnrollmentResponse mapEnrollment(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentRun(enrollment.getStudent().getRun())
                .studentFullName(enrollment.getStudent().getFullName())
                .subjectId(enrollment.getSubject().getId())
                .subjectCode(enrollment.getSubject().getCode())
                .subjectName(enrollment.getSubject().getName())
                .subjectSemester(enrollment.getSubject().getSemester())
                .sectionId(enrollment.getSection().getId())
                .academicPeriodId(enrollment.getAcademicPeriod().getId())
                .academicPeriodCode(enrollment.getAcademicPeriod().getYear() + "-" + enrollment.getAcademicPeriod().getSemester())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .cancelledAt(enrollment.getCancelledAt())
                .finalGrade(enrollment.getFinalGrade())
                .result(enrollment.getFinalGrade() == null ? null
                    : enrollment.getFinalGrade().compareTo(PASSING_GRADE) >= 0 ? EnrollmentResult.PASSED : EnrollmentResult.FAILED)
                .build();
    }

    private SectionResponse mapSection(Section section) {
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
                .scheduleBlocks(section.getScheduleBlocks().stream()
                        .map(block -> new ScheduleBlockRequest(block.getDayCode(), block.getModule()))
                        .toList())
                .build();
    }
}