package com.example.demo.controller;

import com.example.demo.dto.SubjectCreateRequest;
import com.example.demo.dto.SubjectResponse;
import com.example.demo.dto.SubjectUpdateRequest;
import com.example.demo.service.SubjectService;
import com.example.demo.security.StudentAuthorization;
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
@RequestMapping("/api/study-plans/{studyPlanId}/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects & Prerequisites", description = "Endpoints for managing subjects and curriculum prerequisites (Épica 3)")
public class SubjectController {

    private final SubjectService subjectService;
    private final StudentAuthorization studentAuthorization;

    @GetMapping
    @Operation(summary = "List all subjects in a study plan, optionally filtered by semester")
    public ResponseEntity<List<SubjectResponse>> getSubjects(
            @PathVariable Long studyPlanId,
            @RequestParam(required = false) Integer semester,
            Authentication authentication) {
        studentAuthorization.requireStudyPlanOwnerOrAdmin(studyPlanId, authentication);
        if (semester != null) {
            return ResponseEntity.ok(subjectService.getSubjectsByStudyPlanAndSemester(studyPlanId, semester));
        }
        return ResponseEntity.ok(subjectService.getSubjectsByStudyPlan(studyPlanId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subject by ID within a study plan")
    public ResponseEntity<SubjectResponse> getSubjectById(
            @PathVariable Long studyPlanId,
            @PathVariable Long id,
            Authentication authentication) {
        studentAuthorization.requireStudyPlanOwnerOrAdmin(studyPlanId, authentication);
        return ResponseEntity.ok(subjectService.getSubjectById(studyPlanId, id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get a subject by its code within a study plan")
    public ResponseEntity<SubjectResponse> getSubjectByCode(
            @PathVariable Long studyPlanId,
            @PathVariable String code,
            Authentication authentication) {
        studentAuthorization.requireStudyPlanOwnerOrAdmin(studyPlanId, authentication);
        return ResponseEntity.ok(subjectService.getSubjectByCode(studyPlanId, code));
    }

    @PostMapping
    @Operation(summary = "Create a new subject in a study plan")
    public ResponseEntity<SubjectResponse> createSubject(
            @PathVariable Long studyPlanId,
            @Valid @RequestBody SubjectCreateRequest request) {
        SubjectResponse created = subjectService.createSubject(studyPlanId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing subject")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable Long studyPlanId,
            @PathVariable Long id,
            @Valid @RequestBody SubjectUpdateRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(studyPlanId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subject (only allowed if no other subjects or academic histories depend on it)")
    public ResponseEntity<Void> deleteSubject(
            @PathVariable Long studyPlanId,
            @PathVariable Long id) {
        subjectService.deleteSubject(studyPlanId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/prerequisites/{prerequisiteId}")
    @Operation(summary = "Add a prerequisite to a subject")
    public ResponseEntity<SubjectResponse> addPrerequisite(
            @PathVariable Long studyPlanId,
            @PathVariable Long id,
            @PathVariable Long prerequisiteId) {
        return ResponseEntity.ok(subjectService.addPrerequisite(studyPlanId, id, prerequisiteId));
    }

    @DeleteMapping("/{id}/prerequisites/{prerequisiteId}")
    @Operation(summary = "Remove a prerequisite from a subject")
    public ResponseEntity<SubjectResponse> removePrerequisite(
            @PathVariable Long studyPlanId,
            @PathVariable Long id,
            @PathVariable Long prerequisiteId) {
        return ResponseEntity.ok(subjectService.removePrerequisite(studyPlanId, id, prerequisiteId));
    }
}

