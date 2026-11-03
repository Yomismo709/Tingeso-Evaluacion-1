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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<TeacherResponse> getAllTeachers(TeacherStatus status) {
        List<Teacher> teachers = (status != null) 
                ? teacherRepository.findByStatus(status) 
                : teacherRepository.findAll();

        return teachers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));
        return mapToResponse(teacher);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherByRun(String run) {
        String normalizedRun = normalizeRun(run);
        Teacher teacher = teacherRepository.findByRun(normalizedRun)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with RUN: " + run));
        return mapToResponse(teacher);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with email: " + email));
        return mapToResponse(teacher);
    }

    @Transactional
    public TeacherResponse createTeacher(TeacherCreateRequest request) {
        String normalizedRun = normalizeRun(request.getRun());
        if (teacherRepository.existsByRun(normalizedRun)) {
            throw new DuplicateResourceException("A teacher with RUN '" + request.getRun() + "' already exists.");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (teacherRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("A teacher with email '" + request.getEmail() + "' already exists.");
        }

        Teacher teacher = Teacher.builder()
                .run(normalizedRun)
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .professionalTitle(request.getProfessionalTitle().trim())
                .academicDegree(request.getAcademicDegree().trim())
                .status(TeacherStatus.ACTIVE) // Rule: initially in state ACTIVO
                .build();

        Teacher saved = teacherRepository.save(teacher);
        return mapToResponse(saved);
    }

    @Transactional
    public TeacherResponse updateTeacher(Long id, TeacherUpdateRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (teacherRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException("A teacher with email '" + request.getEmail() + "' already exists.");
        }

        // Rule: A teacher cannot change status to INACTIVE while having sections assigned in an OPEN academic period
        if (teacher.getStatus() == TeacherStatus.ACTIVE && request.getStatus() == TeacherStatus.INACTIVE) {
            validateCanDeactivateTeacher(id);
        }

        teacher.setFullName(request.getFullName().trim());
        teacher.setEmail(normalizedEmail);
        teacher.setProfessionalTitle(request.getProfessionalTitle().trim());
        teacher.setAcademicDegree(request.getAcademicDegree().trim());
        teacher.setStatus(request.getStatus());

        Teacher updated = teacherRepository.save(teacher);
        return mapToResponse(updated);
    }

    @Transactional
    public TeacherResponse updateTeacherStatus(Long id, TeacherStatus newStatus) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));

        if (teacher.getStatus() == TeacherStatus.ACTIVE && newStatus == TeacherStatus.INACTIVE) {
            validateCanDeactivateTeacher(id);
        }

        teacher.setStatus(newStatus);
        Teacher updated = teacherRepository.save(teacher);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));

        // Rule: Un docente que tenga secciones asociadas no podrá ser eliminado físicamente del sistema.
        if (hasAssociatedSections(id)) {
            throw new BusinessRuleException("Teacher cannot be physically deleted because they have associated sections. " +
                    "Please change their status to INACTIVE instead.");
        }

        teacherRepository.delete(teacher);
    }

    /**
     * Checks if the teacher can be deactivated.
     * Rule: Cannot deactivate if having sections assigned in an OPEN period.
     */
    protected void validateCanDeactivateTeacher(Long teacherId) {
        if (hasSectionsInOpenAcademicPeriod(teacherId)) {
            throw new BusinessRuleException("Teacher cannot be deactivated because they have assigned sections in an OPEN academic period.");
        }
    }

    /**
     * Helper to verify if the teacher has any sections assigned in an OPEN period.
     * Will be connected to SectionRepository in Épica 5.
     */
    protected boolean hasSectionsInOpenAcademicPeriod(Long teacherId) {
        return sectionRepository.existsByTeacherIdAndAcademicPeriodStatus(teacherId, com.example.demo.model.AcademicPeriodStatus.OPEN);
    }

    /**
     * Helper to verify if the teacher has any associated sections (historical or current).
     * Will be connected to SectionRepository in Épica 5.
     */
    protected boolean hasAssociatedSections(Long teacherId) {
        return sectionRepository.existsByTeacherId(teacherId);
    }

    public TeacherResponse mapToResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .run(teacher.getRun())
                .fullName(teacher.getFullName())
                .email(teacher.getEmail())
                .professionalTitle(teacher.getProfessionalTitle())
                .academicDegree(teacher.getAcademicDegree())
                .status(teacher.getStatus())
                .build();
    }

    private String normalizeRun(String run) {
        if (run == null) return null;
        return run.trim().toUpperCase();
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }
}

