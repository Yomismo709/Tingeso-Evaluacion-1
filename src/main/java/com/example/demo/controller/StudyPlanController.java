package com.example.demo.controller;

import com.example.demo.dto.StudyPlanCreateRequest;
import com.example.demo.dto.StudyPlanResponse;
import com.example.demo.service.StudyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers/{careerId}/study-plans")
@RequiredArgsConstructor
@Tag(name = "Study Plans", description = "Endpoints for managing study plans belonging to a career")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping
    @Operation(summary = "List all study plans belonging to a career")
    public ResponseEntity<List<StudyPlanResponse>> getStudyPlansByCareer(@PathVariable Long careerId) {
        return ResponseEntity.ok(studyPlanService.getStudyPlansByCareer(careerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a study plan by its ID")
    public ResponseEntity<StudyPlanResponse> getStudyPlanById(
            @PathVariable Long careerId,
            @PathVariable Long id) {
        return ResponseEntity.ok(studyPlanService.getStudyPlanById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new study plan for a career")
    public ResponseEntity<StudyPlanResponse> createStudyPlan(
            @PathVariable Long careerId,
            @Valid @RequestBody StudyPlanCreateRequest request) {
        StudyPlanResponse created = studyPlanService.createStudyPlan(careerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/set-current")
    @Operation(summary = "Set a study plan as current (VIGENTE), marking previous plan as not current")
    public ResponseEntity<StudyPlanResponse> setStudyPlanAsCurrent(
            @PathVariable Long careerId,
            @PathVariable Long id) {
        return ResponseEntity.ok(studyPlanService.setStudyPlanAsCurrent(careerId, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a study plan (only allowed if not associated with students)")
    public ResponseEntity<Void> deleteStudyPlan(
            @PathVariable Long careerId,
            @PathVariable Long id) {
        studyPlanService.deleteStudyPlan(id);
        return ResponseEntity.noContent().build();
    }
}
