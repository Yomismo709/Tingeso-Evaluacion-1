package com.example.demo.dto;

import com.example.demo.model.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanResponse {
    private Long id;
    private String code;
    private String name;
    private PlanStatus status;
    private Long careerId;
    private String careerCode;
    private String careerName;
}
