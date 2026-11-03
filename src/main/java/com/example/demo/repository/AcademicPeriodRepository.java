package com.example.demo.repository;

import com.example.demo.model.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    Optional<AcademicPeriod> findByYearAndSemester(Integer year, Integer semester);

    boolean existsByYearAndSemester(Integer year, Integer semester);
}