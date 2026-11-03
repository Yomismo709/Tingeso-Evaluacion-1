package com.example.demo.repository;

import com.example.demo.model.StudentMatriculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentMatriculationRepository extends JpaRepository<StudentMatriculation, Long> {

    Optional<StudentMatriculation> findByStudentIdAndAcademicPeriodId(Long studentId, Long periodId);

    boolean existsByStudentId(Long studentId);

    boolean existsByStudentIdAndMatriculatedTrue(Long studentId);

    boolean existsByStudentIdAndAcademicPeriodIdAndMatriculatedTrue(Long studentId, Long periodId);

    boolean existsByAcademicPeriodId(Long periodId);
}