package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionCreateRequest {

    @NotNull
    private Long subjectId;

    @NotNull
    private Long teacherId;

    @NotNull
    @Min(1)
    @Max(60)
    private Integer capacity;

    @NotEmpty
    @Valid
    private List<ScheduleBlockRequest> scheduleBlocks;
}