-- Insertar Carrera
INSERT INTO careers (code, name, description, status) 
VALUES ('10450', 'Técnico Analista Programador', 'Carrera de TI', 'ACTIVE');

-- Insertar Plan de Estudios (Asumiendo que la carrera tomó el ID 1)
INSERT INTO study_plans (code, status, career_id) 
VALUES ('2021.3', 'CURRENT', 1);

-- Insertar Estudiante (Cambia '12345678-9' por el RUN exacto de tu usuario en Keycloak)
INSERT INTO students (run, full_name, email, status, career_id, study_plan_id) 
VALUES ('12345678-9', 'Juan Pérez', 'juan.perez@example.com', 'REGULAR', 1, 1);