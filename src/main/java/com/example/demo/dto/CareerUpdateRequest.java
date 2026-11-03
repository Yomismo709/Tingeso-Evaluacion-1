package com.example.demo.dto;

import com.example.demo.model.CareerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerUpdateRequest {

    @NotBlank(message = "Career name is mandatory")
    @Size(max = 150, message = "Career name cannot exceed 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Career status is mandatory")
    private CareerStatus status;
}
