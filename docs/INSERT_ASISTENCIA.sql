USE lumina_bd;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_generar_asistencia_grupo$$
CREATE PROCEDURE sp_generar_asistencia_grupo(
    IN p_grupo_id INT,
    IN p_fecha DATE,
    IN p_ip VARCHAR(45),
    IN p_tipo_ubicacion VARCHAR(15)
)
BEGIN
    DECLARE v_dia ENUM('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES');
    DECLARE v_id_tema INT DEFAULT NULL;

    /* =====================================================
       0. Determinar día de la semana
       ===================================================== */
    SET v_dia = CASE DAYOFWEEK(p_fecha)
        WHEN 2 THEN 'LUNES'
        WHEN 3 THEN 'MARTES'
        WHEN 4 THEN 'MIERCOLES'
        WHEN 5 THEN 'JUEVES'
        WHEN 6 THEN 'VIERNES'
        ELSE NULL
    END;

    IF v_dia IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La fecha no corresponde a un día lectivo';
    END IF;

    START TRANSACTION;

    /* =====================================================
       1. CONTROL ASISTENCIA
       ===================================================== */
    INSERT IGNORE INTO control_asistencia (
        id_horario,
        fecha,
        hora_apertura,
        hora_cierre,
        estado
    )
    SELECT
        h.id_horario,
        p_fecha,
        h.hora_inicio,
        h.hora_fin,
        'ABIERTO'
    FROM horarios h
    WHERE h.grupo_id = p_grupo_id
      AND h.dia_semana = v_dia
      AND h.estado = 'ACTIVO';

    /* =====================================================
       2. ASISTENCIA DOCENTE
       ===================================================== */
    INSERT IGNORE INTO asistencias_docente (
        id_horario,
        id_docente,
        fecha,
        hora_registro,
        ip_registro,
        tipo_ubicacion,
        presente
    )
    SELECT
        h.id_horario,
        h.id_docente,
        p_fecha,
        CURRENT_TIME(),
        p_ip,
        p_tipo_ubicacion,
        1
    FROM horarios h
    WHERE h.grupo_id = p_grupo_id
      AND h.dia_semana = v_dia
      AND h.estado = 'ACTIVO'
      AND h.id_docente IS NOT NULL;

    /* =====================================================
       3. ASISTENCIA ESTUDIANTES
       ===================================================== */
    INSERT IGNORE INTO asistencias_estudiante (
        id_matricula,
        id_horario,
        fecha,
        estado_asistencia,
        registrado_por
    )
    SELECT
        m.id_matricula,
        h.id_horario,
        p_fecha,
        'PRESENTE',
        h.id_docente
    FROM matriculas m
    JOIN horarios h ON h.grupo_id = m.grupo_id
    WHERE m.grupo_id = p_grupo_id
      AND m.estado_matricula = 'ACTIVO'
      AND h.dia_semana = v_dia
      AND h.estado = 'ACTIVO'
      AND h.id_docente IS NOT NULL;

    /* =====================================================
       4. MARCAR UN TEMA COMO COMPLETADO
       ===================================================== */
    SELECT t.id_tema
    INTO v_id_tema
    FROM temas t
    JOIN unidades u       ON t.unidad_id = u.unidad_id
    JOIN silabos s        ON u.id_silabo = s.id_silabo
    JOIN grupos_curso g   ON g.codigo_curso = s.codigo_curso
                         AND g.id_ciclo = s.id_ciclo
                         AND g.letra_grupo = s.grupo_teoria
    WHERE g.grupo_id = p_grupo_id
      AND t.estado = 'PENDIENTE'
    ORDER BY
        u.numero_unidad,
        t.numero_tema
    LIMIT 1;

    IF v_id_tema IS NOT NULL THEN
        UPDATE temas
        SET
            estado = 'COMPLETADO', -- COMPLETADO
            fecha_completado = p_fecha
        WHERE id_tema = v_id_tema;
    END IF;

    COMMIT;
END$$

DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_eliminar_asistencia_grupo_dia$$
CREATE PROCEDURE sp_eliminar_asistencia_grupo_dia(
    IN p_grupo_id INT,
    IN p_fecha DATE,
    IN p_revertir_tema BOOLEAN
)
BEGIN
    DECLARE v_dia ENUM('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES');

    -- Convertir fecha → día de semana
    SET v_dia = CASE DAYOFWEEK(p_fecha)
        WHEN 2 THEN 'LUNES'
        WHEN 3 THEN 'MARTES'
        WHEN 4 THEN 'MIERCOLES'
        WHEN 5 THEN 'JUEVES'
        WHEN 6 THEN 'VIERNES'
        ELSE NULL
    END;

    IF v_dia IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La fecha no corresponde a un día lectivo';
    END IF;

    START TRANSACTION;

    /* =====================================================
       1. BORRAR ASISTENCIA ESTUDIANTES
       ===================================================== */
    DELETE ae
    FROM asistencias_estudiante ae
    JOIN horarios h ON ae.id_horario = h.id_horario
    WHERE h.grupo_id = p_grupo_id
      AND ae.fecha = p_fecha;

    /* =====================================================
       2. BORRAR ASISTENCIA DOCENTE
       ===================================================== */
    DELETE ad
    FROM asistencias_docente ad
    JOIN horarios h ON ad.id_horario = h.id_horario
    WHERE h.grupo_id = p_grupo_id
      AND ad.fecha = p_fecha;

    /* =====================================================
       3. BORRAR CONTROL ASISTENCIA
       ===================================================== */
    DELETE ca
    FROM control_asistencia ca
    JOIN horarios h ON ca.id_horario = h.id_horario
    WHERE h.grupo_id = p_grupo_id
      AND ca.fecha = p_fecha;

    /* =====================================================
       4. (OPCIONAL) REVERTIR TEMA COMPLETADO
       ===================================================== */
    IF p_revertir_tema THEN
        UPDATE temas t
        JOIN unidades u     ON t.unidad_id = u.unidad_id
        JOIN silabos s      ON u.id_silabo = s.id_silabo
        JOIN grupos_curso g ON g.codigo_curso = s.codigo_curso
                           AND g.id_ciclo = s.id_ciclo
                           AND g.letra_grupo = s.grupo_teoria
        SET
            t.estado = 'PENDIENTE',
            t.fecha_completado = NULL
        WHERE g.grupo_id = p_grupo_id
          AND t.estado = 'COMPLETADO' -- COMPLETADO
        ORDER BY
            t.fecha_completado DESC
        LIMIT 1;
    END IF;

    COMMIT;
END$$

DELIMITER ;

CALL sp_eliminar_asistencia_grupo_dia(57, '2026-01-02', FALSE);

