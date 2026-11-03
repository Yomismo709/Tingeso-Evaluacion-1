package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "sections",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_section_subject_period",
        columnNames = {"subject_id", "academic_period_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_period_id", nullable = false)
    private AcademicPeriod academicPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private Integer capacity;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
        name = "section_schedule_blocks",
        joinColumns = @JoinColumn(name = "section_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_section_schedule_block",
            columnNames = {"section_id", "day_code", "module_number"}
        )
    )
    private Set<ScheduleBlock> scheduleBlocks = new HashSet<>();
}