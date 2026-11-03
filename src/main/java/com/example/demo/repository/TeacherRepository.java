package com.example.demo.repository;

import com.example.demo.model.Teacher;
import com.example.demo.model.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByRun(String run);

    Optional<Teacher> findByEmailIgnoreCase(String email);

    boolean existsByRun(String run);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Teacher> findByStatus(TeacherStatus status);
}

