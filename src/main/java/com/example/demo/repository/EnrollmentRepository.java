package com.example.demo.repository;

import com.example.demo.model.Enrollment;
import com.example.demo.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentIdAndAcademicPeriodId(Long studentId, Long periodId);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

        List<Enrollment> findByAcademicPeriodIdAndSectionIdAndStatus(Long periodId, Long sectionId, EnrollmentStatus status);

        List<Enrollment> findByAcademicPeriodIdAndStatus(Long periodId, EnrollmentStatus status);

        List<Enrollment> findByAcademicPeriodIdAndStatusAndFinalGradeIsNull(Long periodId, EnrollmentStatus status);

    Optional<Enrollment> findByIdAndStudentId(Long id, Long studentId);

        Optional<Enrollment> findByIdAndAcademicPeriodIdAndSectionId(Long id, Long periodId, Long sectionId);

    Optional<Enrollment> findByStudentIdAndSubjectIdAndAcademicPeriodId(Long studentId, Long subjectId, Long periodId);

    boolean existsByStudentId(Long studentId);

    boolean existsBySectionId(Long sectionId);

        boolean existsBySubjectId(Long subjectId);

    boolean existsByAcademicPeriodId(Long periodId);

    long countBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);

    long countByStudentIdAndSubjectIdAndFinalGradeGreaterThanEqualAndStatus(
            Long studentId, Long subjectId, BigDecimal minimumGrade, EnrollmentStatus status);

    long countByStudentIdAndSubjectIdAndFinalGradeLessThanAndStatus(
            Long studentId, Long subjectId, BigDecimal passingGrade, EnrollmentStatus status);

    @Query("select count(e) from Enrollment e where e.student.id = :studentId " +
            "and e.subject.id = :subjectId and e.finalGrade < :passingGrade " +
            "and e.status = :status and e.academicPeriod.id <> :periodId")
    long countPreviousFailures(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("passingGrade") BigDecimal passingGrade,
            @Param("status") EnrollmentStatus status,
            @Param("periodId") Long periodId);

    @Query("select count(distinct e.subject.id) from Enrollment e " +
            "where e.student.id = :studentId and e.status = :status and e.finalGrade >= :minimumGrade")
    long countPassedSubjects(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status,
            @Param("minimumGrade") BigDecimal minimumGrade);

    @Query("select distinct e.subject.id from Enrollment e where e.student.id = :studentId " +
            "and e.status = :status and e.finalGrade >= :minimumGrade")
    List<Long> findPassedSubjectIds(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status,
            @Param("minimumGrade") BigDecimal minimumGrade);

    @Query("select count(e) > 0 from Enrollment e where e.student.id = :studentId " +
            "and e.subject.id = :subjectId and e.status = :status")
    boolean hasActiveEnrollment(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("status") EnrollmentStatus status);
}