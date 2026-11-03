package com.example.demo.repository;

import com.example.demo.model.Student;
import com.example.demo.model.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRun(String run);

    Optional<Student> findByEmailIgnoreCase(String email);

    boolean existsByRun(String run);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Student> findByCareerId(Long careerId);

    List<Student> findByCareerIdAndStatus(Long careerId, StudentStatus status);

    List<Student> findByStudyPlanId(Long studyPlanId);

    List<Student> findByStatus(StudentStatus status);

    long countByCareerId(Long careerId);

    long countByStudyPlanId(Long studyPlanId);
}

