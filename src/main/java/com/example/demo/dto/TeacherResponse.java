package com.example.demo.dto;

import com.example.demo.model.TeacherStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {
    private Long id;
    private String run;
    private String fullName;
    private String email;
    private String professionalTitle;
    private String academicDegree;
    private TeacherStatus status;
}

