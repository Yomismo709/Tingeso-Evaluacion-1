package com.example.demo.security;

import com.example.demo.dto.AcademicPeriodClosureResponse;
import com.example.demo.model.AcademicPeriodStatus;
import com.example.demo.service.AcademicPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcademicPeriodService academicPeriodService;

    @BeforeEach
    void setUp() {
        when(academicPeriodService.closePeriod(1L)).thenReturn(AcademicPeriodClosureResponse.builder()
                .academicPeriodId(1L)
                .academicPeriodCode("2027-1")
                .status(AcademicPeriodStatus.CLOSED)
                .build());
    }

    @Test
    void periodCloseRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/academic-periods/1/close"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void periodCloseRejectsStudentRole() throws Exception {
        mockMvc.perform(post("/api/academic-periods/1/close")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void periodCloseAllowsAdminRoleToReachBusinessService() throws Exception {
        mockMvc.perform(post("/api/academic-periods/1/close")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk());
    }
}