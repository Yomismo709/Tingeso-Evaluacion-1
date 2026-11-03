package com.example.demo.repository;

import com.example.demo.model.AcademicPeriodStatus;
import com.example.demo.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByAcademicPeriodId(Long academicPeriodId);

    List<Section> findByTeacherId(Long teacherId);

    boolean existsByTeacherId(Long teacherId);

    boolean existsBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);

    boolean existsByTeacherIdAndAcademicPeriodStatus(Long teacherId, AcademicPeriodStatus status);

    @Query("select count(s) > 0 from Section s join s.scheduleBlocks block " +
            "where s.teacher.id = :teacherId and s.academicPeriod.id = :periodId " +
            "and s.academicPeriod.status = :status and block.dayCode = :dayCode " +
            "and block.module = :module and (:excludedSectionId is null or s.id <> :excludedSectionId)")
    boolean existsScheduleConflict(
            @Param("teacherId") Long teacherId,
            @Param("periodId") Long periodId,
            @Param("status") AcademicPeriodStatus status,
            @Param("dayCode") String dayCode,
            @Param("module") Integer module,
            @Param("excludedSectionId") Long excludedSectionId);

    boolean existsByAcademicPeriodId(Long academicPeriodId);
}