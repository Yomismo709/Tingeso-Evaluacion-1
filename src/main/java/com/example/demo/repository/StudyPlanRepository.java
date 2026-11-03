package com.example.demo.repository;

import com.example.demo.model.PlanStatus;
import com.example.demo.model.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByCareerId(Long careerId);
    Optional<StudyPlan> findByCareerIdAndStatus(Long careerId, PlanStatus status);
    boolean existsByCodeAndCareerId(String code, Long careerId);
    long countByCareerId(Long careerId);
}
