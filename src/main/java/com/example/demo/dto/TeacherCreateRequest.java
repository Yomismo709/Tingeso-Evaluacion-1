package com.example.demo.dto;

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
public class TeacherCreateRequest {

    @NotBlank(message = "Teacher RUN is mandatory")
    @Size(max = 20, message = "Teacher RUN cannot exceed 20 characters")
    private String run;

    @NotBlank(message = "Teacher full name is mandatory")
    @Size(max = 150, message = "Teacher full name cannot exceed 150 characters")
    private String fullName;

    @NotBlank(message = "Teacher email is mandatory")
    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Professional title is mandatory")
    @Size(max = 150, message = "Professional title cannot exceed 150 characters")
    private String professionalTitle;

    @NotBlank(message = "Academic degree is mandatory")
    @Size(max = 100, message = "Academic degree cannot exceed 100 characters")
    private String academicDegree;
}

