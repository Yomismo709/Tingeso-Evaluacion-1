package com.example.demo.service;

import com.example.demo.dto.CareerCreateRequest;
import com.example.demo.dto.CareerResponse;
import com.example.demo.dto.CareerUpdateRequest;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Career;
import com.example.demo.model.CareerStatus;
import com.example.demo.model.PlanStatus;
import com.example.demo.model.StudyPlan;
import com.example.demo.repository.CareerRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<CareerResponse> getAllCareers() {
        return careerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CareerResponse getCareerById(Long id) {
        Career career = careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with ID: " + id));
        return mapToResponse(career);
    }

    @Transactional(readOnly = true)
    public CareerResponse getCareerByCode(String code) {
        Career career = careerRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with code: " + code));
        return mapToResponse(career);
    }

    @Transactional
    public CareerResponse createCareer(CareerCreateRequest request) {
        if (careerRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("A career with code '" + request.getCode() + "' already exists.");
        }

        Career career = Career.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .durationSemesters(request.getDurationSemesters() != null ? request.getDurationSemesters() : 4)
                .status(CareerStatus.ACTIVE)
                .build();

        Career saved = careerRepository.save(career);
        return mapToResponse(saved);
    }

    @Transactional
    public CareerResponse updateCareer(Long id, CareerUpdateRequest request) {
        Career career = careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with ID: " + id));

        career.setName(request.getName().trim());
        career.setDescription(request.getDescription());
        career.setStatus(request.getStatus());

        Career updated = careerRepository.save(career);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCareer(Long id) {
        Career career = careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with ID: " + id));

        // Rule: A career with study plans (or students) cannot be physically deleted; only status can be modified.
        long associatedPlans = studyPlanRepository.countByCareerId(id);
        if (associatedPlans > 0) {
            throw new BusinessRuleException("Career cannot be physically deleted because it has " 
                    + associatedPlans + " associated study plan(s). Please change its status to INACTIVE instead.");
        }

        long associatedStudents = studentRepository.countByCareerId(id);
        if (associatedStudents > 0) {
            throw new BusinessRuleException("Career cannot be physically deleted because it has " 
                    + associatedStudents + " associated student(s). Please change its status to INACTIVE instead.");
        }

        careerRepository.delete(career);
    }

    public CareerResponse mapToResponse(Career career) {
        StudyPlan currentPlan = studyPlanRepository
                .findByCareerIdAndStatus(career.getId(), PlanStatus.CURRENT)
                .orElse(null);

        return CareerResponse.builder()
                .id(career.getId())
                .code(career.getCode())
                .name(career.getName())
                .description(career.getDescription())
                .durationSemesters(career.getDurationSemesters())
                .status(career.getStatus())
                .currentStudyPlanId(currentPlan != null ? currentPlan.getId() : null)
                .currentStudyPlanCode(currentPlan != null ? currentPlan.getCode() : null)
                .currentStudyPlanName(currentPlan != null ? currentPlan.getName() : null)
                .build();
    }
}
