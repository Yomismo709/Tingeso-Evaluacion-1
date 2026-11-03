package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.StudyPlan;
import com.example.demo.model.Subject;
import com.example.demo.repository.StudyPlanRepository;
import com.example.demo.repository.SubjectRepository;
import com.example.demo.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByStudyPlan(Long studyPlanId) {
        verifyStudyPlanExists(studyPlanId);
        return subjectRepository.findByStudyPlanIdWithPrerequisites(studyPlanId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByStudyPlanAndSemester(Long studyPlanId, Integer semester) {
        verifyStudyPlanExists(studyPlanId);
        if (semester < 1 || semester > 4) {
            throw new BusinessRuleException("Semester must be between 1 and 4.");
        }
        return subjectRepository.findByStudyPlanIdAndSemester(studyPlanId, semester).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long studyPlanId, Long id) {
        verifyStudyPlanExists(studyPlanId);
        Subject subject = subjectRepository.findByIdWithPrerequisites(id)
                .filter(s -> s.getStudyPlan().getId().equals(studyPlanId))
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id + " in study plan ID: " + studyPlanId));
        return mapToResponse(subject);
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectByCode(Long studyPlanId, String code) {
        verifyStudyPlanExists(studyPlanId);
        Subject subject = subjectRepository.findByCodeAndStudyPlanId(code.trim().toUpperCase(), studyPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with code: " + code + " in study plan ID: " + studyPlanId));
        return mapToResponse(subject);
    }

    @Transactional
    public SubjectResponse createSubject(Long studyPlanId, SubjectCreateRequest request) {
        StudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + studyPlanId));

        String normalizedCode = request.getCode().trim().toUpperCase();
        if (subjectRepository.existsByCodeAndStudyPlanId(normalizedCode, studyPlanId)) {
            throw new DuplicateResourceException("A subject with code '" + normalizedCode + "' already exists in this study plan.");
        }

        validateWeeklyHours(request.getHoursTheory(), request.getHoursExercise(), request.getHoursLab());
        validateSct(request.getCreditsSct());
        validateSemester(request.getSemester());

        Set<Subject> prerequisites = resolvePrerequisites(
                studyPlanId,
                null,
                request.getSemester(),
                request.getPrerequisiteIds(),
                request.getPrerequisiteCodes()
        );

        Subject subject = Subject.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .semester(request.getSemester())
                .hoursTheory(request.getHoursTheory())
                .hoursExercise(request.getHoursExercise())
                .hoursLab(request.getHoursLab())
                .creditsSct(request.getCreditsSct())
                .studyPlan(studyPlan)
                .prerequisites(prerequisites)
                .build();

        Subject saved = subjectRepository.save(subject);
        return mapToResponse(saved);
    }

    @Transactional
    public SubjectResponse updateSubject(Long studyPlanId, Long id, SubjectUpdateRequest request) {
        verifyStudyPlanExists(studyPlanId);
        Subject subject = subjectRepository.findByIdWithPrerequisites(id)
                .filter(s -> s.getStudyPlan().getId().equals(studyPlanId))
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id + " in study plan ID: " + studyPlanId));

        validateWeeklyHours(request.getHoursTheory(), request.getHoursExercise(), request.getHoursLab());
        validateSct(request.getCreditsSct());
        validateSemester(request.getSemester());

        // Rule: If semester changed, ensure dependent subjects don't become invalid
        if (!subject.getSemester().equals(request.getSemester())) {
            List<Subject> dependents = subjectRepository.findByPrerequisitesContaining(subject);
            for (Subject dependent : dependents) {
                if (dependent.getSemester() <= request.getSemester()) {
                    throw new BusinessRuleException("Cannot move subject to semester " + request.getSemester() 
                            + " because dependent subject '" + dependent.getCode() 
                            + "' is in semester " + dependent.getSemester() + ".");
                }
            }
        }

        Set<Subject> prerequisites = resolvePrerequisites(
                studyPlanId,
                subject.getId(),
                request.getSemester(),
                request.getPrerequisiteIds(),
                request.getPrerequisiteCodes()
        );

        subject.setName(request.getName().trim());
        subject.setSemester(request.getSemester());
        subject.setHoursTheory(request.getHoursTheory());
        subject.setHoursExercise(request.getHoursExercise());
        subject.setHoursLab(request.getHoursLab());
        subject.setCreditsSct(request.getCreditsSct());
        subject.setPrerequisites(prerequisites);

        Subject updated = subjectRepository.save(subject);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteSubject(Long studyPlanId, Long id) {
        verifyStudyPlanExists(studyPlanId);
        Subject subject = subjectRepository.findById(id)
                .filter(s -> s.getStudyPlan().getId().equals(studyPlanId))
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id + " in study plan ID: " + studyPlanId));

        // Rule: cannot delete subject if other subjects in the study plan depend on it as a prerequisite
        List<Subject> dependents = subjectRepository.findByPrerequisitesContaining(subject);
        if (!dependents.isEmpty()) {
            String dependentCodes = dependents.stream().map(Subject::getCode).collect(Collectors.joining(", "));
            throw new BusinessRuleException("Cannot physically delete subject '" + subject.getCode() 
                    + "' because it is a prerequisite for: " + dependentCodes);
        }

        if (enrollmentRepository.existsBySubjectId(subject.getId())) {
            throw new BusinessRuleException("Subject cannot be physically deleted because it has enrollment history.");
        }

        subjectRepository.delete(subject);
    }

    @Transactional
    public SubjectResponse addPrerequisite(Long studyPlanId, Long subjectId, Long prerequisiteId) {
        verifyStudyPlanExists(studyPlanId);
        Subject subject = subjectRepository.findByIdWithPrerequisites(subjectId)
                .filter(s -> s.getStudyPlan().getId().equals(studyPlanId))
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));

        if (subject.getId().equals(prerequisiteId)) {
            throw new BusinessRuleException("A subject cannot be a prerequisite of itself.");
        }

        if (subject.getPrerequisites().size() >= 3) {
            throw new BusinessRuleException("A subject cannot have more than 3 prerequisites.");
        }

        Subject prerequisite = subjectRepository.findById(prerequisiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisite subject not found with ID: " + prerequisiteId));

        if (!prerequisite.getStudyPlan().getId().equals(studyPlanId)) {
            throw new BusinessRuleException("Prerequisite must belong to the same study plan.");
        }

        if (prerequisite.getSemester() >= subject.getSemester()) {
            throw new BusinessRuleException("Prerequisite '" + prerequisite.getCode() 
                    + "' (Semester " + prerequisite.getSemester() 
                    + ") must belong to a previous semester than subject semester (" + subject.getSemester() + ").");
        }

        subject.getPrerequisites().add(prerequisite);
        Subject updated = subjectRepository.save(subject);
        return mapToResponse(updated);
    }

    @Transactional
    public SubjectResponse removePrerequisite(Long studyPlanId, Long subjectId, Long prerequisiteId) {
        verifyStudyPlanExists(studyPlanId);
        Subject subject = subjectRepository.findByIdWithPrerequisites(subjectId)
                .filter(s -> s.getStudyPlan().getId().equals(studyPlanId))
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));

        subject.getPrerequisites().removeIf(p -> p.getId().equals(prerequisiteId));
        Subject updated = subjectRepository.save(subject);
        return mapToResponse(updated);
    }

    private void validateWeeklyHours(Integer theory, Integer exercise, Integer lab) {
        if (theory == null || exercise == null || lab == null) {
            throw new BusinessRuleException("Theory, exercise, and lab hours must not be null.");
        }
        if (theory < 0 || exercise < 0 || lab < 0) {
            throw new BusinessRuleException("Weekly theory, exercise, and lab hours cannot be negative.");
        }
        int total = theory + exercise + lab;
        if (total >= 8) {
            throw new BusinessRuleException("Total weekly hours (T + E + L) must be strictly less than 8 hours. Provided total: " + total);
        }
    }

    private void validateSct(Integer sct) {
        if (sct == null || sct <= 0 || sct > 7) {
            throw new BusinessRuleException("SCT credits must be greater than 0 and less than or equal to 7.");
        }
    }

    private void validateSemester(Integer semester) {
        if (semester == null || semester < 1 || semester > 4) {
            throw new BusinessRuleException("Semester must be between 1 and 4.");
        }
    }

    private Set<Subject> resolvePrerequisites(
            Long studyPlanId,
            Long currentSubjectId,
            Integer subjectSemester,
            List<Long> prerequisiteIds,
            List<String> prerequisiteCodes) {

        Set<Subject> prerequisites = new HashSet<>();
        Set<Long> resolvedIds = new HashSet<>();

        if (prerequisiteIds != null) {
            for (Long prereqId : prerequisiteIds) {
                if (prereqId == null) continue;
                if (currentSubjectId != null && prereqId.equals(currentSubjectId)) {
                    throw new BusinessRuleException("A subject cannot be a prerequisite of itself.");
                }
                Subject prereq = subjectRepository.findById(prereqId)
                        .orElseThrow(() -> new ResourceNotFoundException("Prerequisite not found with ID: " + prereqId));
                validatePrerequisiteProperties(prereq, studyPlanId, subjectSemester);
                prerequisites.add(prereq);
                resolvedIds.add(prereq.getId());
            }
        }

        if (prerequisiteCodes != null) {
            for (String code : prerequisiteCodes) {
                if (code == null || code.trim().isEmpty()) continue;
                String normalized = code.trim().toUpperCase();
                Subject prereq = subjectRepository.findByCodeAndStudyPlanId(normalized, studyPlanId)
                        .orElseThrow(() -> new ResourceNotFoundException("Prerequisite not found with code: " + normalized + " in study plan ID: " + studyPlanId));

                if (currentSubjectId != null && prereq.getId().equals(currentSubjectId)) {
                    throw new BusinessRuleException("A subject cannot be a prerequisite of itself.");
                }

                if (!resolvedIds.contains(prereq.getId())) {
                    validatePrerequisiteProperties(prereq, studyPlanId, subjectSemester);
                    prerequisites.add(prereq);
                    resolvedIds.add(prereq.getId());
                }
            }
        }

        if (prerequisites.size() > 3) {
            throw new BusinessRuleException("A subject cannot have more than 3 prerequisites. Provided: " + prerequisites.size());
        }

        return prerequisites;
    }

    private void validatePrerequisiteProperties(Subject prereq, Long expectedStudyPlanId, Integer targetSemester) {
        if (!prereq.getStudyPlan().getId().equals(expectedStudyPlanId)) {
            throw new BusinessRuleException("Prerequisite '" + prereq.getCode() + "' belongs to a different study plan.");
        }
        if (prereq.getSemester() >= targetSemester) {
            throw new BusinessRuleException("Prerequisite '" + prereq.getCode() 
                    + "' (Semester " + prereq.getSemester() 
                    + ") must belong to a previous semester than subject semester (" + targetSemester + ").");
        }
    }

    private void verifyStudyPlanExists(Long studyPlanId) {
        if (!studyPlanRepository.existsById(studyPlanId)) {
            throw new ResourceNotFoundException("Study plan not found with ID: " + studyPlanId);
        }
    }

    public SubjectResponse mapToResponse(Subject subject) {
        List<SubjectSummaryResponse> prereqResponses = (subject.getPrerequisites() != null)
                ? subject.getPrerequisites().stream()
                        .sorted(Comparator.comparing(Subject::getSemester).thenComparing(Subject::getCode))
                        .map(p -> SubjectSummaryResponse.builder()
                                .id(p.getId())
                                .code(p.getCode())
                                .name(p.getName())
                                .semester(p.getSemester())
                                .tel(p.getTelFormatted())
                                .creditsSct(p.getCreditsSct())
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return SubjectResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .semester(subject.getSemester())
                .hoursTheory(subject.getHoursTheory())
                .hoursExercise(subject.getHoursExercise())
                .hoursLab(subject.getHoursLab())
                .tel(subject.getTelFormatted())
                .totalWeeklyHours(subject.getTotalWeeklyHours())
                .creditsSct(subject.getCreditsSct())
                .studyPlanId(subject.getStudyPlan().getId())
                .studyPlanCode(subject.getStudyPlan().getCode())
                .studyPlanName(subject.getStudyPlan().getName())
                .prerequisites(prereqResponses)
                .build();
    }
}

