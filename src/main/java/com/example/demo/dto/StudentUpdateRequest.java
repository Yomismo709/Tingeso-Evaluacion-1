package com.example.demo.dto;

import com.example.demo.model.StudentStatus;
import jakarta.validation.constraints.Email;
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
public class StudentUpdateRequest {

    @NotBlank(message = "Student full name is mandatory")
    @Size(max = 150, message = "Student full name cannot exceed 150 characters")
    private String fullName;

    @NotBlank(message = "Student email is mandatory")
    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    private Long careerId;

    private Long studyPlanId;

    private StudentStatus status;
}

