package com.example.demo.dto;

import com.example.demo.model.AcademicPeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicPeriodClosureResponse {
    private Long academicPeriodId;
    private String academicPeriodCode;
    private AcademicPeriodStatus status;
    private Integer gradedEnrollments;
    private Integer passedEnrollments;
    private Integer failedEnrollments;
    private Integer expelledStudents;
    private Integer graduatedStudents;
}