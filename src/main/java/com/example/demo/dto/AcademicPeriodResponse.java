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
public class AcademicPeriodResponse {
    private Long id;
    private Integer year;
    private Integer semester;
    private String code;
    private AcademicPeriodStatus status;
}