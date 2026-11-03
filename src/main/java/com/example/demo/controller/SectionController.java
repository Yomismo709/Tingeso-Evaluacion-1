package com.example.demo.controller;

import com.example.demo.dto.SectionCreateRequest;
import com.example.demo.dto.SectionResponse;
import com.example.demo.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods/{periodId}/sections")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Academic offer and section management (Épica 5)")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    @Operation(summary = "List sections for an academic period")
    public ResponseEntity<List<SectionResponse>> getSections(@PathVariable Long periodId) {
        return ResponseEntity.ok(sectionService.getSections(periodId));
    }

    @GetMapping("/{sectionId}")
    @Operation(summary = "Get a section")
    public ResponseEntity<SectionResponse> getSection(@PathVariable Long periodId, @PathVariable Long sectionId) {
        return ResponseEntity.ok(sectionService.getSection(periodId, sectionId));
    }

    @PostMapping
    @Operation(summary = "Create a section for a subject in an open academic period")
    public ResponseEntity<SectionResponse> createSection(
            @PathVariable Long periodId,
            @Valid @RequestBody SectionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.createSection(periodId, request));
    }

    @PutMapping("/{sectionId}")
    @Operation(summary = "Update a section in an open academic period")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable Long periodId,
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionCreateRequest request) {
        return ResponseEntity.ok(sectionService.updateSection(periodId, sectionId, request));
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "Delete a section that has no enrollments")
    public ResponseEntity<Void> deleteSection(@PathVariable Long periodId, @PathVariable Long sectionId) {
        sectionService.deleteSection(periodId, sectionId);
        return ResponseEntity.noContent().build();
    }
}