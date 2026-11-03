package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Career;
import com.example.demo.model.CareerStatus;
import com.example.demo.model.PlanStatus;
import com.example.demo.model.StudyPlan;
import com.example.demo.model.Subject;
import com.example.demo.repository.StudyPlanRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private SubjectService subjectService;

    private Career sampleCareer;
    private StudyPlan samplePlan;
    private Subject sampleSubjectSem1;
    private Subject sampleSubjectSem2;

    @BeforeEach
    void setUp() {
        sampleCareer = Career.builder()
                .id(1L)
                .code("10450")
                .name("Técnico Analista Programador")
                .status(CareerStatus.ACTIVE)
                .build();

        samplePlan = StudyPlan.builder()
                .id(10L)
                .code("2021.3")
                .name("Plan 2021.3")
                .status(PlanStatus.CURRENT)
                .career(sampleCareer)
                .build();

        sampleSubjectSem1 = Subject.builder()
                .id(101L)
                .code("TAP101")
                .name("Fundamentos de Programación")
                .semester(1)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .studyPlan(samplePlan)
                .prerequisites(new HashSet<>())
                .build();

        sampleSubjectSem2 = Subject.builder()
                .id(201L)
                .code("TAP201")
                .name("Programación Orientada a Objetos")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .studyPlan(samplePlan)
                .prerequisites(new HashSet<>(List.of(sampleSubjectSem1)))
                .build();
    }

    @Test
    void testGetSubjectsByStudyPlan_Success() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByStudyPlanIdWithPrerequisites(10L))
                .thenReturn(List.of(sampleSubjectSem1, sampleSubjectSem2));

        List<SubjectResponse> responses = subjectService.getSubjectsByStudyPlan(10L);

        assertEquals(2, responses.size());
        assertEquals("TAP101", responses.get(0).getCode());
        assertEquals("TAP201", responses.get(1).getCode());
        assertEquals(1, responses.get(1).getPrerequisites().size());
    }

    @Test
    void testGetSubjectsByStudyPlan_NotFound() {
        when(studyPlanRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> subjectService.getSubjectsByStudyPlan(99L));
    }

    @Test
    void testGetSubjectsByStudyPlanAndSemester_Success() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByStudyPlanIdAndSemester(10L, 1)).thenReturn(List.of(sampleSubjectSem1));

        List<SubjectResponse> responses = subjectService.getSubjectsByStudyPlanAndSemester(10L, 1);

        assertEquals(1, responses.size());
        assertEquals("TAP101", responses.get(0).getCode());
    }

    @Test
    void testGetSubjectsByStudyPlanAndSemester_InvalidSemester() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> subjectService.getSubjectsByStudyPlanAndSemester(10L, 5));
    }

    @Test
    void testGetSubjectById_Success() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(101L)).thenReturn(Optional.of(sampleSubjectSem1));

        SubjectResponse response = subjectService.getSubjectById(10L, 101L);

        assertNotNull(response);
        assertEquals("TAP101", response.getCode());
        assertEquals("2-2-2", response.getTel());
        assertEquals(6, response.getTotalWeeklyHours());
    }

    @Test
    void testGetSubjectById_NotFound() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subjectService.getSubjectById(10L, 999L));
    }

    @Test
    void testGetSubjectByCode_Success() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByCodeAndStudyPlanId("TAP101", 10L)).thenReturn(Optional.of(sampleSubjectSem1));

        SubjectResponse response = subjectService.getSubjectByCode(10L, "TAP101");

        assertNotNull(response);
        assertEquals("TAP101", response.getCode());
    }

    @Test
    void testGetSubjectById_DifferentPlan_ThrowsException() {
        StudyPlan otherPlan = StudyPlan.builder().id(999L).build();
        Subject subjectOtherPlan = Subject.builder()
                .id(101L)
                .code("TAP101")
                .studyPlan(otherPlan)
                .build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(101L)).thenReturn(Optional.of(subjectOtherPlan));

        assertThrows(ResourceNotFoundException.class, () -> subjectService.getSubjectById(10L, 101L));
    }

    @Test
    void testGetSubjectByCode_NotFound() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByCodeAndStudyPlanId("UNKNOWN", 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subjectService.getSubjectByCode(10L, "UNKNOWN"));
    }

    @Test
    void testCreateSubject_WithPrerequisiteCodes_Success() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP201")
                .name("POO")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .prerequisiteCodes(List.of("TAP101"))
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.existsByCodeAndStudyPlanId("TAP201", 10L)).thenReturn(false);
        when(subjectRepository.findByCodeAndStudyPlanId("TAP101", 10L)).thenReturn(Optional.of(sampleSubjectSem1));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.createSubject(10L, request);

        assertEquals(1, response.getPrerequisites().size());
        assertEquals("TAP101", response.getPrerequisites().get(0).getCode());
    }

    @Test
    void testCreateSubject_PrerequisiteCodeNotFound_ThrowsException() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP201")
                .name("POO")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .prerequisiteCodes(List.of("NOT_FOUND"))
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.existsByCodeAndStudyPlanId("TAP201", 10L)).thenReturn(false);
        when(subjectRepository.findByCodeAndStudyPlanId("NOT_FOUND", 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subjectService.createSubject(10L, request));
    }

    @Test
    void testAddPrerequisite_MaxExceeded_ThrowsException() {
        Subject s1 = Subject.builder().id(1L).code("S1").semester(1).studyPlan(samplePlan).build();
        Subject s2 = Subject.builder().id(2L).code("S2").semester(1).studyPlan(samplePlan).build();
        Subject s3 = Subject.builder().id(3L).code("S3").semester(1).studyPlan(samplePlan).build();
        Subject target = Subject.builder().id(4L).code("TARGET").semester(3).studyPlan(samplePlan)
                .prerequisites(new HashSet<>(List.of(s1, s2, s3))).build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(4L)).thenReturn(Optional.of(target));

        assertThrows(BusinessRuleException.class, () -> subjectService.addPrerequisite(10L, 4L, 5L));
    }

    @Test
    void testAddPrerequisite_DifferentPlan_ThrowsException() {
        StudyPlan other = StudyPlan.builder().id(999L).build();
        Subject diffPlanSubject = Subject.builder().id(50L).code("DIFF").semester(1).studyPlan(other).build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(201L)).thenReturn(Optional.of(sampleSubjectSem2));
        when(subjectRepository.findById(50L)).thenReturn(Optional.of(diffPlanSubject));

        assertThrows(BusinessRuleException.class, () -> subjectService.addPrerequisite(10L, 201L, 50L));
    }

    @Test
    void testAddPrerequisite_PrereqSemesterEqualOrHigher_ThrowsException() {
        Subject sem2Prereq = Subject.builder().id(51L).code("P2").semester(2).studyPlan(samplePlan).build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(201L)).thenReturn(Optional.of(sampleSubjectSem2));
        when(subjectRepository.findById(51L)).thenReturn(Optional.of(sem2Prereq));

        assertThrows(BusinessRuleException.class, () -> subjectService.addPrerequisite(10L, 201L, 51L));
    }

    @Test
    void testCreateSubject_Success_NoPrerequisites() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP102")
                .name("Matemática Aplicada")
                .semester(1)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(0)
                .creditsSct(5)
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.existsByCodeAndStudyPlanId("TAP102", 10L)).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(102L);
            return s;
        });

        SubjectResponse response = subjectService.createSubject(10L, request);

        assertNotNull(response);
        assertEquals("TAP102", response.getCode());
        assertEquals(4, response.getTotalWeeklyHours());
        assertEquals("2-2-0", response.getTel());
        assertEquals(5, response.getCreditsSct());
        assertTrue(response.getPrerequisites().isEmpty());
    }

    @Test
    void testCreateSubject_Success_WithPrerequisites() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP201")
                .name("Programación Orientada a Objetos")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .prerequisiteIds(List.of(101L))
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.existsByCodeAndStudyPlanId("TAP201", 10L)).thenReturn(false);
        when(subjectRepository.findById(101L)).thenReturn(Optional.of(sampleSubjectSem1));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(201L);
            return s;
        });

        SubjectResponse response = subjectService.createSubject(10L, request);

        assertNotNull(response);
        assertEquals("TAP201", response.getCode());
        assertEquals(1, response.getPrerequisites().size());
        assertEquals("TAP101", response.getPrerequisites().get(0).getCode());
    }

    @Test
    void testCreateSubject_DuplicateCode_ThrowsException() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP101")
                .name("Fundamentos")
                .semester(1)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.existsByCodeAndStudyPlanId("TAP101", 10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> subjectService.createSubject(10L, request));
    }

    @Test
    void testCreateSubject_WeeklyHours8OrMore_ThrowsException() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP101")
                .name("Heavy Subject")
                .semester(1)
                .hoursTheory(4)
                .hoursExercise(2)
                .hoursLab(2) // sum = 8, must be strictly < 8
                .creditsSct(6)
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.existsByCodeAndStudyPlanId("TAP101", 10L)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> subjectService.createSubject(10L, request));
        assertTrue(ex.getMessage().contains("strictly less than 8 hours"));
    }

    @Test
    void testCreateSubject_NegativeHours_ThrowsException() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP101")
                .name("Invalid Hours")
                .semester(1)
                .hoursTheory(-1)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));

        assertThrows(BusinessRuleException.class, () -> subjectService.createSubject(10L, request));
    }

    @Test
    void testCreateSubject_InvalidSct_ThrowsException() {
        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP101")
                .name("Invalid SCT")
                .semester(1)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(8) // > 7
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));

        assertThrows(BusinessRuleException.class, () -> subjectService.createSubject(10L, request));
    }

    @Test
    void testCreateSubject_PrerequisiteNotFromPriorSemester_ThrowsException() {
        Subject sameSemesterSubject = Subject.builder()
                .id(102L)
                .code("TAP102")
                .semester(2)
                .studyPlan(samplePlan)
                .build();

        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP201")
                .name("Subject Sem 2")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .prerequisiteIds(List.of(102L))
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.findById(102L)).thenReturn(Optional.of(sameSemesterSubject));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> subjectService.createSubject(10L, request));
        assertTrue(ex.getMessage().contains("must belong to a previous semester"));
    }

    @Test
    void testCreateSubject_PrerequisiteDifferentPlan_ThrowsException() {
        StudyPlan otherPlan = StudyPlan.builder().id(20L).code("2022.3").build();
        Subject otherPlanSubject = Subject.builder()
                .id(999L)
                .code("TAD101")
                .semester(1)
                .studyPlan(otherPlan)
                .build();

        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP201")
                .name("Subject Sem 2")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .prerequisiteIds(List.of(999L))
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.findById(999L)).thenReturn(Optional.of(otherPlanSubject));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> subjectService.createSubject(10L, request));
        assertTrue(ex.getMessage().contains("different study plan"));
    }

    @Test
    void testCreateSubject_MoreThanThreePrerequisites_ThrowsException() {
        Subject p1 = Subject.builder().id(1L).code("P1").semester(1).studyPlan(samplePlan).build();
        Subject p2 = Subject.builder().id(2L).code("P2").semester(1).studyPlan(samplePlan).build();
        Subject p3 = Subject.builder().id(3L).code("P3").semester(1).studyPlan(samplePlan).build();
        Subject p4 = Subject.builder().id(4L).code("P4").semester(1).studyPlan(samplePlan).build();

        SubjectCreateRequest request = SubjectCreateRequest.builder()
                .code("TAP201")
                .name("Subject Sem 2")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .prerequisiteIds(List.of(1L, 2L, 3L, 4L))
                .build();

        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(p3));
        when(subjectRepository.findById(4L)).thenReturn(Optional.of(p4));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> subjectService.createSubject(10L, request));
        assertTrue(ex.getMessage().contains("cannot have more than 3 prerequisites"));
    }

    @Test
    void testUpdateSubject_Success() {
        SubjectUpdateRequest updateRequest = SubjectUpdateRequest.builder()
                .name("Updated Name")
                .semester(2)
                .hoursTheory(2)
                .hoursExercise(0)
                .hoursLab(4)
                .creditsSct(6)
                .build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(201L)).thenReturn(Optional.of(sampleSubjectSem2));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.updateSubject(10L, 201L, updateRequest);

        assertEquals("Updated Name", response.getName());
        assertEquals("2-0-4", response.getTel());
        assertEquals(6, response.getTotalWeeklyHours());
    }

    @Test
    void testUpdateSubject_MoveSemesterViolatesDependent_ThrowsException() {
        // sampleSubjectSem1 is prerequisite for sampleSubjectSem2 (semester 2).
        // If we try to move sampleSubjectSem1 to semester 2 or 3, it violates prerequisite rule for sampleSubjectSem2.
        SubjectUpdateRequest updateRequest = SubjectUpdateRequest.builder()
                .name("Moved Subject")
                .semester(3)
                .hoursTheory(2)
                .hoursExercise(2)
                .hoursLab(2)
                .creditsSct(6)
                .build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(101L)).thenReturn(Optional.of(sampleSubjectSem1));
        when(subjectRepository.findByPrerequisitesContaining(sampleSubjectSem1)).thenReturn(List.of(sampleSubjectSem2));

        assertThrows(BusinessRuleException.class, () -> subjectService.updateSubject(10L, 101L, updateRequest));
    }

    @Test
    void testDeleteSubject_Success() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findById(201L)).thenReturn(Optional.of(sampleSubjectSem2));
        when(subjectRepository.findByPrerequisitesContaining(sampleSubjectSem2)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> subjectService.deleteSubject(10L, 201L));
        verify(subjectRepository).delete(sampleSubjectSem2);
    }

    @Test
    void testDeleteSubject_HasPrerequisiteDependents_ThrowsException() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findById(101L)).thenReturn(Optional.of(sampleSubjectSem1));
        when(subjectRepository.findByPrerequisitesContaining(sampleSubjectSem1)).thenReturn(List.of(sampleSubjectSem2));

        assertThrows(BusinessRuleException.class, () -> subjectService.deleteSubject(10L, 101L));
        verify(subjectRepository, never()).delete(any(Subject.class));
    }

    @Test
    void testAddPrerequisite_Success() {
        Subject sem3Subject = Subject.builder()
                .id(301L)
                .code("TAP301")
                .semester(3)
                .studyPlan(samplePlan)
                .prerequisites(new HashSet<>())
                .build();

        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(301L)).thenReturn(Optional.of(sem3Subject));
        when(subjectRepository.findById(201L)).thenReturn(Optional.of(sampleSubjectSem2));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.addPrerequisite(10L, 301L, 201L);

        assertEquals(1, response.getPrerequisites().size());
        assertEquals("TAP201", response.getPrerequisites().get(0).getCode());
    }

    @Test
    void testAddPrerequisite_Self_ThrowsException() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(101L)).thenReturn(Optional.of(sampleSubjectSem1));

        assertThrows(BusinessRuleException.class, () -> subjectService.addPrerequisite(10L, 101L, 101L));
    }

    @Test
    void testRemovePrerequisite_Success() {
        when(studyPlanRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.findByIdWithPrerequisites(201L)).thenReturn(Optional.of(sampleSubjectSem2));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.removePrerequisite(10L, 201L, 101L);

        assertTrue(response.getPrerequisites().isEmpty());
    }
}
