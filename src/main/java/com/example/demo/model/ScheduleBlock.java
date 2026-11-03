package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScheduleBlock {

    @Column(name = "day_code", nullable = false, length = 1)
    private String dayCode;

    @Column(name = "module_number", nullable = false)
    private Integer module;
}