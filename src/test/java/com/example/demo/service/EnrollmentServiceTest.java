package com.example.demo.service;

import com.example.demo.dto.EnrollmentCreateRequest;
import com.example.demo.dto.EnrollmentResponse;
import com.example.demo.dto.FinalGradeRequest;
import com.example.demo.dto.MatriculationRequest;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentMatriculationRepository matriculationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private AcademicPeriod period;
    private Student student;
    private StudyPlan plan;

    @BeforeEach
    void setUp() {
        period = AcademicPeriod.builder().id(1L).year(2027).semester(1).status(AcademicPeriodStatus.OPEN).build();
        plan = StudyPlan.builder().id(9L).code("PLAN-1").build();
        student = Student.builder().id(2L).status(StudentStatus.POSTPONED).studyPlan(plan).build();
        lenient().when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        lenient().when(studentRepository.findById(2L)).thenReturn(Optional.of(student));
    }

    @Test
    void enroll_rejectsStudentWhoIsNotRegular() {
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build();

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L, request));
        verify(enrollmentRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void enroll_rejectsUnmatriculatedStudent() {
        student.setStatus(StudentStatus.REGULAR);
        when(matriculationRepository.existsByStudentIdAndAcademicPeriodIdAndMatriculatedTrue(2L, 1L)).thenReturn(false);
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build();

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L, request));
        verify(enrollmentRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void enroll_rejectsClosedPeriod() {
        period.setStatus(AcademicPeriodStatus.CLOSED);
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build();

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L, request));
        verify(enrollmentRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void setMatriculation_createsNewPeriodRegistration() {
        student.setStatus(StudentStatus.REGULAR);
        when(matriculationRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(Optional.empty());

        enrollmentService.setMatriculation(1L, new MatriculationRequest(2L, true));

        verify(matriculationRepository).save(argThat(value -> value.getMatriculated() && value.getStudent().equals(student)));
    }

    @Test
    void setMatriculation_updatesExistingRegistration() {
        StudentMatriculation existing = StudentMatriculation.builder().id(5L).student(student).academicPeriod(period).matriculated(false).build();
        when(matriculationRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(Optional.of(existing));

        enrollmentService.setMatriculation(1L, new MatriculationRequest(2L, true));

        assertTrue(existing.getMatriculated());
        verify(matriculationRepository).save(existing);
    }

    @Test
    void setMatriculation_rejectsMissingStudent() {
        when(studentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.setMatriculation(1L, new MatriculationRequest(2L, true)));
        verify(matriculationRepository, never()).save(any());
    }

    @Test
    void getOffer_filtersByPlanAndCalculatesAvailableSeats() {
        List<Section> sections = fixtureSections(1);
        sections.get(0).setCapacity(20);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVE)).thenReturn(7L);
        Section otherPlanSection = fixtureSections(1).get(0);
        otherPlanSection.getSubject().setStudyPlan(StudyPlan.builder().id(77L).build());
        when(sectionRepository.findByAcademicPeriodId(1L)).thenReturn(List.of(sections.get(0), otherPlanSection));

        var offers = enrollmentService.getOffer(2L, 1L);

        assertEquals(1, offers.size());
        assertEquals(7L, offers.get(0).getEnrolledCount());
        assertEquals(13L, offers.get(0).getAvailableSeats());
        assertEquals("2027-1", offers.get(0).getSection().getAcademicPeriodCode());
    }

    @Test
    void getOffer_capsAvailableSeatsAtZero() {
        Section section = fixtureSections(1).get(0);
        section.setCapacity(2);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));
        when(sectionRepository.findByAcademicPeriodId(1L)).thenReturn(List.of(section));
        when(enrollmentRepository.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVE)).thenReturn(3L);

        assertEquals(0L, enrollmentService.getOffer(2L, 1L).get(0).getAvailableSeats());
    }

    @Test
    void getOffer_rejectsMissingStudent() {
        when(studentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.getOffer(2L, 1L));
    }

    @Test
    void enroll_savesValidBatchAndMapsResponses() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<EnrollmentResponse> result = enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build());

        assertEquals(3, result.size());
        assertEquals("SUBJ1", result.get(0).getSubjectCode());
        assertEquals("2027-1", result.get(0).getAcademicPeriodCode());
        assertNotNull(result.get(0).getEnrolledAt());
        verify(enrollmentRepository).saveAll(anyList());
    }

    @Test
    void enroll_reactivatesCancelledEnrollmentWithoutChangingOriginalEnrollmentTime() {
        student.setStatus(StudentStatus.REGULAR);
        Subject subject = fixtureSubjects(1).get(0);
        Section section = fixtureSections(List.of(subject)).get(0);
        List<Enrollment> existingForPeriod = List.of();
        stubValidEnrollment(List.of(subject), List.of(section));
        Enrollment cancelled = Enrollment.builder()
                .id(20L).student(student).subject(subject).section(section).academicPeriod(period)
                .status(EnrollmentStatus.CANCELLED).enrolledAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .cancelledAt(LocalDateTime.now()).finalGrade(new BigDecimal("3.5")).build();
        when(enrollmentRepository.findByStudentIdAndSubjectIdAndAcademicPeriodId(2L, subject.getId(), 1L)).thenReturn(Optional.of(cancelled));
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime originalDate = cancelled.getEnrolledAt();

        List<EnrollmentResponse> result = enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build());

        assertEquals(EnrollmentStatus.ACTIVE, cancelled.getStatus());
        assertEquals(originalDate, cancelled.getEnrolledAt());
        assertNull(cancelled.getCancelledAt());
        assertNull(cancelled.getFinalGrade());
        assertEquals(EnrollmentStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void enroll_rejectsDuplicateSectionIds() {
        student.setStatus(StudentStatus.REGULAR);
        when(matriculationRepository.existsByStudentIdAndAcademicPeriodIdAndMatriculatedTrue(2L, 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 10L)).build()));
    }

    @Test
    void enroll_rejectsSectionFromDifferentPeriod() {
        student.setStatus(StudentStatus.REGULAR);
        Section section = fixtureSections(1).get(0);
        section.getAcademicPeriod().setId(88L);
        stubBeforeSelection(List.of(section));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build()));
    }

    @Test
    void enroll_rejectsSectionOutsideStudentsStudyPlan() {
        student.setStatus(StudentStatus.REGULAR);
        Section section = fixtureSections(1).get(0);
        section.getSubject().getStudyPlan().setId(88L);
        stubBeforeSelection(List.of(section));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build()));
    }

    @Test
    void enroll_rejectsSelectionBelowMinimum() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubBeforeSelection(sections);
        when(enrollmentRepository.findPassedSubjectIds(2L, EnrollmentStatus.ACTIVE, new BigDecimal("4.0"))).thenReturn(List.of());
        when(subjectRepository.findByStudyPlanId(9L)).thenReturn(subjects);
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(List.of());

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L)).build()));
    }

    @Test
    void enroll_rejectsSelectionAboveMaximum() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(7);
        List<Section> sections = fixtureSections(subjects);
        stubBeforeSelection(sections);
        when(enrollmentRepository.findPassedSubjectIds(2L, EnrollmentStatus.ACTIVE, new BigDecimal("4.0"))).thenReturn(List.of());
        when(subjectRepository.findByStudyPlanId(9L)).thenReturn(subjects);
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(List.of());

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L, 13L, 14L, 15L, 16L)).build()));
    }

    @Test
    void enroll_rejectsHigherSemesterWhileLowerAvailable() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(4);
        subjects.get(0).setSemester(1);
        subjects.get(1).setSemester(2);
        subjects.get(2).setSemester(2);
        subjects.get(3).setSemester(2);
        List<Section> sections = fixtureSections(subjects);
        stubBeforeSelection(sections);
        when(enrollmentRepository.findPassedSubjectIds(2L, EnrollmentStatus.ACTIVE, new BigDecimal("4.0"))).thenReturn(List.of());
        when(subjectRepository.findByStudyPlanId(9L)).thenReturn(subjects);
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(List.of());
        when(sectionRepository.findByAcademicPeriodId(1L)).thenReturn(sections);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(11L, 12L, 13L)).build()));
    }

    @Test
    void enroll_rejectsScheduleConflictBetweenSelectedSections() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        sections.get(1).setScheduleBlocks(Set.of(new ScheduleBlock("L", 1)));
        stubValidEnrollment(subjects, sections);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsFullSection() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        when(enrollmentRepository.countBySectionIdAndStatus(10L, EnrollmentStatus.ACTIVE)).thenReturn(30L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsPreviouslyApprovedSubject() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        when(enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeGreaterThanEqualAndStatus(
                2L, 101L, new BigDecimal("4.0"), EnrollmentStatus.ACTIVE)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsSubjectFailedTwice() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        when(enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeLessThanAndStatus(
                2L, 101L, new BigDecimal("4.0"), EnrollmentStatus.ACTIVE)).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsUnapprovedPrerequisite() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        subjects.get(0).setPrerequisites(new HashSet<>(Set.of(Subject.builder().id(99L).code("PRE99").build())));
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        when(enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeGreaterThanEqualAndStatus(
                2L, 99L, new BigDecimal("4.0"), EnrollmentStatus.ACTIVE)).thenReturn(0L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsSubjectNotInPlan() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(4);
        List<Section> sections = fixtureSections(subjects.subList(1, 4));
        sections.get(0).getSubject().setId(999L);
        stubValidEnrollment(subjects, sections);
        when(subjectRepository.findByStudyPlanId(9L)).thenReturn(List.of(subjects.get(0), subjects.get(3)));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsSubjectNotOfferedDuringPeriod() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        when(sectionRepository.findByAcademicPeriodId(1L)).thenReturn(List.of());

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void enroll_rejectsAlreadyActiveSubjectAndCapacityBranch() {
        student.setStatus(StudentStatus.REGULAR);
        List<Subject> subjects = fixtureSubjects(3);
        List<Section> sections = fixtureSections(subjects);
        stubValidEnrollment(subjects, sections);
        Enrollment existing = fixtureEnrollment(student, subjects.get(0), sections.get(0), EnrollmentStatus.ACTIVE);
        when(enrollmentRepository.findByStudentIdAndSubjectIdAndAcademicPeriodId(2L, 101L, 1L)).thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(1L,
                EnrollmentCreateRequest.builder().studentId(2L).sectionIds(List.of(10L, 11L, 12L)).build()));
    }

    @Test
    void cancel_marksEnrollmentCancelledAndClearsGrade() {
        List<Section> sections = fixtureSections(1);
        Enrollment enrollment = fixtureEnrollment(student, sections.get(0).getSubject(), sections.get(0), EnrollmentStatus.ACTIVE);
        enrollment.setFinalGrade(new BigDecimal("3.5"));
        when(enrollmentRepository.findByIdAndStudentId(20L, 2L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        var response = enrollmentService.cancel(1L, 2L, 20L);

        assertEquals(EnrollmentStatus.CANCELLED, response.getStatus());
        assertNull(response.getFinalGrade());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void cancel_rejectsEnrollmentForAnotherPeriod() {
        List<Section> sections = fixtureSections(1);
        Enrollment enrollment = fixtureEnrollment(student, sections.get(0).getSubject(), sections.get(0), EnrollmentStatus.ACTIVE);
        when(enrollmentRepository.findByIdAndStudentId(20L, 2L)).thenReturn(Optional.of(enrollment));

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.cancel(99L, 2L, 20L));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void cancel_rejectsCancelledEnrollment() {
        List<Section> sections = fixtureSections(1);
        Enrollment enrollment = fixtureEnrollment(student, sections.get(0).getSubject(), sections.get(0), EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findByIdAndStudentId(20L, 2L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.cancel(1L, 2L, 20L));
    }

    @Test
    void cancel_rejectsEnrollmentInClosedPeriod() {
        period.setStatus(AcademicPeriodStatus.CLOSED);
        List<Section> sections = fixtureSections(1);
        Enrollment enrollment = fixtureEnrollment(student, sections.get(0).getSubject(), sections.get(0), EnrollmentStatus.ACTIVE);
        when(enrollmentRepository.findByIdAndStudentId(20L, 2L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.cancel(1L, 2L, 20L));
    }

    @Test
    void getEnrollmentsMapsPeriodEnrollments() {
        List<Section> sections = fixtureSections(1);
        Enrollment enrollment = fixtureEnrollment(student, sections.get(0).getSubject(), sections.get(0), EnrollmentStatus.ACTIVE);
        when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(List.of(enrollment));

        var result = enrollmentService.getEnrollments(2L, 1L);

        assertEquals(1, result.size());
        assertEquals("SUBJ1", result.get(0).getSubjectCode());
    }

    @Test
    void getAcademicHistoryIncludesOnlyGradedEnrollments() {
        List<Section> sections = fixtureSections(2);
        Enrollment graded = fixtureEnrollment(student, sections.get(0).getSubject(), sections.get(0), EnrollmentStatus.ACTIVE);
        graded.setFinalGrade(new BigDecimal("5.0"));
        Enrollment ungraded = fixtureEnrollment(student, sections.get(1).getSubject(), sections.get(1), EnrollmentStatus.ACTIVE);
        when(enrollmentRepository.findByStudentIdAndStatus(2L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(graded, ungraded));

        var result = enrollmentService.getAcademicHistory(2L);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("5.0"), result.get(0).getFinalGrade());
    }

    @Test
    void getSectionEnrollments_returnsRosterForAssignedTeacher() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByAcademicPeriodIdAndSectionIdAndStatus(1L, 10L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment));

        var result = enrollmentService.getSectionEnrollments(1L, 10L, 3L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getStudentId());
    }

    @Test
    void getSectionEnrollments_rejectsAnotherTeacher() {
        Section section = fixtureSections(1).get(0);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.getSectionEnrollments(1L, 10L, 99L));
        verify(enrollmentRepository, never()).findByAcademicPeriodIdAndSectionIdAndStatus(any(), any(), any());
    }

    @Test
    void recordFinalGrade_savesPassingGradeAndDerivedResult() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        var response = enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("4.0")));

        assertEquals(new BigDecimal("4.0"), response.getFinalGrade());
        assertEquals(EnrollmentResult.PASSED, response.getResult());
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void recordFinalGrade_savesFailingGradeAndDerivedResult() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        var response = enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("3.9")));

        assertEquals(EnrollmentResult.FAILED, response.getResult());
    }

    @Test
    void recordFinalGrade_allowsGradeUpdatesWhilePeriodOpen() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        enrollment.setFinalGrade(new BigDecimal("3.0"));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        var response = enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("4.0")));

        assertEquals(EnrollmentResult.PASSED, response.getResult());
    }

    @Test
    void recordFinalGrade_rejectsGradeOutsideRange() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("7.1"))));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void recordFinalGrade_rejectsGradeBelowRange() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("0.9"))));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void recordFinalGrade_rejectsMoreThanOneDecimalPlace() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.ACTIVE);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("4.00"))));
    }

    @Test
    void recordFinalGrade_rejectsInactiveEnrollment() {
        Section section = fixtureSections(1).get(0);
        Enrollment enrollment = fixtureEnrollment(student, section.getSubject(), section, EnrollmentStatus.CANCELLED);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("5.0"))));
    }

    @Test
    void recordFinalGrade_rejectsClosedPeriod() {
        period.setStatus(AcademicPeriodStatus.CLOSED);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("5.0"))));
        verifyNoInteractions(sectionRepository, enrollmentRepository);
    }

    @Test
    void recordFinalGrade_rejectsEnrollmentOutsideSection() {
        Section section = fixtureSections(1).get(0);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByIdAndAcademicPeriodIdAndSectionId(20L, 1L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.recordFinalGrade(1L, 10L, 20L,
                new FinalGradeRequest(3L, new BigDecimal("5.0"))));
    }

    private void stubBeforeSelection(List<Section> sections) {
        when(matriculationRepository.existsByStudentIdAndAcademicPeriodIdAndMatriculatedTrue(2L, 1L)).thenReturn(true);
        when(sectionRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return sections.stream().filter(section -> section.getId().equals(id)).findFirst();
        });
    }

    private void stubValidEnrollment(List<Subject> subjects, List<Section> sections) {
        stubBeforeSelection(sections);
        lenient().when(enrollmentRepository.findPassedSubjectIds(2L, EnrollmentStatus.ACTIVE, new BigDecimal("4.0"))).thenReturn(List.of());
        lenient().when(subjectRepository.findByStudyPlanId(9L)).thenReturn(subjects);
        lenient().when(enrollmentRepository.findByStudentIdAndAcademicPeriodId(2L, 1L)).thenReturn(List.of());
        lenient().when(sectionRepository.findByAcademicPeriodId(1L)).thenReturn(sections);
        lenient().when(enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeGreaterThanEqualAndStatus(
                any(), any(), any(), any())).thenReturn(0L);
        lenient().when(enrollmentRepository.countByStudentIdAndSubjectIdAndFinalGradeLessThanAndStatus(
                any(), any(), any(), any())).thenReturn(0L);
        lenient().when(enrollmentRepository.countBySectionIdAndStatus(any(), any())).thenReturn(0L);
        lenient().when(enrollmentRepository.findByStudentIdAndSubjectIdAndAcademicPeriodId(any(), any(), any())).thenReturn(Optional.empty());
    }

    private List<Subject> fixtureSubjects(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> Subject.builder()
                        .id(101L + index)
                        .code("SUBJ" + (index + 1))
                        .name("Subject " + (index + 1))
                        .semester(1)
                        .studyPlan(plan)
                        .prerequisites(new HashSet<>())
                        .build())
                .toList();
    }

    private List<Section> fixtureSections(int count) {
        return fixtureSections(fixtureSubjects(count));
    }

    private List<Section> fixtureSections(List<Subject> subjects) {
        return java.util.stream.IntStream.range(0, subjects.size())
                .mapToObj(index -> Section.builder()
                        .id(10L + index)
                        .subject(subjects.get(index))
                        .academicPeriod(period)
                        .teacher(Teacher.builder().id(3L).fullName("Teacher").build())
                        .capacity(30)
                        .scheduleBlocks(new HashSet<>(Set.of(new ScheduleBlock("L", index + 1))))
                        .build())
                .toList();
    }

    private Enrollment fixtureEnrollment(Student owner, Subject subject, Section section, EnrollmentStatus status) {
        return Enrollment.builder()
                .id(20L)
                .student(owner)
                .subject(subject)
                .section(section)
                .academicPeriod(period)
                .status(status)
                .enrolledAt(LocalDateTime.of(2027, 1, 1, 9, 0))
                .build();
    }
}