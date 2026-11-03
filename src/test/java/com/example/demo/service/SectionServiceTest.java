package com.example.demo.service;

import com.example.demo.dto.ScheduleBlockRequest;
import com.example.demo.dto.SectionCreateRequest;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.model.*;
import com.example.demo.repository.AcademicPeriodRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private SectionService sectionService;

    private AcademicPeriod period;
    private Subject subject;
    private Teacher teacher;
    private SectionCreateRequest request;

    @BeforeEach
    void setUp() {
        period = AcademicPeriod.builder().id(1L).year(2027).semester(1).status(AcademicPeriodStatus.OPEN).build();
        subject = Subject.builder()
                .id(2L).code("TAP101").name("Programming").semester(1)
                .hoursTheory(2).hoursExercise(2).hoursLab(2).build();
        teacher = Teacher.builder().id(3L).fullName("Teacher").status(TeacherStatus.ACTIVE).build();
        request = SectionCreateRequest.builder()
                .subjectId(2L).teacherId(3L).capacity(30)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("L", 1),
                        new ScheduleBlockRequest("M", 2),
                        new ScheduleBlockRequest("W", 3)))
                .build();
        lenient().when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        lenient().when(subjectRepository.findById(2L)).thenReturn(Optional.of(subject));
        lenient().when(teacherRepository.findById(3L)).thenReturn(Optional.of(teacher));
        lenient().when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(2L, 1L)).thenReturn(false);
        lenient().when(sectionRepository.existsScheduleConflict(any(), any(), any(), any(), any(), any())).thenReturn(false);
        lenient().when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> {
            Section section = invocation.getArgument(0);
            section.setId(4L);
            return section;
        });
    }

    @Test
    void createSection_acceptsValidOfferAndMapsSchedule() {
        var response = sectionService.createSection(1L, request);

        assertEquals(4L, response.getId());
        assertEquals("2027-1", response.getAcademicPeriodCode());
        assertEquals(3, response.getScheduleBlocks().size());
        verify(sectionRepository).save(any(Section.class));
    }

    @Test
    void createSection_rejectsIncorrectTELBlockCount() {
        request.setScheduleBlocks(List.of(new ScheduleBlockRequest("L", 1)));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_rejectsTeacherScheduleConflict() {
        when(sectionRepository.existsScheduleConflict(3L, 1L, AcademicPeriodStatus.OPEN, "L", 1, null)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_rejectsInactiveTeacher() {
        teacher.setStatus(TeacherStatus.INACTIVE);

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void getSections_listsAndMapsSectionsForPeriod() {
        when(sectionRepository.findByAcademicPeriodId(1L)).thenReturn(List.of(existingSection()));

        var result = sectionService.getSections(1L);

        assertEquals(1, result.size());
        assertEquals("TAP101", result.get(0).getSubjectCode());
        assertEquals("Teacher", result.get(0).getTeacherName());
        assertEquals(1, result.get(0).getScheduleBlocks().size());
    }

    @Test
    void getSections_rejectsUnknownPeriod() {
        when(academicPeriodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.example.demo.exception.ResourceNotFoundException.class, () -> sectionService.getSections(99L));
        verify(sectionRepository, never()).findByAcademicPeriodId(any());
    }

    @Test
    void getSection_returnsSectionInPeriod() {
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(existingSection()));

        var result = sectionService.getSection(1L, 4L);

        assertEquals(4L, result.getId());
        assertEquals("2027-1", result.getAcademicPeriodCode());
    }

    @Test
    void getSection_rejectsSectionFromAnotherPeriod() {
        Section section = existingSection();
        section.getAcademicPeriod().setId(88L);
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));

        assertThrows(com.example.demo.exception.ResourceNotFoundException.class, () -> sectionService.getSection(1L, 4L));
    }

    @Test
    void createSection_rejectsClosedPeriod() {
        period.setStatus(AcademicPeriodStatus.CLOSED);

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
        verifyNoInteractions(subjectRepository, teacherRepository);
    }

    @Test
    void createSection_rejectsSubjectAlreadyOffered() {
        when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(2L, 1L)).thenReturn(true);

        assertThrows(com.example.demo.exception.DuplicateResourceException.class,
                () -> sectionService.createSection(1L, request));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_rejectsEmptySchedule() {
        request.setScheduleBlocks(List.of());

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
    }

    @Test
    void createSection_rejectsDuplicateScheduleBlocks() {
        request.setScheduleBlocks(List.of(
                new ScheduleBlockRequest("L", 1),
                new ScheduleBlockRequest("L", 1),
                new ScheduleBlockRequest("M", 2)));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
    }

    @Test
    void createSection_rejectsInvalidDayOrModule() {
        request.setScheduleBlocks(List.of(
                new ScheduleBlockRequest("X", 1),
                new ScheduleBlockRequest("M", 2),
                new ScheduleBlockRequest("W", 3)));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
    }

    @Test
    void createSection_rejectsOddTELHours() {
        subject.setHoursTheory(1);
        subject.setHoursExercise(0);
        subject.setHoursLab(0);
        request.setScheduleBlocks(List.of(new ScheduleBlockRequest("L", 1)));

        assertThrows(BusinessRuleException.class, () -> sectionService.createSection(1L, request));
    }

    @Test
    void updateSection_updatesTeacherCapacityAndSortedSchedule() {
        Section section = existingSection();
        Teacher replacementTeacher = Teacher.builder().id(5L).fullName("Replacement").status(TeacherStatus.ACTIVE).build();
        SectionCreateRequest update = SectionCreateRequest.builder()
                .subjectId(2L).teacherId(5L).capacity(40)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("W", 3),
                        new ScheduleBlockRequest("L", 1),
                        new ScheduleBlockRequest("M", 2)))
                .build();
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(5L)).thenReturn(Optional.of(replacementTeacher));
        when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(2L, 1L)).thenReturn(false);
        when(enrollmentRepository.countBySectionIdAndStatus(4L, EnrollmentStatus.ACTIVE)).thenReturn(10L);
        when(sectionRepository.existsScheduleConflict(any(), any(), any(), any(), any(), eq(4L))).thenReturn(false);

        var result = sectionService.updateSection(1L, 4L, update);

        assertEquals("Replacement", result.getTeacherName());
        assertEquals(40, result.getCapacity());
        assertEquals("L", result.getScheduleBlocks().get(0).getDayCode());
        assertEquals("M", result.getScheduleBlocks().get(1).getDayCode());
        assertEquals("W", result.getScheduleBlocks().get(2).getDayCode());
        verify(sectionRepository).save(section);
    }

    @Test
    void updateSection_allowsUnchangedSubjectDespiteUniqueOffer() {
        Section section = existingSection();
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(2L, 1L)).thenReturn(true);
        when(enrollmentRepository.countBySectionIdAndStatus(4L, EnrollmentStatus.ACTIVE)).thenReturn(0L);
        when(sectionRepository.existsScheduleConflict(any(), any(), any(), any(), any(), eq(4L))).thenReturn(false);

        assertDoesNotThrow(() -> sectionService.updateSection(1L, 4L, request));
    }

    @Test
    void updateSection_rejectsDuplicateSubjectOffer() {
        Section section = existingSection();
        Subject replacement = Subject.builder().id(9L).code("TAP102").name("Math").hoursTheory(2).hoursExercise(2).hoursLab(2).build();
        SectionCreateRequest update = SectionCreateRequest.builder()
                .subjectId(9L).teacherId(3L).capacity(30).scheduleBlocks(request.getScheduleBlocks()).build();
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(subjectRepository.findById(9L)).thenReturn(Optional.of(replacement));
        when(sectionRepository.existsBySubjectIdAndAcademicPeriodId(9L, 1L)).thenReturn(true);

        assertThrows(com.example.demo.exception.DuplicateResourceException.class,
                () -> sectionService.updateSection(1L, 4L, update));
    }

    @Test
    void updateSection_rejectsCapacityBelowCurrentEnrollment() {
        Section section = existingSection();
        request.setCapacity(9);
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.countBySectionIdAndStatus(4L, EnrollmentStatus.ACTIVE)).thenReturn(10L);

        assertThrows(BusinessRuleException.class, () -> sectionService.updateSection(1L, 4L, request));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_rejectsClosedPeriod() {
        Section section = existingSection();
        period.setStatus(AcademicPeriodStatus.CLOSED);
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));

        assertThrows(BusinessRuleException.class, () -> sectionService.updateSection(1L, 4L, request));
    }

    @Test
    void updateSection_rejectsScheduleConflict() {
        Section section = existingSection();
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.countBySectionIdAndStatus(4L, EnrollmentStatus.ACTIVE)).thenReturn(0L);
        when(sectionRepository.existsScheduleConflict(3L, 1L, AcademicPeriodStatus.OPEN, "L", 1, 4L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> sectionService.updateSection(1L, 4L, request));
    }

    @Test
    void deleteSection_deletesUnenrolledSection() {
        Section section = existingSection();
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.existsBySectionId(4L)).thenReturn(false);

        sectionService.deleteSection(1L, 4L);

        verify(sectionRepository).delete(section);
    }

    @Test
    void deleteSection_rejectsSectionWithEnrollmentHistory() {
        Section section = existingSection();
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.existsBySectionId(4L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> sectionService.deleteSection(1L, 4L));
        verify(sectionRepository, never()).delete(any());
    }

    @Test
    void deleteSection_rejectsClosedPeriod() {
        Section section = existingSection();
        period.setStatus(AcademicPeriodStatus.CLOSED);
        when(sectionRepository.findById(4L)).thenReturn(Optional.of(section));

        assertThrows(BusinessRuleException.class, () -> sectionService.deleteSection(1L, 4L));
        verify(enrollmentRepository, never()).existsBySectionId(any());
    }

    private Section existingSection() {
        return Section.builder()
                .id(4L)
                .subject(subject)
                .academicPeriod(period)
                .teacher(teacher)
                .capacity(30)
                .scheduleBlocks(Set.of(new ScheduleBlock("W", 3)))
                .build();
    }
}