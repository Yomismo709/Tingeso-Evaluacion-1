package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSummaryResponse {
    private Long id;
    private String code;
    private String name;
    private Integer semester;
    private String tel;
    private Integer creditsSct;
}

