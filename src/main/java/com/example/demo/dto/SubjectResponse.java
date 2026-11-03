package com.example.demo.dto;

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
public class SubjectResponse {
    private Long id;
    private String code;
    private String name;
    private Integer semester;
    private Integer hoursTheory;
    private Integer hoursExercise;
    private Integer hoursLab;
    private String tel;
    private Integer totalWeeklyHours;
    private Integer creditsSct;
    private Long studyPlanId;
    private String studyPlanCode;
    private String studyPlanName;

    @Builder.Default
    private List<SubjectSummaryResponse> prerequisites = new ArrayList<>();
}

