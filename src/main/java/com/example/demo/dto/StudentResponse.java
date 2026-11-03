package com.example.demo.dto;

import com.example.demo.model.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String run;
    private String fullName;
    private String email;
    private StudentStatus status;
    private Long careerId;
    private String careerCode;
    private String careerName;
    private Long studyPlanId;
    private String studyPlanCode;
    private String studyPlanName;
}

