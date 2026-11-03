package com.example.demo.controller;

import com.example.demo.dto.EnrollmentCreateRequest;
import com.example.demo.dto.EnrollmentResponse;
import com.example.demo.dto.FinalGradeRequest;
import com.example.demo.dto.MatriculationRequest;
import com.example.demo.dto.SectionOfferingResponse;
import com.example.demo.service.EnrollmentService;
import com.example.demo.security.StudentAuthorization;
import com.example.demo.security.TeacherAuthorization;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods/{periodId}")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Academic enrollment management (Épica 6)")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentAuthorization studentAuthorization;
    private final TeacherAuthorization teacherAuthorization;

    @PutMapping("/matriculations")
    @Operation(summary = "Register or update a student's administrative matriculation")
    public ResponseEntity<Void> setMatriculation(
            @PathVariable Long periodId,
            @Valid @RequestBody MatriculationRequest request) {
        enrollmentService.setMatriculation(periodId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/offer")
    @Operation(summary = "List the academic offer for a student's study plan")
    public ResponseEntity<List<SectionOfferingResponse>> getOffer(
            @PathVariable Long periodId,
            @RequestParam Long studentId,
            Authentication authentication) {
        studentAuthorization.requireOwnerOrAdmin(studentId, authentication);
        return ResponseEntity.ok(enrollmentService.getOffer(studentId, periodId));
    }

    @PostMapping("/enrollments")
    @Operation(summary = "Enroll a student in a set of sections")
    public ResponseEntity<List<EnrollmentResponse>> enroll(
            @PathVariable Long periodId,
            @Valid @RequestBody EnrollmentCreateRequest request,
            Authentication authentication) {
        studentAuthorization.requireOwnerOrAdmin(request.getStudentId(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(periodId, request));
    }

    @GetMapping("/students/{studentId}/enrollments")
    @Operation(summary = "List a student's enrollments for an academic period")
    public ResponseEntity<List<EnrollmentResponse>> getEnrollments(
            @PathVariable Long periodId,
            @PathVariable Long studentId,
            Authentication authentication) {
        studentAuthorization.requireOwnerOrAdmin(studentId, authentication);
        return ResponseEntity.ok(enrollmentService.getEnrollments(studentId, periodId));
    }

    @DeleteMapping("/students/{studentId}/enrollments/{enrollmentId}")
    @Operation(summary = "Cancel an enrollment while the period is open")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable Long periodId,
            @PathVariable Long studentId,
            @PathVariable Long enrollmentId,
            Authentication authentication) {
        studentAuthorization.requireOwnerOrAdmin(studentId, authentication);
        return ResponseEntity.ok(enrollmentService.cancel(periodId, studentId, enrollmentId));
    }

    @GetMapping("/students/{studentId}/history")
    @Operation(summary = "List the student's graded academic history")
    public ResponseEntity<List<EnrollmentResponse>> getAcademicHistory(@PathVariable Long studentId, Authentication authentication) {
        studentAuthorization.requireOwnerOrAdmin(studentId, authentication);
        return ResponseEntity.ok(enrollmentService.getAcademicHistory(studentId));
    }

    @GetMapping("/sections/{sectionId}/enrollments")
    @Operation(summary = "List active enrollments in a teacher's section")
    public ResponseEntity<List<EnrollmentResponse>> getSectionEnrollments(
            @PathVariable Long periodId,
            @PathVariable Long sectionId,
            @RequestParam Long teacherId,
            Authentication authentication) {
        teacherAuthorization.requireOwnerOrAdmin(teacherId, authentication);
        return ResponseEntity.ok(enrollmentService.getSectionEnrollments(periodId, sectionId, teacherId));
    }

    @PutMapping("/sections/{sectionId}/enrollments/{enrollmentId}/grade")
    @Operation(summary = "Record or update an enrolled student's final grade")
    public ResponseEntity<EnrollmentResponse> recordFinalGrade(
            @PathVariable Long periodId,
            @PathVariable Long sectionId,
            @PathVariable Long enrollmentId,
            @Valid @RequestBody FinalGradeRequest request,
            Authentication authentication) {
        teacherAuthorization.requireOwnerOrAdmin(request.getTeacherId(), authentication);
        return ResponseEntity.ok(enrollmentService.recordFinalGrade(periodId, sectionId, enrollmentId, request));
    }
}