package com.example.demo.dto;

import com.example.demo.model.AcademicPeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {
    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long academicPeriodId;
    private String academicPeriodCode;
    private AcademicPeriodStatus academicPeriodStatus;
    private Long teacherId;
    private String teacherName;
    private Integer capacity;
    private List<ScheduleBlockRequest> scheduleBlocks;
}