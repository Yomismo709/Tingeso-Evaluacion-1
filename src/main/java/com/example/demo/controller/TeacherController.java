package com.example.demo.controller;

import com.example.demo.dto.TeacherCreateRequest;
import com.example.demo.dto.TeacherResponse;
import com.example.demo.dto.TeacherUpdateRequest;
import com.example.demo.model.TeacherStatus;
import com.example.demo.service.TeacherService;
import com.example.demo.service.SectionService;
import com.example.demo.security.TeacherAuthorization;
import com.example.demo.dto.SectionResponse;
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
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Endpoints for managing teachers in SIGA IPT (Épica 4)")
public class TeacherController {

    private final TeacherService teacherService;
    private final SectionService sectionService;
    private final TeacherAuthorization teacherAuthorization;

    @GetMapping
    @Operation(summary = "List all teachers, optionally filtered by status (ACTIVE/INACTIVE)")
    public ResponseEntity<List<TeacherResponse>> getAllTeachers(
            @RequestParam(required = false) TeacherStatus status) {
        return ResponseEntity.ok(teacherService.getAllTeachers(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a teacher by ID")
    public ResponseEntity<TeacherResponse> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @GetMapping("/run/{run}")
    @Operation(summary = "Get a teacher by RUN")
    public ResponseEntity<TeacherResponse> getTeacherByRun(@PathVariable String run, Authentication authentication) {
        teacherAuthorization.requireOwnRun(run, authentication);
        return ResponseEntity.ok(teacherService.getTeacherByRun(run));
    }

    @GetMapping("/{teacherId}/sections")
    @Operation(summary = "List the sections assigned to a teacher")
    public ResponseEntity<List<SectionResponse>> getAssignedSections(
            @PathVariable Long teacherId,
            Authentication authentication) {
        teacherAuthorization.requireOwnerOrAdmin(teacherId, authentication);
        return ResponseEntity.ok(sectionService.getSectionsByTeacher(teacherId));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get a teacher by email")
    public ResponseEntity<TeacherResponse> getTeacherByEmail(@PathVariable String email) {
        return ResponseEntity.ok(teacherService.getTeacherByEmail(email));
    }

    @PostMapping
    @Operation(summary = "Register a new teacher (initially set to ACTIVE status)")
    public ResponseEntity<TeacherResponse> createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
        TeacherResponse created = teacherService.createTeacher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update teacher profile and status")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherUpdateRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update teacher status (cannot be set to INACTIVE if having sections in OPEN period)")
    public ResponseEntity<TeacherResponse> updateTeacherStatus(
            @PathVariable Long id,
            @RequestParam TeacherStatus status) {
        return ResponseEntity.ok(teacherService.updateTeacherStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a teacher (only allowed if no associated sections exist)")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}

