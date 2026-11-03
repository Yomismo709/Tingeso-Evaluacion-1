package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalGradeRequest {

    @NotNull
    private Long teacherId;

    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("7.0")
    @Digits(integer = 1, fraction = 1)
    private BigDecimal grade;
}