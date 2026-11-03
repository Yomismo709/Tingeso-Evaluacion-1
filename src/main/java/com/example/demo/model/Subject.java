package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "subjects",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_code_study_plan", columnNames = {"code", "study_plan_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "hours_theory", nullable = false)
    private Integer hoursTheory;

    @Column(name = "hours_exercise", nullable = false)
    private Integer hoursExercise;

    @Column(name = "hours_lab", nullable = false)
    private Integer hoursLab;

    @Column(name = "credits_sct", nullable = false)
    private Integer creditsSct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subject_prerequisites",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private Set<Subject> prerequisites = new HashSet<>();

    public int getTotalWeeklyHours() {
        int t = hoursTheory != null ? hoursTheory : 0;
        int e = hoursExercise != null ? hoursExercise : 0;
        int l = hoursLab != null ? hoursLab : 0;
        return t + e + l;
    }

    public String getTelFormatted() {
        return (hoursTheory != null ? hoursTheory : 0) + "-" +
               (hoursExercise != null ? hoursExercise : 0) + "-" +
               (hoursLab != null ? hoursLab : 0);
    }
}

