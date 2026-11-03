package com.example.demo.controller;

import com.example.demo.dto.StudentCreateRequest;
import com.example.demo.dto.StudentResponse;
import com.example.demo.dto.StudentStatusUpdateRequest;
import com.example.demo.dto.StudentUpdateRequest;
import com.example.demo.model.StudentStatus;
import com.example.demo.service.StudentService;
import com.example.demo.security.StudentAuthorization;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Endpoints for managing students and academic statuses in SIGA IPT (Épica 1)")
public class StudentController {

    private final StudentService studentService;
    private final StudentAuthorization studentAuthorization;

    @GetMapping
    @Operation(summary = "List all students, optionally filtered by career and/or status")
    public ResponseEntity<List<StudentResponse>> getAllStudents(
            @RequestParam(required = false) Long careerId,
            @RequestParam(required = false) StudentStatus status) {
        return ResponseEntity.ok(studentService.getAllStudents(careerId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a student by ID")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id, Authentication authentication) {
        studentAuthorization.requireOwnerOrAdmin(id, authentication);
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/run/{run}")
    @Operation(summary = "Get a student by RUN")
    public ResponseEntity<StudentResponse> getStudentByRun(@PathVariable String run, Authentication authentication) {
        studentAuthorization.requireOwnRun(run, authentication);
        return ResponseEntity.ok(studentService.getStudentByRun(run));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get a student by email")
    public ResponseEntity<StudentResponse> getStudentByEmail(@PathVariable String email) {
        return ResponseEntity.ok(studentService.getStudentByEmail(email));
    }

    @PostMapping
    @Operation(summary = "Register a new student (automatically associated with the current study plan and REGULAR status)")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentCreateRequest request) {
        StudentResponse created = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update student details, career/plan (if no history), or status")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update student academic status (REGULAR, POSTPONED, TEMPORARY_WITHDRAWAL)")
    public ResponseEntity<StudentResponse> updateStudentStatus(
            @PathVariable Long id,
            @Valid @RequestBody StudentStatusUpdateRequest request) {
        return ResponseEntity.ok(studentService.updateStudentStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a student (only allowed if they have no enrollments or academic history)")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}

