package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcademicPeriodControllerTest {

        @Autowired
        private MockMvc mockMvc;

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
        private EnrollmentRepository enrollmentRepository;

        private AcademicPeriod currentPeriod;
        private AcademicPeriod previousPeriod;
        private StudyPlan plan;
        private Student studentGraduating;
        private Student studentFailing;
        private Subject sub1;
        private Subject sub2;
        private Section sec1;
        private Section sec2;

        @BeforeEach
        void setUp() {
                // 1. Configurar Períodos Académicos
                previousPeriod = periodRepository.save(AcademicPeriod.builder()
                                .year(2026).semester(2).status(AcademicPeriodStatus.CLOSED).build());
                currentPeriod = periodRepository.save(AcademicPeriod.builder()
                                .year(2027).semester(1).status(AcademicPeriodStatus.OPEN).build());

                // 2. Configurar Carrera y Plan de Estudios
                Career career = careerRepository.save(Career.builder()
                                .code("10450").name("Ingeniería").status(CareerStatus.ACTIVE).build());
                plan = planRepository.save(StudyPlan.builder()
                                .code("2027.1").name("Plan 2027.1").career(career).status(PlanStatus.CURRENT).build());

                // 3. Configurar Asignaturas (Plan completo de 2 asignaturas para probar
                // graduación)
                sub1 = subjectRepository.save(createSubject("SUB1", 1, plan));
                sub2 = subjectRepository.save(createSubject("SUB2", 1, plan));

                // 4. Configurar Estudiantes
                studentGraduating = studentRepository.save(Student.builder()
                                .run("11.111.111-1").fullName("Graduating Student").email("grad@ipt.cl")
                                .status(StudentStatus.REGULAR).career(career).studyPlan(plan).build());

                studentFailing = studentRepository.save(Student.builder()
                                .run("22.222.222-2").fullName("Failing Student").email("fail@ipt.cl")
                                .status(StudentStatus.REGULAR).career(career).studyPlan(plan).build());

                // 5. Configurar Docente y Secciones
                Teacher teacher = teacherRepository.save(Teacher.builder()
                                .run("99.999.999-9").fullName("Profesor Test").email("profe@ipt.cl")
                                .professionalTitle("Ingeniero").academicDegree("Magíster").status(TeacherStatus.ACTIVE)
                                .build());

                sec1 = sectionRepository.save(Section.builder()
                                .subject(sub1).academicPeriod(currentPeriod).teacher(teacher).capacity(30)
                                .scheduleBlocks(Set.of(new ScheduleBlock("L", 1))).build());
                sec2 = sectionRepository.save(Section.builder()
                                .subject(sub2).academicPeriod(currentPeriod).teacher(teacher).capacity(30)
                                .scheduleBlocks(Set.of(new ScheduleBlock("M", 2))).build());

                // 6. Configurar reprobación previa para el escenario de expulsión
                Section prevSec1 = sectionRepository.save(Section.builder()
                                .subject(sub1).academicPeriod(previousPeriod).teacher(teacher).capacity(30)
                                .scheduleBlocks(Set.of(new ScheduleBlock("L", 1))).build());

                enrollmentRepository.save(Enrollment.builder()
                                .student(studentFailing).subject(sub1).section(prevSec1).academicPeriod(previousPeriod)
                                .status(EnrollmentStatus.ACTIVE).finalGrade(new BigDecimal("3.5"))
                                .enrolledAt(LocalDateTime.now().minusMonths(6)).build());
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
        void closePeriod_SuccessfulWithAllGrades() throws Exception {
                enrollmentRepository.save(Enrollment.builder()
                                .student(studentGraduating).subject(sub1).section(sec1).academicPeriod(currentPeriod)
                                .status(EnrollmentStatus.ACTIVE).finalGrade(new BigDecimal("5.0"))
                                .enrolledAt(LocalDateTime.now()).build());

                mockMvc.perform(post("/api/academic-periods/{periodId}/close", currentPeriod.getId())
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CLOSED"))
                                .andExpect(jsonPath("$.gradedEnrollments").value(1))
                                .andExpect(jsonPath("$.passedEnrollments").value(1))
                                .andExpect(jsonPath("$.failedEnrollments").value(0));
        }

        @Test
        void closePeriod_SuccessfulExpelsStudent() throws Exception {
                enrollmentRepository.save(Enrollment.builder()
                                .student(studentFailing).subject(sub1).section(sec1).academicPeriod(currentPeriod)
                                .status(EnrollmentStatus.ACTIVE).finalGrade(new BigDecimal("3.9")) // Segunda
                                                                                                   // reprobación
                                .enrolledAt(LocalDateTime.now()).build());

                mockMvc.perform(post("/api/academic-periods/{periodId}/close", currentPeriod.getId())
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CLOSED"))
                                .andExpect(jsonPath("$.failedEnrollments").value(1))
                                .andExpect(jsonPath("$.expelledStudents").value(1));
        }

        @Test
        void closePeriod_SuccessfulGraduatesStudent() throws Exception {
                enrollmentRepository.save(Enrollment.builder()
                                .student(studentGraduating).subject(sub1).section(sec1).academicPeriod(currentPeriod)
                                .status(EnrollmentStatus.ACTIVE).finalGrade(new BigDecimal("4.0"))
                                .enrolledAt(LocalDateTime.now()).build());
                enrollmentRepository.save(Enrollment.builder()
                                .student(studentGraduating).subject(sub2).section(sec2).academicPeriod(currentPeriod)
                                .status(EnrollmentStatus.ACTIVE).finalGrade(new BigDecimal("6.0"))
                                .enrolledAt(LocalDateTime.now()).build());

                mockMvc.perform(post("/api/academic-periods/{periodId}/close", currentPeriod.getId())
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CLOSED"))
                                .andExpect(jsonPath("$.passedEnrollments").value(2))
                                .andExpect(jsonPath("$.graduatedStudents").value(1));
        }

        // --- ESCENARIOS DE RECHAZO ---

        @Test
        void closePeriod_RejectsMissingGrades() throws Exception {
                enrollmentRepository.save(Enrollment.builder()
                                .student(studentGraduating).subject(sub1).section(sec1).academicPeriod(currentPeriod)
                                .status(EnrollmentStatus.ACTIVE).finalGrade(null) // Nota faltante
                                .enrolledAt(LocalDateTime.now()).build());

                mockMvc.perform(post("/api/academic-periods/{periodId}/close", currentPeriod.getId())
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void closePeriod_RejectsAlreadyClosedPeriod() throws Exception {
                currentPeriod.setStatus(AcademicPeriodStatus.CLOSED);
                periodRepository.save(currentPeriod);

                mockMvc.perform(post("/api/academic-periods/{periodId}/close", currentPeriod.getId())
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void deletePeriod_RejectsPeriodWithSections() throws Exception {
                // currentPeriod ya tiene sec1 y sec2 configuradas en el setUp()
                mockMvc.perform(delete("/api/academic-periods/{periodId}", currentPeriod.getId())
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }
}
