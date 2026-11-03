package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "academic_periods",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_academic_period_year_semester",
        columnNames = {"year", "semester"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer semester;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademicPeriodStatus status = AcademicPeriodStatus.OPEN;
}