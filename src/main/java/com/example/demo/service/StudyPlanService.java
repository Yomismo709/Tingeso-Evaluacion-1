package com.example.demo.service;

import com.example.demo.dto.StudyPlanCreateRequest;
import com.example.demo.dto.StudyPlanResponse;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Career;
import com.example.demo.model.PlanStatus;
import com.example.demo.model.StudyPlan;
import com.example.demo.repository.CareerRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerRepository careerRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<StudyPlanResponse> getStudyPlansByCareer(Long careerId) {
        if (!careerRepository.existsById(careerId)) {
            throw new ResourceNotFoundException("Career not found with ID: " + careerId);
        }
        return studyPlanRepository.findByCareerId(careerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse getStudyPlanById(Long id) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + id));
        return mapToResponse(plan);
    }

    @Transactional
    public StudyPlanResponse createStudyPlan(Long careerId, StudyPlanCreateRequest request) {
        Career career = careerRepository.findById(careerId)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with ID: " + careerId));

        if (studyPlanRepository.existsByCodeAndCareerId(request.getCode(), careerId)) {
            throw new DuplicateResourceException("Study plan with code '" + request.getCode()
                    + "' already exists for career: " + career.getName());
        }

        // Rule: Only one plan can be CURRENT at a time. If set as current, deactivate
        // previous current plan.
        if (request.isSetAsCurrent()) {
            deactivateCurrentPlanForCareer(careerId);
        }

        StudyPlan studyPlan = StudyPlan.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .status(request.isSetAsCurrent() ? PlanStatus.CURRENT : PlanStatus.NOT_CURRENT)
                .career(career)
                .build();

        StudyPlan saved = studyPlanRepository.save(studyPlan);
        return mapToResponse(saved);
    }

    @Transactional
    public StudyPlanResponse setStudyPlanAsCurrent(Long careerId, Long planId) {
        if (!careerRepository.existsById(careerId)) {
            throw new ResourceNotFoundException("Career not found with ID: " + careerId);
        }

        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + planId));

        if (!plan.getCareer().getId().equals(careerId)) {
            throw new ResourceNotFoundException("Study plan does not belong to the specified career.");
        }

        deactivateCurrentPlanForCareer(careerId);

        plan.setStatus(PlanStatus.CURRENT);
        StudyPlan updated = studyPlanRepository.save(plan);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteStudyPlan(Long id) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with ID: " + id));

        // Rule: Un plan de estudios que se encuentre asociado a estudiantes no podrá
        // ser eliminado físicamente del sistema.
        long associatedStudents = studentRepository.countByStudyPlanId(id);
        if (associatedStudents > 0) {
            throw new BusinessRuleException("Study plan cannot be physically deleted because it has "
                    + associatedStudents + " associated student(s).");
        }

        studyPlanRepository.delete(plan);
    }

    private void deactivateCurrentPlanForCareer(Long careerId) {
        Optional<StudyPlan> currentPlanOpt = studyPlanRepository.findByCareerIdAndStatus(careerId, PlanStatus.CURRENT);
        if (currentPlanOpt.isPresent()) {
            StudyPlan currentPlan = currentPlanOpt.get();
            currentPlan.setStatus(PlanStatus.NOT_CURRENT);
            studyPlanRepository.save(currentPlan);
        }
    }

    public StudyPlanResponse mapToResponse(StudyPlan plan) {
        return StudyPlanResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .status(plan.getStatus())
                .careerId(plan.getCareer().getId())
                .careerCode(plan.getCareer().getCode())
                .careerName(plan.getCareer().getName())
                .build();
    }
}
