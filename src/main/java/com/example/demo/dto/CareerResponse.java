package com.example.demo.dto;

import com.example.demo.model.CareerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer durationSemesters;
    private CareerStatus status;
    private Long currentStudyPlanId;
    private String currentStudyPlanCode;
    private String currentStudyPlanName;
}
