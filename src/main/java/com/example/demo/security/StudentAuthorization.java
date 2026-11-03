package com.example.demo.security;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentAuthorization {

    private final StudentRepository studentRepository;

    public void requireOwnerOrAdmin(Long studentId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }
        Student authenticatedStudent = findAuthenticatedStudent(authentication);
        if (!authenticatedStudent.getId().equals(studentId)) {
            throw new AccessDeniedException("Students may only access their own academic records.");
        }
    }

    public void requireOwnRun(String run, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }
        Student authenticatedStudent = findAuthenticatedStudent(authentication);
        if (!authenticatedStudent.getRun().equalsIgnoreCase(run.trim())) {
            throw new AccessDeniedException("Students may only access their own academic records.");
        }
    }

    public void requireStudyPlanOwnerOrAdmin(Long studyPlanId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_TEACHER")) {
            return;
        }
        Student authenticatedStudent = findAuthenticatedStudent(authentication);
        if (!authenticatedStudent.getStudyPlan().getId().equals(studyPlanId)) {
            throw new AccessDeniedException("Students may only access their assigned study plan.");
        }
    }

    private Student findAuthenticatedStudent(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("A Keycloak username is required to identify the student.");
        }
        String run = jwt.getClaimAsString("preferred_username");
        if (run == null || run.isBlank()) {
            throw new AccessDeniedException("The access token does not contain a student RUN.");
        }
        return studentRepository.findByRun(run.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No student is associated with the authenticated RUN."));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}