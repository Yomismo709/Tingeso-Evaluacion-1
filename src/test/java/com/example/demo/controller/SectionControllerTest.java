package com.example.demo.controller;

import com.example.demo.dto.ScheduleBlockRequest;
import com.example.demo.dto.SectionCreateRequest;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private AcademicPeriodRepository periodRepository;
    @Autowired
    private CareerRepository careerRepository;
    @Autowired
    private StudyPlanRepository planRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private SectionRepository sectionRepository;

    private AcademicPeriod openPeriod;
    private AcademicPeriod closedPeriod;
    private Subject subject1;
    private Subject subject2;
    private Teacher teacher;
    private Section existingSection;

    @BeforeEach
    void setUp() {
        // 1. Configurar Períodos Académicos
        openPeriod = periodRepository.save(AcademicPeriod.builder()
                .year(2027).semester(1).status(AcademicPeriodStatus.OPEN).build());
        closedPeriod = periodRepository.save(AcademicPeriod.builder()
                .year(2026).semester(2).status(AcademicPeriodStatus.CLOSED).build());

        // 2. Configurar Carrera y Plan
        Career career = careerRepository.save(Career.builder()
                .code("10450").name("Ingeniería").status(CareerStatus.ACTIVE).build());
        StudyPlan plan = planRepository.save(StudyPlan.builder()
                .code("2027.1").name("Plan 2027.1").career(career).status(PlanStatus.CURRENT).build());

        // 3. Configurar Asignaturas (6 horas TEL = 3 bloques requeridos)
        subject1 = subjectRepository.save(Subject.builder()
                .code("SUB1").name("Asignatura 1").semester(1)
                .hoursTheory(2).hoursExercise(2).hoursLab(2).creditsSct(5).studyPlan(plan).build());
        subject2 = subjectRepository.save(Subject.builder()
                .code("SUB2").name("Asignatura 2").semester(1)
                .hoursTheory(2).hoursExercise(2).hoursLab(2).creditsSct(5).studyPlan(plan).build());

        // 4. Configurar Docente
        teacher = teacherRepository.save(Teacher.builder()
                .run("99.999.999-9").fullName("Profesor Test").email("profe@ipt.cl")
                .professionalTitle("Ingeniero").academicDegree("Magíster").status(TeacherStatus.ACTIVE).build());

        // 5. Configurar Sección Existente para probar actualización y conflictos
        existingSection = sectionRepository.save(Section.builder()
                .subject(subject1).academicPeriod(openPeriod).teacher(teacher).capacity(30)
                .scheduleBlocks(Set.of(
                        new ScheduleBlock("L", 1),
                        new ScheduleBlock("M", 2),
                        new ScheduleBlock("W", 3)))
                .build());
    }

    // --- ESCENARIOS DE ÉXITO ---

    @Test
    void createSection_SuccessfulValidScheduleAndCapacity() throws Exception {
        SectionCreateRequest request = SectionCreateRequest.builder()
                .subjectId(subject2.getId())
                .teacherId(teacher.getId())
                .capacity(40)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("J", 1),
                        new ScheduleBlockRequest("J", 2),
                        new ScheduleBlockRequest("V", 1)))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/sections", openPeriod.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.capacity").value(40))
                .andExpect(jsonPath("$.scheduleBlocks.length()").value(3));
    }

    @Test
    void updateSection_SuccessfulCapacityUpdate() throws Exception {
        SectionCreateRequest request = SectionCreateRequest.builder()
                .subjectId(subject1.getId())
                .teacherId(teacher.getId())
                .capacity(50) // Aumentando capacidad
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("L", 1),
                        new ScheduleBlockRequest("M", 2),
                        new ScheduleBlockRequest("W", 3)))
                .build();

        mockMvc.perform(put("/api/academic-periods/{periodId}/sections/{sectionId}", openPeriod.getId(),
                existingSection.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(50));
    }

    @Test
    void createSection_SuccessfulAvailableTeacherNoConflict() throws Exception {
        // El 'teacher' ya tiene asignada 'existingSection' en los bloques (L1, M2, W3).
        // Programaremos una nueva sección para el mismo docente en bloques
        // completamente libres.
        SectionCreateRequest request = SectionCreateRequest.builder()
                .subjectId(subject2.getId()) // subject2 pide 6 horas (3 bloques)
                .teacherId(teacher.getId())
                .capacity(30)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("J", 3),
                        new ScheduleBlockRequest("J", 4),
                        new ScheduleBlockRequest("V", 5)))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/sections", openPeriod.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Verificamos que se asignó correctamente al mismo docente sin arrojar error de
                // conflicto
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.teacherName").value("Profesor Test"));
    }

    // --- ESCENARIOS DE RECHAZO ---

    @Test
    void createSection_RejectsTeacherScheduleConflict() throws Exception {
        SectionCreateRequest request = SectionCreateRequest.builder()
                .subjectId(subject2.getId())
                .teacherId(teacher.getId())
                .capacity(30)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("L", 1), // Conflicto con existingSection
                        new ScheduleBlockRequest("J", 2),
                        new ScheduleBlockRequest("V", 1)))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/sections", openPeriod.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createSection_RejectsClosedPeriod() throws Exception {
        SectionCreateRequest request = SectionCreateRequest.builder()
                .subjectId(subject2.getId())
                .teacherId(teacher.getId())
                .capacity(30)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("J", 1),
                        new ScheduleBlockRequest("J", 2),
                        new ScheduleBlockRequest("V", 1)))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/sections", closedPeriod.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createSection_RejectsIncorrectBlockCountForSubjectHours() throws Exception {
        SectionCreateRequest request = SectionCreateRequest.builder()
                .subjectId(subject2.getId())
                .teacherId(teacher.getId())
                .capacity(30)
                .scheduleBlocks(List.of(
                        new ScheduleBlockRequest("J", 1) // subject2 requiere 3 bloques (6 horas / 2)
                )).build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/sections", openPeriod.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}