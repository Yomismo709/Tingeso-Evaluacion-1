package com.example.demo.dto;

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
public class StudyPlanCreateRequest {

    @NotBlank(message = "Study plan code is mandatory")
    @Size(max = 50, message = "Study plan code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Study plan name is mandatory")
    @Size(max = 150, message = "Study plan name cannot exceed 150 characters")
    private String name;

    @Builder.Default
    private boolean setAsCurrent = true;
}
