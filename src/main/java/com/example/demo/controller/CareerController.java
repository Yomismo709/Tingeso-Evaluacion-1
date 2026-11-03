package com.example.demo.controller;

import com.example.demo.dto.CareerCreateRequest;
import com.example.demo.dto.CareerResponse;
import com.example.demo.dto.CareerUpdateRequest;
import com.example.demo.service.CareerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
@Tag(name = "Careers", description = "Endpoints for managing academic careers in SIGA IPT")
public class CareerController {

    private final CareerService careerService;

    @GetMapping
    @Operation(summary = "Get all careers")
    public ResponseEntity<List<CareerResponse>> getAllCareers() {
        return ResponseEntity.ok(careerService.getAllCareers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a career by ID")
    public ResponseEntity<CareerResponse> getCareerById(@PathVariable Long id) {
        return ResponseEntity.ok(careerService.getCareerById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get a career by unique code")
    public ResponseEntity<CareerResponse> getCareerByCode(@PathVariable String code) {
        return ResponseEntity.ok(careerService.getCareerByCode(code));
    }

    @PostMapping
    @Operation(summary = "Create a new academic career")
    public ResponseEntity<CareerResponse> createCareer(@Valid @RequestBody CareerCreateRequest request) {
        CareerResponse created = careerService.createCareer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update career details and status")
    public ResponseEntity<CareerResponse> updateCareer(
            @PathVariable Long id,
            @Valid @RequestBody CareerUpdateRequest request) {
        return ResponseEntity.ok(careerService.updateCareer(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a career (only allowed if it has no associated plans or students)")
    public ResponseEntity<Void> deleteCareer(@PathVariable Long id) {
        careerService.deleteCareer(id);
        return ResponseEntity.noContent().build();
    }
}
