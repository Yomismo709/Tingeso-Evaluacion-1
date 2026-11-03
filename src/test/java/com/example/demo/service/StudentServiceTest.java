package com.example.demo.service;

import com.example.demo.dto.StudentCreateRequest;
import com.example.demo.dto.StudentResponse;
import com.example.demo.dto.StudentUpdateRequest;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.CareerRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.StudyPlanRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.StudentMatriculationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentMatriculationRepository matriculationRepository;

    @InjectMocks
    @Spy
    private StudentService studentService;

    private Career activeCareer;
    private Career inactiveCareer;
    private StudyPlan currentPlan;
    private StudyPlan otherPlan;
    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        activeCareer = Career.builder()
                .id(1L)
                .code("10450")
                .name("Técnico Analista Programador")
                .status(CareerStatus.ACTIVE)
                .build();

        inactiveCareer = Career.builder()
                .id(2L)
                .code("10460")
                .name("Carrera Inactiva")
                .status(CareerStatus.INACTIVE)
                .build();

        currentPlan = StudyPlan.builder()
                .id(10L)
                .code("2021.3")
                .name("Plan 2021.3")
                .status(PlanStatus.CURRENT)
                .career(activeCareer)
                .build();

        otherPlan = StudyPlan.builder()
                .id(11L)
                .code("2018.1")
                .name("Plan 2018.1")
                .status(PlanStatus.NOT_CURRENT)
                .career(activeCareer)
                .build();

        sampleStudent = Student.builder()
                .id(1L)
                .run("12.345.678-9")
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .status(StudentStatus.REGULAR)
                .career(activeCareer)
                .studyPlan(currentPlan)
                .build();
    }

    // ─── Queries ───────────────────────────────────────────────────────────

    @Test
    void testGetAllStudents_NoFilter() {
        when(studentRepository.findAll()).thenReturn(List.of(sampleStudent));

        List<StudentResponse> result = studentService.getAllStudents(null, null);

        assertEquals(1, result.size());
        assertEquals("12.345.678-9", result.get(0).getRun());
        verify(studentRepository).findAll();
    }

    @Test
    void testGetAllStudents_FilterByCareer() {
        when(studentRepository.findByCareerId(1L)).thenReturn(List.of(sampleStudent));

        List<StudentResponse> result = studentService.getAllStudents(1L, null);

        assertEquals(1, result.size());
        verify(studentRepository).findByCareerId(1L);
    }

    @Test
    void testGetAllStudents_FilterByStatus() {
        when(studentRepository.findByStatus(StudentStatus.REGULAR)).thenReturn(List.of(sampleStudent));

        List<StudentResponse> result = studentService.getAllStudents(null, StudentStatus.REGULAR);

        assertEquals(1, result.size());
        assertEquals(StudentStatus.REGULAR, result.get(0).getStatus());
        verify(studentRepository).findByStatus(StudentStatus.REGULAR);
    }

    @Test
    void testGetAllStudents_FilterByCareerAndStatus() {
        when(studentRepository.findByCareerIdAndStatus(1L, StudentStatus.REGULAR)).thenReturn(List.of(sampleStudent));

        List<StudentResponse> result = studentService.getAllStudents(1L, StudentStatus.REGULAR);

        assertEquals(1, result.size());
        verify(studentRepository).findByCareerIdAndStatus(1L, StudentStatus.REGULAR);
    }

    @Test
    void testGetStudentById_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        StudentResponse response = studentService.getStudentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Ana García", response.getFullName());
        assertEquals("10450", response.getCareerCode());
        assertEquals("2021.3", response.getStudyPlanCode());
    }

    @Test
    void testGetStudentById_NotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getStudentById(99L));
    }

    @Test
    void testGetStudentByRun_Success() {
        when(studentRepository.findByRun("12.345.678-9")).thenReturn(Optional.of(sampleStudent));

        StudentResponse response = studentService.getStudentByRun("12.345.678-9");

        assertNotNull(response);
        assertEquals("12.345.678-9", response.getRun());
    }

    @Test
    void testGetStudentByRun_NormalizesInput() {
        when(studentRepository.findByRun("12.345.678-9")).thenReturn(Optional.of(sampleStudent));

        StudentResponse response = studentService.getStudentByRun("  12.345.678-9  ");

        assertNotNull(response);
        verify(studentRepository).findByRun("12.345.678-9");
    }

    @Test
    void testGetStudentByRun_NotFound() {
        when(studentRepository.findByRun("99.999.999-9")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getStudentByRun("99.999.999-9"));
    }

    @Test
    void testGetStudentByEmail_Success() {
        when(studentRepository.findByEmailIgnoreCase("ana.garcia@ipt.cl")).thenReturn(Optional.of(sampleStudent));

        StudentResponse response = studentService.getStudentByEmail("ana.garcia@ipt.cl");

        assertNotNull(response);
        assertEquals("ana.garcia@ipt.cl", response.getEmail());
    }

    @Test
    void testGetStudentByEmail_NotFound() {
        when(studentRepository.findByEmailIgnoreCase("unknown@ipt.cl")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getStudentByEmail("unknown@ipt.cl"));
    }

    // ─── Create ────────────────────────────────────────────────────────────

    @Test
    void testCreateStudent_Success_AutoAssignsCurrentPlan() {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .careerId(1L)
                .build();

        when(studentRepository.existsByRun("12.345.678-9")).thenReturn(false);
        when(studentRepository.existsByEmailIgnoreCase("ana.garcia@ipt.cl")).thenReturn(false);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(activeCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT))
                .thenReturn(Optional.of(currentPlan));
        when(studentRepository.save(any(Student.class))).thenReturn(sampleStudent);

        StudentResponse response = studentService.createStudent(request);

        assertNotNull(response);
        assertEquals(StudentStatus.REGULAR, response.getStatus()); // Rule: starts REGULAR
        assertEquals("2021.3", response.getStudyPlanCode());       // auto-assigned to current plan
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void testCreateStudent_DuplicateRun_ThrowsException() {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Other Name")
                .email("other@ipt.cl")
                .careerId(1L)
                .build();

        when(studentRepository.existsByRun("12.345.678-9")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void testCreateStudent_DuplicateEmail_ThrowsException() {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .run("98.765.432-1")
                .fullName("Other Name")
                .email("ana.garcia@ipt.cl")
                .careerId(1L)
                .build();

        when(studentRepository.existsByRun("98.765.432-1")).thenReturn(false);
        when(studentRepository.existsByEmailIgnoreCase("ana.garcia@ipt.cl")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void testCreateStudent_CareerNotFound_ThrowsException() {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Ana García")
                .email("ana@ipt.cl")
                .careerId(99L)
                .build();

        when(studentRepository.existsByRun("12.345.678-9")).thenReturn(false);
        when(studentRepository.existsByEmailIgnoreCase("ana@ipt.cl")).thenReturn(false);
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.createStudent(request));
    }

    @Test
    void testCreateStudent_InactiveCareer_ThrowsException() {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Ana García")
                .email("ana@ipt.cl")
                .careerId(2L)
                .build();

        when(studentRepository.existsByRun("12.345.678-9")).thenReturn(false);
        when(studentRepository.existsByEmailIgnoreCase("ana@ipt.cl")).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(inactiveCareer));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.createStudent(request));
        assertTrue(ex.getMessage().contains("INACTIVE"));
    }

    @Test
    void testCreateStudent_NoCurrentPlan_ThrowsException() {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Ana García")
                .email("ana@ipt.cl")
                .careerId(1L)
                .build();

        when(studentRepository.existsByRun("12.345.678-9")).thenReturn(false);
        when(studentRepository.existsByEmailIgnoreCase("ana@ipt.cl")).thenReturn(false);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(activeCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(1L, PlanStatus.CURRENT)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.createStudent(request));
        assertTrue(ex.getMessage().contains("CURRENT"));
    }

    // ─── Update ────────────────────────────────────────────────────────────

    @Test
    void testUpdateStudent_Success_BasicFields() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García Updated")
                .email("ana.updated@ipt.cl")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.updated@ipt.cl", 1L)).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = studentService.updateStudent(1L, request);

        assertEquals("Ana García Updated", response.getFullName());
        assertEquals("ana.updated@ipt.cl", response.getEmail());
    }

    @Test
    void testUpdateStudent_DuplicateEmail_ThrowsException() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("taken@ipt.cl")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("taken@ipt.cl", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.updateStudent(1L, request));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void testUpdateStudent_ManualStatusGRADUATED_ThrowsException() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .status(StudentStatus.GRADUATED)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.updateStudent(1L, request));
        assertTrue(ex.getMessage().contains("GRADUATED"));
    }

    @Test
    void testUpdateStudent_ManualStatusEXPELLED_ThrowsException() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .status(StudentStatus.EXPELLED)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.updateStudent(1L, request));
        assertTrue(ex.getMessage().contains("EXPELLED"));
    }

    @Test
    void testUpdateStudent_ChangeCareer_WithNoHistory_Success() {
        Career newCareer = Career.builder().id(3L).code("10470").name("Nueva Carrera").status(CareerStatus.ACTIVE).build();
        StudyPlan newCurrentPlan = StudyPlan.builder().id(20L).code("2022.1").name("Plan 2022.1")
                .status(PlanStatus.CURRENT).career(newCareer).build();

        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .careerId(3L)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);
        doReturn(false).when(studentService).hasEnrollmentsOrAcademicHistory(1L);
        when(careerRepository.findById(3L)).thenReturn(Optional.of(newCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(3L, PlanStatus.CURRENT)).thenReturn(Optional.of(newCurrentPlan));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = studentService.updateStudent(1L, request);

        assertEquals("10470", response.getCareerCode());
        assertEquals("2022.1", response.getStudyPlanCode());
    }

    @Test
    void testUpdateStudent_ChangeCareer_WithHistory_ThrowsException() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .careerId(3L)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);
        doReturn(true).when(studentService).hasEnrollmentsOrAcademicHistory(1L);

        assertThrows(BusinessRuleException.class, () -> studentService.updateStudent(1L, request));
    }

    @Test
    void testUpdateStudent_ChangePlan_ToAnotherCareersPlan_ThrowsException() {
        Career otherCareer = Career.builder().id(99L).code("99999").name("Other").status(CareerStatus.ACTIVE).build();
        StudyPlan otherCareerPlan = StudyPlan.builder().id(50L).code("OCP").name("Other Career Plan")
                .status(PlanStatus.CURRENT).career(otherCareer).build();

        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .studyPlanId(50L)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);
        doReturn(false).when(studentService).hasEnrollmentsOrAcademicHistory(1L);
        when(studyPlanRepository.findById(50L)).thenReturn(Optional.of(otherCareerPlan));

        assertThrows(BusinessRuleException.class, () -> studentService.updateStudent(1L, request));
    }

    @Test
    void testUpdateStudent_ChangeCareer_ToInactiveCareer_ThrowsException() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .careerId(2L)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);
        doReturn(false).when(studentService).hasEnrollmentsOrAcademicHistory(1L);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(inactiveCareer));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.updateStudent(1L, request));
        assertTrue(ex.getMessage().contains("INACTIVE"));
    }

    @Test
    void testUpdateStudent_ChangePlan_ValidPlanSameCareer_Success() {
        StudentUpdateRequest request = StudentUpdateRequest.builder()
                .fullName("Ana García")
                .email("ana.garcia@ipt.cl")
                .studyPlanId(11L)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("ana.garcia@ipt.cl", 1L)).thenReturn(false);
        doReturn(false).when(studentService).hasEnrollmentsOrAcademicHistory(1L);
        when(studyPlanRepository.findById(11L)).thenReturn(Optional.of(otherPlan));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = studentService.updateStudent(1L, request);

        assertEquals("2018.1", response.getStudyPlanCode());
    }

    @Test
    void testUpdateStudent_NotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.updateStudent(99L, StudentUpdateRequest.builder()
                        .fullName("X").email("x@ipt.cl").build()));
    }

    // ─── Status patch ──────────────────────────────────────────────────────

    @Test
    void testUpdateStudentStatus_ToPostponed_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = studentService.updateStudentStatus(1L, StudentStatus.POSTPONED);

        assertEquals(StudentStatus.POSTPONED, response.getStatus());
    }

    @Test
    void testUpdateStudentStatus_ToTemporaryWithdrawal_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = studentService.updateStudentStatus(1L, StudentStatus.TEMPORARY_WITHDRAWAL);

        assertEquals(StudentStatus.TEMPORARY_WITHDRAWAL, response.getStatus());
    }

    @Test
    void testUpdateStudentStatus_ToGraduated_ThrowsException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.updateStudentStatus(1L, StudentStatus.GRADUATED));
        assertTrue(ex.getMessage().contains("GRADUATED"));
    }

    @Test
    void testUpdateStudentStatus_ToExpelled_ThrowsException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.updateStudentStatus(1L, StudentStatus.EXPELLED));
        assertTrue(ex.getMessage().contains("EXPELLED"));
    }

    @Test
    void testUpdateStudentStatus_NotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.updateStudentStatus(99L, StudentStatus.POSTPONED));
    }

    // ─── Delete ────────────────────────────────────────────────────────────

    @Test
    void testDeleteStudent_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        doReturn(false).when(studentService).hasEnrollmentsOrAcademicHistory(1L);

        assertDoesNotThrow(() -> studentService.deleteStudent(1L));
        verify(studentRepository).delete(sampleStudent);
    }

    @Test
    void testDeleteStudent_WithHistory_ThrowsException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        doReturn(true).when(studentService).hasEnrollmentsOrAcademicHistory(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> studentService.deleteStudent(1L));
        assertTrue(ex.getMessage().contains("academic enrollments or history"));
        verify(studentRepository, never()).delete(any());
    }

    @Test
    void testDeleteStudent_NotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.deleteStudent(99L));
    }
}

