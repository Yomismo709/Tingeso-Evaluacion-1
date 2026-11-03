package com.example.demo.service;

import com.example.demo.dto.StudyPlanCreateRequest;
import com.example.demo.dto.StudyPlanResponse;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Career;
import com.example.demo.model.CareerStatus;
import com.example.demo.model.PlanStatus;
import com.example.demo.model.StudyPlan;
import com.example.demo.repository.CareerRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.StudyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudyPlanService studyPlanService;

    private Career sampleCareer;
    private StudyPlan currentPlan;

    @BeforeEach
    void setUp() {
        sampleCareer = Career.builder()
                .id(1L)
                .code("10450")
                .name("Técnico Analista Programador")
                .status(CareerStatus.ACTIVE)
                .build();

        currentPlan = StudyPlan.builder()
                .id(10L)
                .code("2021.3")
                .name("Plan de estudios 2021.3")
                .status(PlanStatus.CURRENT)
                .career(sampleCareer)
                .build();
    }

    @Test
    void testGetStudyPlansByCareer_Success() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findByCareerId(1L)).thenReturn(List.of(currentPlan));

        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansByCareer(1L);

        assertEquals(1, plans.size());
        assertEquals("2021.3", plans.get(0).getCode());
    }

    @Test
    void testGetStudyPlansByCareer_CareerNotFound() {
        when(careerRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.getStudyPlansByCareer(99L));
    }

    @Test
    void testCreateStudyPlan_Success_DeactivatesPreviousCurrent() {
        StudyPlanCreateRequest request = StudyPlanCreateRequest.builder()
                .code("2026.1")
                .name("Plan de estudios 2026.1")
                .setAsCurrent(true)
                .build();

        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.existsByCodeAndCareerId("2026.1", 1L)).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.of(currentPlan));

        StudyPlan savedPlan = StudyPlan.builder()
                .id(11L)
                .code("2026.1")
                .name("Plan de estudios 2026.1")
                .status(PlanStatus.CURRENT)
                .career(sampleCareer)
                .build();

        when(studyPlanRepository.save(any(StudyPlan.class))).thenReturn(savedPlan);

        StudyPlanResponse response = studyPlanService.createStudyPlan(1L, request);

        assertNotNull(response);
        assertEquals("2026.1", response.getCode());
        assertEquals(PlanStatus.CURRENT, response.getStatus());

        // Verify previous plan was updated to NOT_CURRENT
        assertEquals(PlanStatus.NOT_CURRENT, currentPlan.getStatus());
        verify(studyPlanRepository, atLeastOnce()).save(currentPlan);
    }

    @Test
    void testCreateStudyPlan_DuplicateCode_ThrowsException() {
        StudyPlanCreateRequest request = StudyPlanCreateRequest.builder()
                .code("2021.3")
                .name("Plan de estudios 2021.3")
                .build();

        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.existsByCodeAndCareerId("2021.3", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studyPlanService.createStudyPlan(1L, request));
    }

    @Test
    void testSetStudyPlanAsCurrent_Success() {
        StudyPlan oldPlan = StudyPlan.builder()
                .id(12L)
                .code("2020.1")
                .name("Plan de estudios 2020.1")
                .status(PlanStatus.NOT_CURRENT)
                .career(sampleCareer)
                .build();

        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findById(12L)).thenReturn(Optional.of(oldPlan));
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.of(currentPlan));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudyPlanResponse response = studyPlanService.setStudyPlanAsCurrent(1L, 12L);

        assertEquals(PlanStatus.CURRENT, response.getStatus());
        assertEquals(PlanStatus.NOT_CURRENT, currentPlan.getStatus());
    }

    @Test
    void testSetStudyPlanAsCurrent_PlanDoesNotBelongToCareer() {
        Career otherCareer = Career.builder().id(2L).code("10451").build();
        StudyPlan foreignPlan = StudyPlan.builder().id(15L).career(otherCareer).build();

        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findById(15L)).thenReturn(Optional.of(foreignPlan));

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.setStudyPlanAsCurrent(1L, 15L));
    }

    @Test
    void testDeleteStudyPlan_Success() {
        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(currentPlan));
        when(studentRepository.countByStudyPlanId(10L)).thenReturn(0L);

        assertDoesNotThrow(() -> studyPlanService.deleteStudyPlan(10L));
        verify(studyPlanRepository).delete(currentPlan);
    }

    @Test
    void testDeleteStudyPlan_HasAssociatedStudents_ThrowsException() {
        when(studyPlanRepository.findById(10L)).thenReturn(Optional.of(currentPlan));
        when(studentRepository.countByStudyPlanId(10L)).thenReturn(5L);

        assertThrows(BusinessRuleException.class, () -> studyPlanService.deleteStudyPlan(10L));
        verify(studyPlanRepository, never()).delete(any(StudyPlan.class));
    }
}
