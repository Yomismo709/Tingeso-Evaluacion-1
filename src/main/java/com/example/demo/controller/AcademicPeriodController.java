package com.example.demo.controller;

import com.example.demo.dto.AcademicPeriodCreateRequest;
import com.example.demo.dto.AcademicPeriodClosureResponse;
import com.example.demo.dto.AcademicPeriodResponse;
import com.example.demo.service.AcademicPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
@Tag(name = "Academic Periods", description = "Academic period management (Épica 5)")
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    @GetMapping
    @Operation(summary = "List academic periods")
    public ResponseEntity<List<AcademicPeriodResponse>> getAllPeriods() {
        return ResponseEntity.ok(academicPeriodService.getAllPeriods());
    }

    @GetMapping("/{periodId}")
    @Operation(summary = "Get an academic period")
    public ResponseEntity<AcademicPeriodResponse> getPeriod(@PathVariable Long periodId) {
        return ResponseEntity.ok(academicPeriodService.getPeriod(periodId));
    }

    @PostMapping
    @Operation(summary = "Create an academic period in OPEN status")
    public ResponseEntity<AcademicPeriodResponse> createPeriod(@Valid @RequestBody AcademicPeriodCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicPeriodService.createPeriod(request));
    }

    @DeleteMapping("/{periodId}")
    @Operation(summary = "Delete an academic period that has no sections")
    public ResponseEntity<Void> deletePeriod(@PathVariable Long periodId) {
        academicPeriodService.deletePeriod(periodId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{periodId}/close")
    @Operation(summary = "Close an academic period after all active enrollments have final grades")
    public ResponseEntity<AcademicPeriodClosureResponse> closePeriod(@PathVariable Long periodId) {
        return ResponseEntity.ok(academicPeriodService.closePeriod(periodId));
    }
}