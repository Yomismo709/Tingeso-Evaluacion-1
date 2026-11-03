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
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.StudentMatriculationRepository;
import com.example.demo.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentMatriculationRepository matriculationRepository;

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents(Long careerId, StudentStatus status) {
        List<Student> students;
        if (careerId != null && status != null) {
            students = studentRepository.findByCareerIdAndStatus(careerId, status);
        } else if (careerId != null) {
            students = studentRepository.findByCareerId(careerId);
        } else if (status != null) {
            students = studentRepository.findByStatus(status);
        } else {
            students = studentRepository.findAll();
        }

        return students.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByRun(String run) {
        String normalizedRun = normalizeRun(run);
        Student student = studentRepository.findByRun(normalizedRun)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with RUN: " + run));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        Student student = studentRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));
        return mapToResponse(student);
    }

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        String normalizedRun = normalizeRun(request.getRun());
        if (studentRepository.existsByRun(normalizedRun)) {
            throw new DuplicateResourceException("A student with RUN '" + request.getRun() + "' already exists.");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (studentRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("A student with email '" + request.getEmail() + "' already exists.");
        }

        Career career = careerRepository.findById(request.getCareerId())
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with ID: " + request.getCareerId()));

        // Rule: Una carrera INACTIVA no podrá recibir nuevos estudiantes
        if (career.getStatus() == CareerStatus.INACTIVE) {
            throw new BusinessRuleException("Cannot register a student in an INACTIVE career: " + career.getName());
        }

        // Rule: El estudiante quedará asociado al plan de estudios vigente correspondiente a dicha carrera
        StudyPlan currentPlan = studyPlanRepository.findByCareerIdAndStatus(career.getId(), PlanStatus.CURRENT)
                .orElseThrow(() -> new BusinessRuleException("Career '" + career.getName() 
                        + "' does not have a CURRENT (vigente) study plan. A student must be associated with an active study plan."));

        Student student = Student.builder()
                .run(normalizedRun)
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .status(StudentStatus.REGULAR) // Rule: initially REGULAR
                .career(career)
                .studyPlan(currentPlan)
                .build();

        Student saved = studentRepository.save(student);
        return mapToResponse(saved);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (studentRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException("A student with email '" + request.getEmail() + "' already exists.");
        }

        // Rule: Status manual assignment validation
        if (request.getStatus() != null) {
            validateManualStatusChange(request.getStatus());
            student.setStatus(request.getStatus());
        }

        // Rule: Career and study plan can only be modified if the student has no enrollments or academic history
        boolean careerChanged = request.getCareerId() != null && !request.getCareerId().equals(student.getCareer().getId());
        boolean planChanged = request.getStudyPlanId() != null && !request.getStudyPlanId().equals(student.getStudyPlan().getId());

        if (careerChanged || planChanged) {
            if (hasEnrollmentsOrAcademicHistory(id)) {
                throw new BusinessRuleException("The career and study plan of a student can only be modified while the student has no enrollments or academic history.");
            }

            Career targetCareer = student.getCareer();
            if (careerChanged) {
                targetCareer = careerRepository.findById(request.getCareerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Career not found with ID: " + request.getCareerId()));
                if (targetCareer.getStatus() == CareerStatus.INACTIVE) {
                    throw new BusinessRuleException("Cannot transfer student to an INACTIVE career: " + targetCareer.getName());
                }
                student.setCareer(targetCareer);
            }

            if (planChanged) {
                StudyPlan targetPlan = studyPlanRepository.findById(request.getStudyPlanId())
                        .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + request.getStudyPlanId()));
                if (!targetPlan.getCareer().getId().equals(targetCareer.getId())) {
                    throw new BusinessRuleException("Study plan does not belong to the student's career.");
                }
                student.setStudyPlan(targetPlan);
            } else if (careerChanged) {
                // If career changed but specific plan was not provided, assign the current plan of that career
                StudyPlan currentPlan = studyPlanRepository.findByCareerIdAndStatus(targetCareer.getId(), PlanStatus.CURRENT)
                        .orElseThrow(() -> new BusinessRuleException("Target career has no CURRENT study plan."));
                student.setStudyPlan(currentPlan);
            }
        }

        student.setFullName(request.getFullName().trim());
        student.setEmail(normalizedEmail);

        Student updated = studentRepository.save(student);
        return mapToResponse(updated);
    }

    @Transactional
    public StudentResponse updateStudentStatus(Long id, StudentStatus newStatus) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        validateManualStatusChange(newStatus);

        student.setStatus(newStatus);
        Student updated = studentRepository.save(student);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        // Rule: Un estudiante que posea inscripciones o historial académico no podrá ser eliminado físicamente del sistema
        if (hasEnrollmentsOrAcademicHistory(id)) {
            throw new BusinessRuleException("Student cannot be physically deleted because they have academic enrollments or history.");
        }

        studentRepository.delete(student);
    }

    /**
     * Rule: Los estados EGRESADO y ELIMINADO no podrán ser asignados manualmente.
     * Estos estados serán determinados automáticamente por SIGA IPT durante el proceso de cierre del período académico.
     */
    private void validateManualStatusChange(StudentStatus status) {
        if (status == StudentStatus.GRADUATED || status == StudentStatus.EXPELLED) {
            throw new BusinessRuleException("Statuses GRADUATED and EXPELLED cannot be assigned manually. " +
                    "They are automatically determined by the system during academic period closing.");
        }
    }

    /**
     * Checks if the student has enrollments or academic history.
     * Will be connected to EnrollmentRepository / AcademicHistoryRepository in Épicas 6 and 7.
     */
    protected boolean hasEnrollmentsOrAcademicHistory(Long studentId) {
        return enrollmentRepository.existsByStudentId(studentId) || matriculationRepository.existsByStudentId(studentId);
    }

    public StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .run(student.getRun())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .status(student.getStatus())
                .careerId(student.getCareer().getId())
                .careerCode(student.getCareer().getCode())
                .careerName(student.getCareer().getName())
                .studyPlanId(student.getStudyPlan().getId())
                .studyPlanCode(student.getStudyPlan().getCode())
                .studyPlanName(student.getStudyPlan().getName())
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

