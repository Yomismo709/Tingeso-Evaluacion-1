package com.example.demo.controller;

import com.example.demo.dto.EnrollmentCreateRequest;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EnrollmentControllerTest {

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
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentMatriculationRepository matriculationRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private AcademicPeriod period;
    private Student student;
    private Section section1, section2, section3, section4, sectionConflict, sectionFull, sectionPrereq;

    @BeforeEach
    void setUp() {
        // 1. Configurar Período Académico Abierto
        period = periodRepository
                .save(AcademicPeriod.builder().year(2027).semester(1).status(AcademicPeriodStatus.OPEN).build());

        // 2. Configurar Malla y Estudiante
        Career career = careerRepository
                .save(Career.builder().code("10450").name("Ingeniería").status(CareerStatus.ACTIVE).build());
        StudyPlan plan = planRepository
                .save(StudyPlan.builder().code("2027.1").name("Plan 2027.1").career(career).status(PlanStatus.CURRENT).build());
        student = studentRepository.save(Student.builder()
                .run("12.345.678-9")
                .fullName("Estudiante Test")
                .email("estudiante@ipt.cl")
                .status(StudentStatus.REGULAR)
                .career(career)
                .studyPlan(plan)
                .build());
        matriculationRepository.save(
                StudentMatriculation.builder().student(student).academicPeriod(period).matriculated(true).build());

        // 3. Configurar Asignaturas y Prerrequisitos
        Subject sub1 = subjectRepository.save(createSubject("SUB1", 1, plan));
        Subject sub2 = subjectRepository.save(createSubject("SUB2", 1, plan));
        Subject sub3 = subjectRepository.save(createSubject("SUB3", 1, plan));
        Subject sub4 = subjectRepository.save(createSubject("SUB4", 1, plan));
        Subject subConflict = subjectRepository.save(createSubject("SUBC", 1, plan));
        Subject subFull = subjectRepository.save(createSubject("SUBF", 1, plan));

        Subject subPrereqBase = subjectRepository.save(createSubject("PRE1", 1, plan));
        Subject subRequiresPrereq = createSubject("SUBP", 2, plan);
        subRequiresPrereq.setPrerequisites(Set.of(subPrereqBase));
        subRequiresPrereq = subjectRepository.save(subRequiresPrereq);

        // 4. Configurar Docente y Secciones con Horarios
        Teacher teacher = teacherRepository
                .save(Teacher.builder()
                        .run("99.999.999-9")
                        .fullName("Profesor Test")
                        .email("profesor@ipt.cl")
                        .professionalTitle("Ingeniero")
                        .academicDegree("Magíster")
                        .status(TeacherStatus.ACTIVE)
                        .build());

        section1 = sectionRepository.save(Section.builder().subject(sub1).academicPeriod(period).teacher(teacher)
                .capacity(30).scheduleBlocks(Set.of(new ScheduleBlock("L", 1))).build());
        section2 = sectionRepository.save(Section.builder().subject(sub2).academicPeriod(period).teacher(teacher)
                .capacity(30).scheduleBlocks(Set.of(new ScheduleBlock("M", 2))).build());
        section3 = sectionRepository.save(Section.builder().subject(sub3).academicPeriod(period).teacher(teacher)
                .capacity(30).scheduleBlocks(Set.of(new ScheduleBlock("W", 3))).build());
        section4 = sectionRepository.save(Section.builder().subject(sub4).academicPeriod(period).teacher(teacher)
                .capacity(30).scheduleBlocks(Set.of(new ScheduleBlock("J", 4))).build());

        // Sección que topa con section1 (Lunes Módulo 1)
        Teacher teacherConflict = teacherRepository.save(Teacher.builder()
                .run("88.888.888-8")
                .fullName("Profesor Conflicto")
                .email("conflicto@ipt.cl")
                .professionalTitle("Ingeniero")
                .academicDegree("Magíster")
                .status(TeacherStatus.ACTIVE)
                .build());
        sectionConflict = sectionRepository.save(Section.builder().subject(subConflict).academicPeriod(period)
                .teacher(teacherConflict).capacity(30).scheduleBlocks(Set.of(new ScheduleBlock("L", 1))).build());

        // Sección sin cupos
        sectionFull = sectionRepository.save(Section.builder().subject(subFull).academicPeriod(period).teacher(teacher)
                .capacity(0).scheduleBlocks(Set.of(new ScheduleBlock("V", 5))).build());

        // Sección que requiere prerrequisito
        sectionPrereq = sectionRepository.save(Section.builder().subject(subRequiresPrereq).academicPeriod(period)
                .teacher(teacher).capacity(30).scheduleBlocks(Set.of(new ScheduleBlock("V", 6))).build());
    }

    private Subject createSubject(String code, int semester, StudyPlan plan) {
        return Subject.builder()
                .code(code)
                .name("Subject " + code)
                .semester(semester)
                .hoursTheory(2)
                .hoursExercise(0)
                .hoursLab(0)
                .creditsSct(5)
                .studyPlan(plan)
                .build();
    }

    // --- ESCENARIOS DE ÉXITO ---

    @Test
    void enroll_SuccessfulValidSchedule() throws Exception {
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder()
                .studentId(student.getId())
                .sectionIds(List.of(section1.getId(), section2.getId(), section3.getId()))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/enrollments", period.getId())
                .with(jwt().jwt(builder -> builder.claim("preferred_username", "12.345.678-9"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void enroll_SuccessfulMaxAllowedSubjects() throws Exception {
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder()
                .studentId(student.getId())
                .sectionIds(List.of(section1.getId(), section2.getId(), section3.getId(), section4.getId()))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/enrollments", period.getId())
                .with(jwt().jwt(builder -> builder.claim("preferred_username", "12.345.678-9"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void cancel_SuccessfulStatusChange() throws Exception {
        // Preparar una inscripción activa
        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student).subject(section1.getSubject()).section(section1)
                .academicPeriod(period).status(EnrollmentStatus.ACTIVE)
                .enrolledAt(LocalDateTime.now()).build());

        mockMvc.perform(delete("/api/academic-periods/{periodId}/students/{studentId}/enrollments/{enrollmentId}",
                period.getId(), student.getId(), enrollment.getId())
                .with(jwt().jwt(builder -> builder.claim("preferred_username", "12.345.678-9"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
    }

    // --- ESCENARIOS DE RECHAZO ---

    @Test
    void enroll_RejectsScheduleConflict() throws Exception {
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder()
                .studentId(student.getId())
                .sectionIds(List.of(section1.getId(), section2.getId(), sectionConflict.getId())) // section1 y
                                                                                                  // sectionConflict
                                                                                                  // ambas L1
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/enrollments", period.getId())
                .with(jwt().jwt(builder -> builder.claim("preferred_username", "12.345.678-9"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void enroll_RejectsMissingPrerequisites() throws Exception {
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder()
                .studentId(student.getId())
                .sectionIds(List.of(section1.getId(), section2.getId(), sectionPrereq.getId()))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/enrollments", period.getId())
                .with(jwt().jwt(builder -> builder.claim("preferred_username", "12.345.678-9"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enroll_RejectsFullSectionCapacity() throws Exception {
        EnrollmentCreateRequest request = EnrollmentCreateRequest.builder()
                .studentId(student.getId())
                .sectionIds(List.of(section1.getId(), section2.getId(), sectionFull.getId()))
                .build();

        mockMvc.perform(post("/api/academic-periods/{periodId}/enrollments", period.getId())
                .with(jwt().jwt(builder -> builder.claim("preferred_username", "12.345.678-9"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}