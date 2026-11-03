package com.example.demo.dto;

import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.EnrollmentResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private String studentRun;
    private String studentFullName;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectSemester;
    private Long sectionId;
    private Long academicPeriodId;
    private String academicPeriodCode;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime cancelledAt;
    private BigDecimal finalGrade;
    private EnrollmentResult result;
}