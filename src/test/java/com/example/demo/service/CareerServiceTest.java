package com.example.demo.service;

import com.example.demo.dto.CareerCreateRequest;
import com.example.demo.dto.CareerResponse;
import com.example.demo.dto.CareerUpdateRequest;
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
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CareerService careerService;

    private Career sampleCareer;

    @BeforeEach
    void setUp() {
        sampleCareer = Career.builder()
                .id(1L)
                .code("10450")
                .name("Técnico Analista Programador")
                .description("Carrera técnica de 2 años")
                .durationSemesters(4)
                .status(CareerStatus.ACTIVE)
                .build();
    }

    @Test
    void testCreateCareer_Success() {
        CareerCreateRequest request = CareerCreateRequest.builder()
                .code("10450")
                .name("Técnico Analista Programador")
                .description("Carrera técnica")
                .durationSemesters(4)
                .build();

        when(careerRepository.existsByCode("10450")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenReturn(sampleCareer);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.empty());

        CareerResponse response = careerService.createCareer(request);

        assertNotNull(response);
        assertEquals("10450", response.getCode());
        assertEquals("Técnico Analista Programador", response.getName());
        verify(careerRepository).save(any(Career.class));
    }

    @Test
    void testCreateCareer_DuplicateCode_ThrowsException() {
        CareerCreateRequest request = CareerCreateRequest.builder()
                .code("10450")
                .name("Técnico Analista Programador")
                .build();

        when(careerRepository.existsByCode("10450")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> careerService.createCareer(request));
        verify(careerRepository, never()).save(any(Career.class));
    }

    @Test
    void testGetCareerById_Success() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.empty());

        CareerResponse response = careerService.getCareerById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void testGetCareerById_NotFound() {
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.getCareerById(99L));
    }

    @Test
    void testGetCareerByCode_Success() {
        when(careerRepository.findByCode("10450")).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.empty());

        CareerResponse response = careerService.getCareerByCode("10450");

        assertNotNull(response);
        assertEquals("10450", response.getCode());
    }

    @Test
    void testUpdateCareer_Success() {
        CareerUpdateRequest updateRequest = CareerUpdateRequest.builder()
                .name("Updated Name")
                .description("Updated Description")
                .status(CareerStatus.INACTIVE)
                .build();

        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(careerRepository.save(any(Career.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerResponse response = careerService.updateCareer(1L, updateRequest);

        assertEquals("Updated Name", response.getName());
        assertEquals(CareerStatus.INACTIVE, response.getStatus());
    }

    @Test
    void testDeleteCareer_Success() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.countByCareerId(1L)).thenReturn(0L);
        when(studentRepository.countByCareerId(1L)).thenReturn(0L);

        assertDoesNotThrow(() -> careerService.deleteCareer(1L));
        verify(careerRepository).delete(sampleCareer);
    }

    @Test
    void testDeleteCareer_HasAssociatedPlans_ThrowsException() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.countByCareerId(1L)).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> careerService.deleteCareer(1L));
        verify(careerRepository, never()).delete(any(Career.class));
    }

    @Test
    void testDeleteCareer_HasAssociatedStudents_ThrowsException() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(sampleCareer));
        when(studyPlanRepository.countByCareerId(1L)).thenReturn(0L);
        when(studentRepository.countByCareerId(1L)).thenReturn(3L);

        assertThrows(BusinessRuleException.class, () -> careerService.deleteCareer(1L));
        verify(careerRepository, never()).delete(any(Career.class));
    }

    @Test
    void testGetAllCareers() {
        StudyPlan currentPlan = StudyPlan.builder()
                .id(10L)
                .code("2021.3")
                .name("Plan de estudios 2021.3")
                .status(PlanStatus.CURRENT)
                .career(sampleCareer)
                .build();

        when(careerRepository.findAll()).thenReturn(List.of(sampleCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.of(currentPlan));

        List<CareerResponse> results = careerService.getAllCareers();

        assertEquals(1, results.size());
        assertEquals("2021.3", results.get(0).getCurrentStudyPlanCode());
    }
}
