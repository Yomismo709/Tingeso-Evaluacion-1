package com.example.demo.repository;

import com.example.demo.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByStudyPlanId(Long studyPlanId);

    List<Subject> findByStudyPlanIdOrderBySemesterAscCodeAsc(Long studyPlanId);

    List<Subject> findByStudyPlanIdAndSemester(Long studyPlanId, Integer semester);

    Optional<Subject> findByIdAndStudyPlanId(Long id, Long studyPlanId);

    Optional<Subject> findByCodeAndStudyPlanId(String code, Long studyPlanId);

    boolean existsByCodeAndStudyPlanId(String code, Long studyPlanId);

    long countByStudyPlanId(Long studyPlanId);

    List<Subject> findByPrerequisitesContaining(Subject prerequisite);

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.prerequisites WHERE s.id = :id")
    Optional<Subject> findByIdWithPrerequisites(@Param("id") Long id);

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.prerequisites WHERE s.studyPlan.id = :studyPlanId ORDER BY s.semester ASC, s.code ASC")
    List<Subject> findByStudyPlanIdWithPrerequisites(@Param("studyPlanId") Long studyPlanId);
}

