-- =====================================================
-- INSERCIÓN DE ASISTENCIAS PARA EL CURSO MAC
-- =====================================================

-- Variables de configuración
-- Fechas de ejemplo (ajusta según tus necesidades)
-- id_horario 1: LUNES 07:00-08:40
-- id_horario 2: MARTES 09:40-11:30
-- id_horario 3: VIERNES 08:50-10:30

-- =====================================================
-- EJEMPLO 1: Asistencias para una fecha específica
-- =====================================================
USE lumina_bd;
-- Asistencias del LUNES (horario 1) - Fecha: 2025-12-15
INSERT INTO asistencias_estudiante (id_matricula, id_horario, fecha, estado_asistencia, registrado_por, fecha_registro) VALUES
-- Estudiantes PRESENTES
(42, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(43, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(44, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(45, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(46, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(47, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(48, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(49, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(50, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(51, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(52, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(53, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(54, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(55, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(56, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(57, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(58, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(59, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(60, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(61, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(62, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(63, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(64, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(65, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(66, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(67, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(68, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(69, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(70, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(71, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(72, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(73, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(74, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(75, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(76, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(77, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(78, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(79, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(80, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(81, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(82, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(83, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(84, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(85, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(86, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
(87, 1, '2025-12-15', 'PRESENTE', 2, NOW()),
-- Estudiantes con FALTA
(88, 1, '2025-12-15', 'FALTA', 2, NOW()),
(89, 1, '2025-12-15', 'FALTA', 2, NOW()),
(90, 1, '2025-12-15', 'FALTA', 2, NOW()),
(91, 1, '2025-12-15', 'FALTA', 2, NOW()),
(92, 1, '2025-12-15', 'FALTA', 2, NOW());

-- =====================================================
-- EJEMPLO 2: Asistencias para MARTES (horario 2)
-- =====================================================

-- Asistencias del MARTES - Fecha: 2025-12-16
INSERT INTO asistencias_estudiante (id_matricula, id_horario, fecha, estado_asistencia, registrado_por, fecha_registro) VALUES
(42, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(43, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(44, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(45, 2, '2025-12-16', 'FALTA', 2, NOW()),
(46, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(47, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(48, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(49, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(50, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(51, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(52, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(53, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(54, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(55, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(56, 2, '2025-12-16', 'FALTA', 2, NOW()),
(57, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(58, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(59, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(60, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(61, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(62, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(63, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(64, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(65, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(66, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(67, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(68, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(69, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(70, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(71, 2, '2025-12-16', 'FALTA', 2, NOW()),
(72, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(73, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(74, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(75, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(76, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(77, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(78, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(79, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(80, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(81, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(82, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(83, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(84, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(85, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(86, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(87, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(88, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(89, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(90, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(91, 2, '2025-12-16', 'PRESENTE', 2, NOW()),
(92, 2, '2025-12-16', 'PRESENTE', 2, NOW());

-- =====================================================
-- CONSULTAS ÚTILES PARA VERIFICAR
-- =====================================================

-- Ver todas las asistencias del curso MAC
SELECT 
    ae.id_asistencia,
    e.cui,
    e.apellidos_nombres,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    ae.fecha,
    ae.estado_asistencia,
    d.apellidos_nombres AS docente_registro
FROM asistencias_estudiante ae
JOIN matriculas m ON ae.id_matricula = m.id_matricula
JOIN estudiantes e ON m.cui = e.cui
JOIN horarios h ON ae.id_horario = h.id_horario
JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
JOIN docentes d ON ae.registrado_por = d.id_docente
WHERE gc.codigo_curso = 'IS2'
ORDER BY ae.fecha DESC, e.apellidos_nombres;

-- Contar asistencias por fecha
SELECT 
    h.dia_semana,
    ae.fecha,
    COUNT(*) as total_registros,
    SUM(CASE WHEN ae.estado_asistencia = 'PRESENTE' THEN 1 ELSE 0 END) as presentes,
    SUM(CASE WHEN ae.estado_asistencia = 'FALTA' THEN 1 ELSE 0 END) as faltas
FROM asistencias_estudiante ae
JOIN horarios h ON ae.id_horario = h.id_horario
JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
WHERE gc.codigo_curso = 'IS2'
GROUP BY h.dia_semana, ae.fecha
ORDER BY ae.fecha DESC;

-- Ver estudiantes matriculados en MAC grupo 1
SELECT 
    m.id_matricula,
    e.cui,
    e.apellidos_nombres,
    gc.letra_grupo,
    gc.tipo_clase
FROM matriculas m
JOIN estudiantes e ON m.cui = e.cui
JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
WHERE gc.codigo_curso = 'IS2' AND gc.letra_grupo = 'A' AND gc.tipo_clase = 'LABORATORIO'
ORDER BY e.apellidos_nombres;