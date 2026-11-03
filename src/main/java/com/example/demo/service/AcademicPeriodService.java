package com.example.demo.service;

import com.example.demo.dto.AcademicPeriodCreateRequest;
import com.example.demo.dto.AcademicPeriodClosureResponse;
import com.example.demo.dto.AcademicPeriodResponse;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.AcademicPeriod;
import com.example.demo.model.AcademicPeriodStatus;
import com.example.demo.model.Enrollment;
import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.Student;
import com.example.demo.model.StudentStatus;
import com.example.demo.repository.AcademicPeriodRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.StudentMatriculationRepository;
import com.example.demo.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentMatriculationRepository matriculationRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<AcademicPeriodResponse> getAllPeriods() {
        return academicPeriodRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public AcademicPeriodResponse getPeriod(Long id) {
        return mapToResponse(findPeriod(id));
    }

    @Transactional
    public AcademicPeriodResponse createPeriod(AcademicPeriodCreateRequest request) {
        if (academicPeriodRepository.existsByYearAndSemester(request.getYear(), request.getSemester())) {
            throw new DuplicateResourceException("Academic period " + request.getYear() + "-" + request.getSemester() + " already exists.");
        }

        AcademicPeriod period = AcademicPeriod.builder()
                .year(request.getYear())
                .semester(request.getSemester())
                .status(AcademicPeriodStatus.OPEN)
                .build();
        return mapToResponse(academicPeriodRepository.save(period));
    }

    @Transactional(readOnly = true)
    public AcademicPeriod findPeriod(Long id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found with ID: " + id));
    }

    @Transactional
    public void deletePeriod(Long id) {
        AcademicPeriod period = findPeriod(id);
        if (sectionRepository.existsByAcademicPeriodId(id)
                || enrollmentRepository.existsByAcademicPeriodId(id)
                || matriculationRepository.existsByAcademicPeriodId(id)) {
            throw new BusinessRuleException("Academic period cannot be deleted while it has sections, enrollments, or matriculations.");
        }
        academicPeriodRepository.delete(period);
    }

    @Transactional
    public AcademicPeriodClosureResponse closePeriod(Long id) {
        AcademicPeriod period = findPeriod(id);
        if (period.getStatus() != AcademicPeriodStatus.OPEN) {
            throw new BusinessRuleException("Only an OPEN academic period can be closed.");
        }

        List<Enrollment> missingGrades = enrollmentRepository.findByAcademicPeriodIdAndStatusAndFinalGradeIsNull(
                id, EnrollmentStatus.ACTIVE);
        if (!missingGrades.isEmpty()) {
            String sectionDetails = missingGrades.stream()
                    .map(enrollment -> enrollment.getSubject().getCode() + " (section " + enrollment.getSection().getId() + ")")
                    .distinct()
                    .sorted()
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
            throw new BusinessRuleException("Cannot close period because these sections have enrollments without final grades: " + sectionDetails);
        }

        List<Enrollment> periodEnrollments = enrollmentRepository.findByAcademicPeriodIdAndStatus(id, EnrollmentStatus.ACTIVE);
        BigDecimal passingGrade = new BigDecimal("4.0");
        int passedCount = 0;
        int failedCount = 0;
        Set<Long> expelledStudentIds = new HashSet<>();
        Map<Long, Student> affectedStudents = new HashMap<>();

        for (Enrollment enrollment : periodEnrollments) {
            Student student = enrollment.getStudent();
            affectedStudents.put(student.getId(), student);
            if (enrollment.getFinalGrade().compareTo(passingGrade) >= 0) {
                passedCount++;
            } else {
                failedCount++;
                if (enrollmentRepository.countPreviousFailures(
                        student.getId(), enrollment.getSubject().getId(), passingGrade,
                        EnrollmentStatus.ACTIVE, id) > 0) {
                    expelledStudentIds.add(student.getId());
                }
            }
        }

        int expelledCount = 0;
        int graduatedCount = 0;
        for (Student student : affectedStudents.values()) {
            if (expelledStudentIds.contains(student.getId())) {
                student.setStatus(StudentStatus.EXPELLED);
                expelledCount++;
                continue;
            }

            Set<Long> passedSubjectIds = new HashSet<>(enrollmentRepository.findPassedSubjectIds(
                    student.getId(), EnrollmentStatus.ACTIVE, passingGrade));
            List<Long> studyPlanSubjectIds = subjectRepository.findByStudyPlanId(student.getStudyPlan().getId()).stream()
                    .map(subject -> subject.getId())
                    .toList();
            if (!studyPlanSubjectIds.isEmpty() && passedSubjectIds.containsAll(studyPlanSubjectIds)) {
                student.setStatus(StudentStatus.GRADUATED);
                graduatedCount++;
            }
        }

        studentRepository.saveAll(affectedStudents.values());
        period.setStatus(AcademicPeriodStatus.CLOSED);
        academicPeriodRepository.save(period);

        return AcademicPeriodClosureResponse.builder()
                .academicPeriodId(period.getId())
                .academicPeriodCode(period.getYear() + "-" + period.getSemester())
                .status(period.getStatus())
                .gradedEnrollments(periodEnrollments.size())
                .passedEnrollments(passedCount)
                .failedEnrollments(failedCount)
                .expelledStudents(expelledCount)
                .graduatedStudents(graduatedCount)
                .build();
    }

    private AcademicPeriodResponse mapToResponse(AcademicPeriod period) {
        return AcademicPeriodResponse.builder()
                .id(period.getId())
                .year(period.getYear())
                .semester(period.getSemester())
                .code(period.getYear() + "-" + period.getSemester())
                .status(period.getStatus())
                .build();
    }
}