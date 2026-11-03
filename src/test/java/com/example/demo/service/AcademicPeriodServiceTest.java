package com.example.demo.service;

import com.example.demo.dto.AcademicPeriodCreateRequest;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.AcademicPeriod;
import com.example.demo.model.AcademicPeriodStatus;
import com.example.demo.model.Enrollment;
import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.Section;
import com.example.demo.model.Student;
import com.example.demo.model.StudentStatus;
import com.example.demo.model.StudyPlan;
import com.example.demo.model.Subject;
import com.example.demo.repository.AcademicPeriodRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.StudentMatriculationRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentMatriculationRepository matriculationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private AcademicPeriodService academicPeriodService;

    private AcademicPeriod period;

    @BeforeEach
    void setUp() {
        period = AcademicPeriod.builder().id(1L).year(2027).semester(2).status(AcademicPeriodStatus.OPEN).build();
    }

    @Test
    void getAllPeriods_mapsPeriodCodeAndStatus() {
        when(academicPeriodRepository.findAll()).thenReturn(List.of(period));

        var result = academicPeriodService.getAllPeriods();

        assertEquals("2027-2", result.get(0).getCode());
        assertEquals(AcademicPeriodStatus.OPEN, result.get(0).getStatus());
    }

    @Test
    void getPeriod_returnsMappedPeriod() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertEquals("2027-2", academicPeriodService.getPeriod(1L).getCode());
    }

    @Test
    void getPeriod_rejectsUnknownId() {
        when(academicPeriodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> academicPeriodService.getPeriod(99L));
    }

    @Test
    void createPeriod_startsOpen() {
        when(academicPeriodRepository.existsByYearAndSemester(2027, 2)).thenReturn(false);
        when(academicPeriodRepository.save(any(AcademicPeriod.class))).thenAnswer(invocation -> {
            AcademicPeriod saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = academicPeriodService
                .createPeriod(AcademicPeriodCreateRequest.builder().year(2027).semester(2).build());

        assertEquals(AcademicPeriodStatus.OPEN, result.getStatus());
        assertEquals("2027-2", result.getCode());
    }

    @Test
    void createPeriod_rejectsDuplicateYearAndSemester() {
        when(academicPeriodRepository.existsByYearAndSemester(2027, 2)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> academicPeriodService.createPeriod(
                AcademicPeriodCreateRequest.builder().year(2027).semester(2).build()));
        verify(academicPeriodRepository, never()).save(any());
    }

    @Test
    void deletePeriod_removesPeriodWithoutAcademicRecords() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(sectionRepository.existsByAcademicPeriodId(1L)).thenReturn(false);
        when(enrollmentRepository.existsByAcademicPeriodId(1L)).thenReturn(false);
        when(matriculationRepository.existsByAcademicPeriodId(1L)).thenReturn(false);

        academicPeriodService.deletePeriod(1L);

        verify(academicPeriodRepository).delete(period);
    }

    @Test
    void deletePeriod_rejectsPeriodWithSections() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(sectionRepository.existsByAcademicPeriodId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.deletePeriod(1L));
        verify(academicPeriodRepository, never()).delete(any());
        verifyNoInteractions(enrollmentRepository, matriculationRepository);
    }

    @Test
    void deletePeriod_rejectsPeriodWithEnrollments() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(sectionRepository.existsByAcademicPeriodId(1L)).thenReturn(false);
        when(enrollmentRepository.existsByAcademicPeriodId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.deletePeriod(1L));
        verify(academicPeriodRepository, never()).delete(any());
    }

    @Test
    void deletePeriod_rejectsPeriodWithMatriculations() {
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(sectionRepository.existsByAcademicPeriodId(1L)).thenReturn(false);
        when(enrollmentRepository.existsByAcademicPeriodId(1L)).thenReturn(false);
        when(matriculationRepository.existsByAcademicPeriodId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.deletePeriod(1L));
        verify(academicPeriodRepository, never()).delete(any());
    }

    @Test
    void closePeriod_rejectsPeriodWithMissingGradesAndReportsSections() {
        Enrollment ungraded = enrollment(50L, "TAP101", null, 2L);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusAndFinalGradeIsNull(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(ungraded));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> academicPeriodService.closePeriod(1L));

        assertTrue(error.getMessage().contains("TAP101 (section 2)"));
        assertEquals(AcademicPeriodStatus.OPEN, period.getStatus());
        verify(academicPeriodRepository, never()).save(any());
        verifyNoInteractions(studentRepository);
    }

    @Test
    void closePeriod_rejectsAlreadyClosedPeriod() {
        period.setStatus(AcademicPeriodStatus.CLOSED);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.closePeriod(1L));
        verifyNoInteractions(enrollmentRepository, studentRepository);
    }

    @Test
    void closePeriod_closesWhenAllGradesAreRecorded() {
        Student student = student(10L, 20L);
        Enrollment passed = enrollment(50L, "TAP101", "4.0", student, 100L, 2L);
        Enrollment failed = enrollment(51L, "TAP102", "3.9", student, 101L, 3L);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusAndFinalGradeIsNull(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of());
        when(enrollmentRepository.findByAcademicPeriodIdAndStatus(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(passed, failed));
        when(enrollmentRepository.countPreviousFailures(10L, 101L, new BigDecimal("4.0"), EnrollmentStatus.ACTIVE, 1L))
                .thenReturn(0L);
        when(enrollmentRepository.findPassedSubjectIds(10L, EnrollmentStatus.ACTIVE, new BigDecimal("4.0")))
                .thenReturn(List.of(100L));
        when(subjectRepository.findByStudyPlanId(20L)).thenReturn(List.of(
                Subject.builder().id(100L).build(), Subject.builder().id(101L).build()));

        var response = academicPeriodService.closePeriod(1L);

        assertEquals(AcademicPeriodStatus.CLOSED, response.getStatus());
        assertEquals(2, response.getGradedEnrollments());
        assertEquals(1, response.getPassedEnrollments());
        assertEquals(1, response.getFailedEnrollments());
        assertEquals(StudentStatus.REGULAR, student.getStatus());
        verify(studentRepository).saveAll(any());
        verify(academicPeriodRepository).save(period);
    }

    @Test
    void closePeriod_expelledAfterSecondFailureOfSameSubject() {
        Student student = student(10L, 20L);
        Enrollment failed = enrollment(50L, "TAP101", "3.9", student, 100L, 2L);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusAndFinalGradeIsNull(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of());
        when(enrollmentRepository.findByAcademicPeriodIdAndStatus(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(failed));
        when(enrollmentRepository.countPreviousFailures(10L, 100L, new BigDecimal("4.0"), EnrollmentStatus.ACTIVE, 1L))
                .thenReturn(1L);

        var response = academicPeriodService.closePeriod(1L);

        assertEquals(StudentStatus.EXPELLED, student.getStatus());
        assertEquals(1, response.getExpelledStudents());
        assertEquals(0, response.getGraduatedStudents());
        verify(subjectRepository, never()).findByStudyPlanId(any());
    }

    @Test
    void closePeriod_graduatesStudentWhenAllPlanSubjectsAreApproved() {
        Student student = student(10L, 20L);
        Enrollment first = enrollment(50L, "TAP101", "4.0", student, 100L, 2L);
        Enrollment second = enrollment(51L, "TAP102", "5.0", student, 101L, 3L);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByAcademicPeriodIdAndStatusAndFinalGradeIsNull(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of());
        when(enrollmentRepository.findByAcademicPeriodIdAndStatus(1L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(enrollmentRepository.findPassedSubjectIds(10L, EnrollmentStatus.ACTIVE, new BigDecimal("4.0")))
                .thenReturn(List.of(100L, 101L));
        when(subjectRepository.findByStudyPlanId(20L)).thenReturn(List.of(
                Subject.builder().id(100L).build(), Subject.builder().id(101L).build()));

        var response = academicPeriodService.closePeriod(1L);

        assertEquals(StudentStatus.GRADUATED, student.getStatus());
        assertEquals(1, response.getGraduatedStudents());
        assertEquals(0, response.getExpelledStudents());
    }

    private Student student(Long studentId, Long studyPlanId) {
        return Student.builder()
                .id(studentId)
                .status(StudentStatus.REGULAR)
                .studyPlan(StudyPlan.builder().id(studyPlanId).build())
                .build();
    }

    private Enrollment enrollment(Long id, String subjectCode, String grade, Long sectionId) {
        Student student = student(10L, 20L);
        return enrollment(id, subjectCode, grade, student, id + 50L, sectionId);
    }

    private Enrollment enrollment(Long id, String subjectCode, String grade,
            Student student, Long subjectId, Long sectionId) {
        Section section = Section.builder().id(sectionId).build();
        Subject subject = Subject.builder().id(subjectId).code(subjectCode).build();
        section.setSubject(subject);
        return Enrollment.builder()
                .id(id)
                .student(student)
                .subject(subject)
                .section(section)
                .academicPeriod(period)
                .status(EnrollmentStatus.ACTIVE)
                .finalGrade(grade == null ? null : new BigDecimal(grade))
                .build();
    }
}