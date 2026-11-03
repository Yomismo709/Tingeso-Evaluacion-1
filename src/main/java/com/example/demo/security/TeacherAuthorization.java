package com.example.demo.security;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Teacher;
import com.example.demo.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherAuthorization {

    private final TeacherRepository teacherRepository;

    public void requireOwnerOrAdmin(Long teacherId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }
        Teacher authenticatedTeacher = findAuthenticatedTeacher(authentication);
        if (!authenticatedTeacher.getId().equals(teacherId)) {
            throw new AccessDeniedException("Teachers may only access sections assigned to their own account.");
        }
    }

    public void requireOwnRun(String run, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }
        Teacher teacher = findAuthenticatedTeacher(authentication);
        if (!teacher.getRun().equalsIgnoreCase(run.trim())) {
            throw new AccessDeniedException("Teachers may only access their own account.");
        }
    }

    private Teacher findAuthenticatedTeacher(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("A Keycloak username is required to identify the teacher.");
        }
        String run = jwt.getClaimAsString("preferred_username");
        if (run == null || run.isBlank()) {
            throw new AccessDeniedException("The access token does not contain a teacher RUN.");
        }
        return teacherRepository.findByRun(run.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No teacher is associated with the authenticated RUN."));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}