package com.example.demo.repository;

import com.example.demo.model.Career;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long> {
    boolean existsByCode(String code);
    Optional<Career> findByCode(String code);
}
