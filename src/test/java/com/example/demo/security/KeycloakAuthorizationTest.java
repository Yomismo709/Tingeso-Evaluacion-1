package com.example.demo.security;

import com.example.demo.config.SecurityConfig;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Student;
import com.example.demo.model.Teacher;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthorizationTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private StudentAuthorization studentAuthorization;

    @Test
    void jwtConverterMapsKeycloakRealmRolesAndUsername() {
        Jwt jwt = token("admin-run", List.of("ADMIN", "STUDENT", "offline_access"));
        AbstractAuthenticationToken authentication = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertNotNull(authentication);
        assertEquals("admin-run", authentication.getName());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        assertFalse(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_offline_access")));
    }

    @Test
    void jwtConverterReturnsNoApplicationRolesWhenClaimIsMissing() {
        Jwt jwt = token("student-run", List.of());
        jwt = Jwt.withTokenValue(jwt.getTokenValue())
                .header("alg", "none")
                .claim("preferred_username", "student-run")
                .build();
        AbstractAuthenticationToken authentication = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
            .noneMatch(authority -> authority.getAuthority().startsWith("ROLE_")));
    }

    @Test
    void studentCanAccessOwnRecordByRun() {
        Student student = Student.builder().id(1L).run("12.345.678-9").build();
        when(studentRepository.findByRun("12.345.678-9")).thenReturn(Optional.of(student));

        assertDoesNotThrow(() -> studentAuthorization.requireOwnerOrAdmin(1L,
                authentication("12.345.678-9", "ROLE_STUDENT")));
    }

    @Test
    void studentCanLookUpOnlyTheirOwnRun() {
        Student student = Student.builder().id(1L).run("12.345.678-9").build();
        when(studentRepository.findByRun("12.345.678-9")).thenReturn(Optional.of(student));

        assertDoesNotThrow(() -> studentAuthorization.requireOwnRun("12.345.678-9", authentication("12.345.678-9", "ROLE_STUDENT")));
        assertThrows(AccessDeniedException.class,
                () -> studentAuthorization.requireOwnRun("98.765.432-1", authentication("12.345.678-9", "ROLE_STUDENT")));
    }

    @Test
    void studentCannotAccessAnotherStudentRecord() {
        Student authenticatedStudent = Student.builder().id(1L).run("12.345.678-9").build();
        when(studentRepository.findByRun("12.345.678-9")).thenReturn(Optional.of(authenticatedStudent));

        assertThrows(AccessDeniedException.class, () -> studentAuthorization.requireOwnerOrAdmin(2L,
                authentication("12.345.678-9", "ROLE_STUDENT")));
    }

    @Test
    void adminCanAccessAnyStudentWithoutRunLookup() {
        assertDoesNotThrow(() -> studentAuthorization.requireOwnerOrAdmin(999L,
                authentication("admin-run", "ROLE_ADMIN")));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void studentLookupRejectsUnlinkedKeycloakAccount() {
        when(studentRepository.findByRun("UNKNOWN-RUN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentAuthorization.requireOwnerOrAdmin(1L,
                authentication("unknown-run", "ROLE_STUDENT")));
    }

    @Test
    void teacherCanOnlyUseOwnAccountId() {
        TeacherAuthorization authorization = new TeacherAuthorization(teacherRepository);
        Teacher teacher = Teacher.builder().id(3L).run("98.765.432-1").build();
        when(teacherRepository.findByRun("98.765.432-1")).thenReturn(Optional.of(teacher));

        assertDoesNotThrow(() -> authorization.requireOwnerOrAdmin(3L,
                authentication("98.765.432-1", "ROLE_TEACHER")));
        assertThrows(AccessDeniedException.class, () -> authorization.requireOwnerOrAdmin(4L,
                authentication("98.765.432-1", "ROLE_TEACHER")));
    }

    @Test
    void accessDeniedIsReturnedAsForbiddenResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students/2");
        var response = new GlobalExceptionHandler().handleAccessDenied(
                new AccessDeniedException("Forbidden"), request);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", ((ErrorResponse) response.getBody()).getMessage());
    }

    private JwtAuthenticationToken authentication(String run, String role) {
        return new JwtAuthenticationToken(token(run, List.of()), List.of(new SimpleGrantedAuthority(role)));
    }

    private Jwt token(String username, List<String> realmRoles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", username)
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", realmRoles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}