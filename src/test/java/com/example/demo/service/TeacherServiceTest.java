package com.example.demo.service;

import com.example.demo.dto.TeacherCreateRequest;
import com.example.demo.dto.TeacherResponse;
import com.example.demo.dto.TeacherUpdateRequest;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Teacher;
import com.example.demo.model.TeacherStatus;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.SectionRepository;
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
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    @Spy
    private TeacherService teacherService;

    private Teacher sampleTeacher;

    @BeforeEach
    void setUp() {
        sampleTeacher = Teacher.builder()
                .id(1L)
                .run("12.345.678-9")
                .fullName("Gonzalo Valenzuela")
                .email("gonzalo.valenzuela@ipt.cl")
                .professionalTitle("Ingeniero Civil en Informática")
                .academicDegree("Magíster en Ciencias de la Computación")
                .status(TeacherStatus.ACTIVE)
                .build();
    }

    @Test
    void testGetAllTeachers_NoFilter() {
        when(teacherRepository.findAll()).thenReturn(List.of(sampleTeacher));

        List<TeacherResponse> result = teacherService.getAllTeachers(null);

        assertEquals(1, result.size());
        assertEquals("12.345.678-9", result.get(0).getRun());
        verify(teacherRepository).findAll();
        verify(teacherRepository, never()).findByStatus(any());
    }

    @Test
    void testGetAllTeachers_FilteredByStatus() {
        when(teacherRepository.findByStatus(TeacherStatus.ACTIVE)).thenReturn(List.of(sampleTeacher));

        List<TeacherResponse> result = teacherService.getAllTeachers(TeacherStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(TeacherStatus.ACTIVE, result.get(0).getStatus());
        verify(teacherRepository).findByStatus(TeacherStatus.ACTIVE);
    }

    @Test
    void testGetTeacherById_Success() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));

        TeacherResponse response = teacherService.getTeacherById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Gonzalo Valenzuela", response.getFullName());
    }

    @Test
    void testGetTeacherById_NotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.getTeacherById(99L));
    }

    @Test
    void testGetTeacherByRun_Success() {
        when(teacherRepository.findByRun("12.345.678-9")).thenReturn(Optional.of(sampleTeacher));

        TeacherResponse response = teacherService.getTeacherByRun("12.345.678-9");

        assertNotNull(response);
        assertEquals("12.345.678-9", response.getRun());
    }

    @Test
    void testGetTeacherByRun_NotFound() {
        when(teacherRepository.findByRun("99.999.999-9")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.getTeacherByRun("99.999.999-9"));
    }

    @Test
    void testGetTeacherByEmail_Success() {
        when(teacherRepository.findByEmailIgnoreCase("gonzalo.valenzuela@ipt.cl")).thenReturn(Optional.of(sampleTeacher));

        TeacherResponse response = teacherService.getTeacherByEmail("gonzalo.valenzuela@ipt.cl");

        assertNotNull(response);
        assertEquals("gonzalo.valenzuela@ipt.cl", response.getEmail());
    }

    @Test
    void testGetTeacherByEmail_NotFound() {
        when(teacherRepository.findByEmailIgnoreCase("unknown@ipt.cl")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.getTeacherByEmail("unknown@ipt.cl"));
    }

    @Test
    void testCreateTeacher_Success() {
        TeacherCreateRequest request = TeacherCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Gonzalo Valenzuela")
                .email("gonzalo.valenzuela@ipt.cl")
                .professionalTitle("Ingeniero Civil en Informática")
                .academicDegree("Magíster")
                .build();

        when(teacherRepository.existsByRun("12.345.678-9")).thenReturn(false);
        when(teacherRepository.existsByEmailIgnoreCase("gonzalo.valenzuela@ipt.cl")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(sampleTeacher);

        TeacherResponse response = teacherService.createTeacher(request);

        assertNotNull(response);
        assertEquals("12.345.678-9", response.getRun());
        assertEquals(TeacherStatus.ACTIVE, response.getStatus()); // Rule: initially ACTIVE
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void testCreateTeacher_DuplicateRun_ThrowsException() {
        TeacherCreateRequest request = TeacherCreateRequest.builder()
                .run("12.345.678-9")
                .fullName("Other Name")
                .email("other@ipt.cl")
                .professionalTitle("Ingeniero")
                .academicDegree("Licenciado")
                .build();

        when(teacherRepository.existsByRun("12.345.678-9")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teacherService.createTeacher(request));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void testCreateTeacher_DuplicateEmail_ThrowsException() {
        TeacherCreateRequest request = TeacherCreateRequest.builder()
                .run("98.765.432-1")
                .fullName("Other Name")
                .email("gonzalo.valenzuela@ipt.cl")
                .professionalTitle("Ingeniero")
                .academicDegree("Licenciado")
                .build();

        when(teacherRepository.existsByRun("98.765.432-1")).thenReturn(false);
        when(teacherRepository.existsByEmailIgnoreCase("gonzalo.valenzuela@ipt.cl")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teacherService.createTeacher(request));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void testUpdateTeacher_Success() {
        TeacherUpdateRequest updateRequest = TeacherUpdateRequest.builder()
                .fullName("Gonzalo Valenzuela Updated")
                .email("gonzalo.updated@ipt.cl")
                .professionalTitle("Doctor en Computación")
                .academicDegree("PhD")
                .status(TeacherStatus.ACTIVE)
                .build();

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        when(teacherRepository.existsByEmailIgnoreCaseAndIdNot("gonzalo.updated@ipt.cl", 1L)).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherResponse response = teacherService.updateTeacher(1L, updateRequest);

        assertEquals("Gonzalo Valenzuela Updated", response.getFullName());
        assertEquals("gonzalo.updated@ipt.cl", response.getEmail());
        assertEquals("PhD", response.getAcademicDegree());
    }

    @Test
    void testUpdateTeacher_DuplicateEmail_ThrowsException() {
        TeacherUpdateRequest updateRequest = TeacherUpdateRequest.builder()
                .fullName("Gonzalo Valenzuela")
                .email("taken.email@ipt.cl")
                .professionalTitle("Ingeniero")
                .academicDegree("Licenciado")
                .status(TeacherStatus.ACTIVE)
                .build();

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        when(teacherRepository.existsByEmailIgnoreCaseAndIdNot("taken.email@ipt.cl", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teacherService.updateTeacher(1L, updateRequest));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void testUpdateTeacher_DeactivateWithOpenSections_ThrowsException() {
        TeacherUpdateRequest updateRequest = TeacherUpdateRequest.builder()
                .fullName("Gonzalo Valenzuela")
                .email("gonzalo.valenzuela@ipt.cl")
                .professionalTitle("Ingeniero")
                .academicDegree("Magíster")
                .status(TeacherStatus.INACTIVE)
                .build();

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        when(teacherRepository.existsByEmailIgnoreCaseAndIdNot("gonzalo.valenzuela@ipt.cl", 1L)).thenReturn(false);
        doReturn(true).when(teacherService).hasSectionsInOpenAcademicPeriod(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> teacherService.updateTeacher(1L, updateRequest));
        assertTrue(ex.getMessage().contains("OPEN academic period"));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void testUpdateTeacherStatus_Success() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        doReturn(false).when(teacherService).hasSectionsInOpenAcademicPeriod(1L);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherResponse response = teacherService.updateTeacherStatus(1L, TeacherStatus.INACTIVE);

        assertEquals(TeacherStatus.INACTIVE, response.getStatus());
    }

    @Test
    void testUpdateTeacherStatus_DeactivateWithOpenSections_ThrowsException() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        doReturn(true).when(teacherService).hasSectionsInOpenAcademicPeriod(1L);

        assertThrows(BusinessRuleException.class, 
                () -> teacherService.updateTeacherStatus(1L, TeacherStatus.INACTIVE));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void testDeleteTeacher_Success() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        doReturn(false).when(teacherService).hasAssociatedSections(1L);

        assertDoesNotThrow(() -> teacherService.deleteTeacher(1L));
        verify(teacherRepository).delete(sampleTeacher);
    }

    @Test
    void testDeleteTeacher_HasAssociatedSections_ThrowsException() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(sampleTeacher));
        doReturn(true).when(teacherService).hasAssociatedSections(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> teacherService.deleteTeacher(1L));
        assertTrue(ex.getMessage().contains("associated sections"));
        verify(teacherRepository, never()).delete(any());
    }

    @Test
    void testDeleteTeacher_NotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.deleteTeacher(99L));
    }
}