-- MAC 1A  3B (lunes y martes A)
-- MAC LAB 2A 4B
-- SO 56A 57B (Miercoles y Viernes en el B)
-- SO 123A 127B
-- TI2 58 A (Miercoles y Jueves)
-- EDA 55A TEORIA 124 A LAB 128 B LAB (Lunes Jueves A)
-- PC 52 A 125 LAB A (Martes y Jueves
-- IS2 53 A y 54 B
CALL sp_generar_asistencia_grupo(
    52,
    '2025-12-18',
    '192.168.1.20',
    'PRESENCIAL'
);
-- IS2 labs 122 A 126 B
CALL sp_generar_asistencia_grupo_LABS(
    125,
    '2025-12-19',
    '192.168.1.20',
    'PRESENCIAL'
);

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_generar_asistencia_grupo_LABS$$

CREATE PROCEDURE sp_generar_asistencia_grupo_LABS(
    IN p_grupo_id INT,
    IN p_fecha DATE,
    IN p_ip VARCHAR(45),
    IN p_tipo_ubicacion ENUM('PRESENCIAL','VIRTUAL')
)
BEGIN
    DECLARE v_dia ENUM('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES');
    DECLARE v_tipo_clase VARCHAR(20);
    
    /* =====================================================
       0. Validaciones iniciales
       ===================================================== */
    
    -- Verificar que el grupo existe y obtener su tipo
    SELECT tipo_clase INTO v_tipo_clase
    FROM grupos_curso
    WHERE grupo_id = p_grupo_id;
    
    -- Si no se encuentra el grupo, v_tipo_clase será NULL
    IF v_tipo_clase IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El grupo especificado no existe';
    END IF;
    
    -- CAMBIO IMPORTANTE: La columna se llama tipo_clase, no tipo_grupo
    IF v_tipo_clase != 'LABORATORIO' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El grupo especificado no es de tipo LABORATORIO';
    END IF;
    
    -- Determinar día de la semana
    SET v_dia = CASE DAYOFWEEK(p_fecha)
        WHEN 2 THEN 'LUNES'
        WHEN 3 THEN 'MARTES'
        WHEN 4 THEN 'MIERCOLES'
        WHEN 5 THEN 'JUEVES'
        WHEN 6 THEN 'VIERNES'
        ELSE NULL
    END;
    
    IF v_dia IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La fecha no corresponde a un día lectivo (Lunes-Viernes)';
    END IF;
    
    START TRANSACTION;
    
    /* =====================================================
       1. CONTROL ASISTENCIA (SOLO LABORATORIO)
       ===================================================== */
    INSERT IGNORE INTO control_asistencia (
        id_horario,
        fecha,
        hora_apertura,
        hora_cierre,
        estado
    )
    SELECT
        h.id_horario,
        p_fecha,
        h.hora_inicio,
        h.hora_fin,
        'ABIERTO'
    FROM horarios h
    INNER JOIN grupos_curso g ON g.grupo_id = h.grupo_id
    WHERE h.grupo_id = p_grupo_id
      AND g.tipo_clase = 'LABORATORIO'  -- Corregido: tipo_clase en lugar de tipo_grupo
      AND h.dia_semana = v_dia
      AND h.estado = 'ACTIVO';
    
    /* =====================================================
       2. ASISTENCIA DOCENTE (LAB)
       ===================================================== */
    INSERT IGNORE INTO asistencias_docente (
        id_horario,
        id_docente,
        fecha,
        hora_registro,
        ip_registro,
        tipo_ubicacion,
        presente
    )
    SELECT
        h.id_horario,
        h.id_docente,
        p_fecha,
        CURRENT_TIME(),
        p_ip,
        p_tipo_ubicacion,
        1
    FROM horarios h
    INNER JOIN grupos_curso g ON g.grupo_id = h.grupo_id
    WHERE h.grupo_id = p_grupo_id
      AND g.tipo_clase = 'LABORATORIO'  -- Corregido: tipo_clase
      AND h.dia_semana = v_dia
      AND h.estado = 'ACTIVO'
      AND h.id_docente IS NOT NULL;
    
    /* =====================================================
       3. ASISTENCIA ESTUDIANTES (LAB)
       ===================================================== */
    INSERT IGNORE INTO asistencias_estudiante (
        id_matricula,
        id_horario,
        fecha,
        estado_asistencia,
        registrado_por
    )
    SELECT
        m.id_matricula,
        h.id_horario,
        p_fecha,
        'PRESENTE',
        h.id_docente
    FROM matriculas m
    INNER JOIN horarios h ON h.grupo_id = m.grupo_id
    INNER JOIN grupos_curso g ON g.grupo_id = h.grupo_id
    WHERE m.grupo_id = p_grupo_id
      AND g.tipo_clase = 'LABORATORIO'  -- Corregido: tipo_clase
      AND m.estado_matricula = 'ACTIVO'
      AND h.dia_semana = v_dia
      AND h.estado = 'ACTIVO'
      AND h.id_docente IS NOT NULL;
    
    COMMIT;
    
    -- Mensaje de confirmación
    SELECT 
        CONCAT('Asistencia generada exitosamente para el grupo ', p_grupo_id, 
               ' en fecha ', p_fecha) AS mensaje,
        (SELECT COUNT(*) FROM control_asistencia 
         WHERE fecha = p_fecha 
         AND id_horario IN (SELECT id_horario FROM horarios WHERE grupo_id = p_grupo_id)) AS controles_creados,
        (SELECT COUNT(*) FROM asistencias_docente 
         WHERE fecha = p_fecha 
         AND id_horario IN (SELECT id_horario FROM horarios WHERE grupo_id = p_grupo_id)) AS docentes_registrados,
        (SELECT COUNT(*) FROM asistencias_estudiante 
         WHERE fecha = p_fecha 
         AND id_horario IN (SELECT id_horario FROM horarios WHERE grupo_id = p_grupo_id)) AS estudiantes_registrados;
    
END$$

DELIMITER ;
