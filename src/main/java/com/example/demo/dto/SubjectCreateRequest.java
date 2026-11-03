package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectCreateRequest {

    @NotBlank(message = "Subject code is mandatory")
    @Size(max = 50, message = "Subject code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Subject name is mandatory")
    @Size(max = 150, message = "Subject name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Semester is mandatory")
    @Min(value = 1, message = "Semester must be between 1 and 4")
    @Max(value = 4, message = "Semester must be between 1 and 4")
    private Integer semester;

    @NotNull(message = "Theory hours are mandatory")
    @Min(value = 0, message = "Theory hours cannot be negative")
    private Integer hoursTheory;

    @NotNull(message = "Exercise hours are mandatory")
    @Min(value = 0, message = "Exercise hours cannot be negative")
    private Integer hoursExercise;

    @NotNull(message = "Laboratory hours are mandatory")
    @Min(value = 0, message = "Laboratory hours cannot be negative")
    private Integer hoursLab;

    @NotNull(message = "SCT credits are mandatory")
    @Min(value = 1, message = "SCT credits must be greater than 0")
    @Max(value = 7, message = "SCT credits must be less than or equal to 7")
    private Integer creditsSct;

    @Builder.Default
    private List<Long> prerequisiteIds = new ArrayList<>();

    @Builder.Default
    private List<String> prerequisiteCodes = new ArrayList<>();
}

