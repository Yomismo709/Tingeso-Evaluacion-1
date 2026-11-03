package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "teachers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_run", columnNames = {"run"}),
        @UniqueConstraint(name = "uk_teacher_email", columnNames = {"email"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String run;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "professional_title", nullable = false, length = 150)
    private String professionalTitle;

    @Column(name = "academic_degree", nullable = false, length = 100)
    private String academicDegree;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherStatus status = TeacherStatus.ACTIVE;
}

