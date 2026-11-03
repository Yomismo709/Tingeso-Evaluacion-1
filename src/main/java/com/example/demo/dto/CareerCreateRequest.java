package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerCreateRequest {

    @NotBlank(message = "Career code is mandatory")
    @Size(max = 50, message = "Career code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Career name is mandatory")
    @Size(max = 150, message = "Career name cannot exceed 150 characters")
    private String name;

    private String description;

    @Builder.Default
    @Min(value = 1, message = "Duration must be at least 1 semester")
    @Max(value = 12, message = "Duration cannot exceed 12 semesters")
    private Integer durationSemesters = 4;
}
