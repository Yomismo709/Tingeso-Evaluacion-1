package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionOfferingResponse {
    private SectionResponse section;
    private Long enrolledCount;
    private Long availableSeats;
}