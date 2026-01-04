-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: lumina_bd
-- ------------------------------------------------------
-- Server version	8.0.43

CREATE TABLE `asistencias_docente` (
  `id_asistencia_docente` int NOT NULL AUTO_INCREMENT,
  `id_horario` int NOT NULL,
  `id_docente` int NOT NULL,
  `fecha` date NOT NULL,
  `hora_registro` time NOT NULL,
  `ip_registro` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_ubicacion` enum('PRESENCIAL','VIRTUAL') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `presente` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id_asistencia_docente`),
  UNIQUE KEY `uk_docente_horario_fecha` (`id_docente`,`id_horario`,`fecha`),
  KEY `id_horario` (`id_horario`),
  KEY `idx_fecha_horario` (`fecha`,`id_horario`),
  CONSTRAINT `asistencias_docente_ibfk_1` FOREIGN KEY (`id_horario`) REFERENCES `horarios` (`id_horario`) ON DELETE CASCADE,
  CONSTRAINT `asistencias_docente_ibfk_2` FOREIGN KEY (`id_docente`) REFERENCES `docentes` (`id_docente`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `asistencias_estudiante` (
  `id_asistencia` int NOT NULL AUTO_INCREMENT,
  `id_matricula` int NOT NULL,
  `id_horario` int NOT NULL,
  `fecha` date NOT NULL,
  `estado_asistencia` enum('PRESENTE','FALTA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `registrado_por` int NOT NULL,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_asistencia`),
  UNIQUE KEY `uk_matricula_horario_fecha` (`id_matricula`,`id_horario`,`fecha`),
  KEY `id_horario` (`id_horario`),
  KEY `registrado_por` (`registrado_por`),
  KEY `idx_fecha_horario` (`fecha`,`id_horario`),
  CONSTRAINT `asistencias_estudiante_ibfk_1` FOREIGN KEY (`id_matricula`) REFERENCES `matriculas` (`id_matricula`) ON DELETE CASCADE,
  CONSTRAINT `asistencias_estudiante_ibfk_2` FOREIGN KEY (`id_horario`) REFERENCES `horarios` (`id_horario`) ON DELETE CASCADE,
  CONSTRAINT `asistencias_estudiante_ibfk_3` FOREIGN KEY (`registrado_por`) REFERENCES `docentes` (`id_docente`)
) ENGINE=InnoDB AUTO_INCREMENT=118 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ciclos_academicos` (
  `id_ciclo` int NOT NULL AUTO_INCREMENT,
  `nombre_ciclo` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `anio` int NOT NULL,
  `semestre` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `fecha_inicio_clases` date NOT NULL,
  `fecha_fin_clases` date NOT NULL,
  `fecha_inicio_examenes` date DEFAULT NULL,
  `fecha_fin_examenes` date DEFAULT NULL,
  `estado` enum('ACTIVO','INACTIVO','PLANIFICADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PLANIFICADO',
  `fecha_creacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_ciclo`),
  UNIQUE KEY `nombre_ciclo` (`nombre_ciclo`),
  UNIQUE KEY `uk_ciclo` (`anio`,`semestre`),
  KEY `idx_activo` (`estado`),
  KEY `idx_fechas` (`fecha_inicio`,`fecha_fin`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `control_asistencia` (
  `id_control` int NOT NULL AUTO_INCREMENT,
  `id_horario` int NOT NULL,
  `fecha` date NOT NULL,
  `hora_apertura` time NOT NULL,
  `hora_cierre` time NOT NULL,
  `estado` enum('ABIERTO','CERRADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ABIERTO',
  PRIMARY KEY (`id_control`),
  UNIQUE KEY `uk_horario_fecha` (`id_horario`,`fecha`),
  KEY `idx_fecha_estado` (`fecha`,`estado`),
  CONSTRAINT `control_asistencia_ibfk_1` FOREIGN KEY (`id_horario`) REFERENCES `horarios` (`id_horario`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cursos` (
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_curso` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tiene_laboratorio` tinyint(1) DEFAULT '0',
  `numero_grupos_teoria` int DEFAULT '2',
  `numero_grupos_laboratorio` int DEFAULT '0',
  `estado` enum('ACTIVO','INACTIVO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVO',
  PRIMARY KEY (`codigo_curso`),
  KEY `idx_nombre` (`nombre_curso`),
  KEY `idx_estado` (`estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `docente_horario` (
  `id_docente` int NOT NULL,
  `id_horario` int NOT NULL,
  PRIMARY KEY (`id_docente`,`id_horario`),
  KEY `id_horario` (`id_horario`),
  CONSTRAINT `docente_horario_ibfk_1` FOREIGN KEY (`id_docente`) REFERENCES `docentes` (`id_docente`),
  CONSTRAINT `docente_horario_ibfk_2` FOREIGN KEY (`id_horario`) REFERENCES `horarios` (`id_horario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `docentes` (
  `id_docente` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `apellidos_nombres` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `departamento` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `es_responsable_teoria` tinyint(1) DEFAULT '0',
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_docente`),
  UNIQUE KEY `id_usuario` (`id_usuario`),
  KEY `idx_departamento` (`departamento`),
  CONSTRAINT `docentes_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `estudiante_horario` (
  `cui_est` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_horario` int NOT NULL,
  PRIMARY KEY (`cui_est`,`id_horario`),
  KEY `id_horario` (`id_horario`),
  CONSTRAINT `estudiante_horario_ibfk_1` FOREIGN KEY (`cui_est`) REFERENCES `estudiantes` (`cui`),
  CONSTRAINT `estudiante_horario_ibfk_2` FOREIGN KEY (`id_horario`) REFERENCES `horarios` (`id_horario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `estudiantes` (
  `cui` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_usuario` int NOT NULL,
  `apellidos_nombres` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `numero_matricula` int NOT NULL,
  `estado_estudiante` enum('VIGENTE','RETIRADO','EGRESADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'VIGENTE',
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cui`),
  UNIQUE KEY `id_usuario` (`id_usuario`),
  KEY `idx_estado` (`estado_estudiante`),
  CONSTRAINT `estudiantes_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `examenes_pdf` (
  `id_examen` int NOT NULL AUTO_INCREMENT,
  `grupo_id` int NOT NULL,
  `tipo_eval_id` int NOT NULL,
  `tipo_nota` enum('ALTA','BAJA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ruta_archivo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_subida` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `subido_por` int NOT NULL,
  PRIMARY KEY (`id_examen`),
  UNIQUE KEY `uk_grupo_eval_tipo` (`grupo_id`,`tipo_eval_id`,`tipo_nota`),
  KEY `fk_examenes_tipo_eval` (`tipo_eval_id`),
  KEY `fk_examenes_docente` (`subido_por`),
  KEY `idx_grupo_tipo` (`grupo_id`,`tipo_eval_id`),
  CONSTRAINT `fk_examenes_docente` FOREIGN KEY (`subido_por`) REFERENCES `docentes` (`id_docente`),
  CONSTRAINT `fk_examenes_grupo` FOREIGN KEY (`grupo_id`) REFERENCES `grupos_curso` (`grupo_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_examenes_tipo_eval` FOREIGN KEY (`tipo_eval_id`) REFERENCES `tipos_evaluacion` (`tipo_eval_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `grupos_curso` (
  `grupo_id` int NOT NULL AUTO_INCREMENT,
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_ciclo` int NOT NULL,
  `letra_grupo` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_clase` enum('TEORIA','LABORATORIO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `capacidad_maxima` int NOT NULL,
  `estado` enum('ACTIVO','CERRADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVO',
  PRIMARY KEY (`grupo_id`),
  UNIQUE KEY `uk_grupo_curso` (`codigo_curso`,`id_ciclo`,`letra_grupo`,`tipo_clase`),
  KEY `id_ciclo` (`id_ciclo`),
  KEY `idx_curso_ciclo` (`codigo_curso`,`id_ciclo`),
  KEY `idx_tipo_clase` (`tipo_clase`),
  CONSTRAINT `grupos_curso_ibfk_1` FOREIGN KEY (`codigo_curso`) REFERENCES `cursos` (`codigo_curso`) ON DELETE CASCADE,
  CONSTRAINT `grupos_curso_ibfk_2` FOREIGN KEY (`id_ciclo`) REFERENCES `ciclos_academicos` (`id_ciclo`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=143 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `horarios` (
  `id_horario` int NOT NULL AUTO_INCREMENT,
  `grupo_id` int NOT NULL,
  `numero_salon` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dia_semana` enum('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL,
  `id_docente` int DEFAULT NULL,
  `estado` enum('ACTIVO','SUSPENDIDO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_horario`),
  KEY `grupo_id` (`grupo_id`),
  KEY `idx_salon_dia` (`numero_salon`,`dia_semana`),
  KEY `idx_docente_dia` (`id_docente`,`dia_semana`),
  KEY `idx_dia_hora` (`dia_semana`,`hora_inicio`),
  CONSTRAINT `horarios_ibfk_1` FOREIGN KEY (`grupo_id`) REFERENCES `grupos_curso` (`grupo_id`) ON DELETE CASCADE,
  CONSTRAINT `horarios_ibfk_2` FOREIGN KEY (`numero_salon`) REFERENCES `salones` (`numero_salon`),
  CONSTRAINT `horarios_ibfk_3` FOREIGN KEY (`id_docente`) REFERENCES `docentes` (`id_docente`)
) ENGINE=InnoDB AUTO_INCREMENT=118 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `log_actividades` (
  `id_log` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `accion` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tabla_afectada` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `ip_origen` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_accion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_log`),
  KEY `idx_usuario_fecha` (`id_usuario`,`fecha_accion`),
  KEY `idx_tabla` (`tabla_afectada`),
  KEY `idx_fecha` (`fecha_accion`),
  CONSTRAINT `log_actividades_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `matriculas` (
  `id_matricula` int NOT NULL AUTO_INCREMENT,
  `cui` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `grupo_id` int NOT NULL,
  `numero_matricula` int DEFAULT NULL,
  `prioridad_matricula` tinyint(1) DEFAULT '0',
  `estado_matricula` enum('ACTIVO','RETIRADO','ABANDONADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVO',
  `fecha_matricula` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_matricula`),
  UNIQUE KEY `uk_estudiante_grupo` (`cui`,`grupo_id`),
  KEY `idx_estado` (`estado_matricula`),
  KEY `idx_grupo` (`grupo_id`),
  CONSTRAINT `matriculas_ibfk_1` FOREIGN KEY (`cui`) REFERENCES `estudiantes` (`cui`) ON DELETE CASCADE,
  CONSTRAINT `matriculas_ibfk_2` FOREIGN KEY (`grupo_id`) REFERENCES `grupos_curso` (`grupo_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=153 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notas` (
  `id_nota` int NOT NULL AUTO_INCREMENT,
  `id_matricula` int NOT NULL,
  `tipo_eval_id` int NOT NULL,
  `calificacion` decimal(5,2) DEFAULT NULL,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `docente_registro_id` int DEFAULT NULL,
  PRIMARY KEY (`id_nota`),
  UNIQUE KEY `uk_matricula_evaluacion` (`id_matricula`,`tipo_eval_id`),
  KEY `docente_registro_id` (`docente_registro_id`),
  KEY `idx_matricula` (`id_matricula`),
  KEY `idx_tipo_eval` (`tipo_eval_id`),
  CONSTRAINT `notas_ibfk_1` FOREIGN KEY (`id_matricula`) REFERENCES `matriculas` (`id_matricula`) ON DELETE CASCADE,
  CONSTRAINT `notas_ibfk_2` FOREIGN KEY (`tipo_eval_id`) REFERENCES `tipos_evaluacion` (`tipo_eval_id`),
  CONSTRAINT `notas_ibfk_3` FOREIGN KEY (`docente_registro_id`) REFERENCES `docentes` (`id_docente`),
  CONSTRAINT `notas_chk_1` CHECK (((`calificacion` >= 0) and (`calificacion` <= 20)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `periodos_ingreso_notas` (
  `id_periodo` int NOT NULL AUTO_INCREMENT,
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_eval_id` int DEFAULT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime NOT NULL,
  `estado` enum('ACTIVO','FINALIZADO','PROGRAMADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PROGRAMADO',
  `creado_por` int NOT NULL,
  `fecha_creacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_periodo`),
  KEY `creado_por` (`creado_por`),
  KEY `idx_estado` (`estado`),
  KEY `idx_fechas` (`fecha_inicio`,`fecha_fin`),
  KEY `idx_curso` (`codigo_curso`),
  KEY `idx_tipo_eval` (`tipo_eval_id`),
  CONSTRAINT `periodos_ingreso_notas_ibfk_1` FOREIGN KEY (`codigo_curso`) REFERENCES `cursos` (`codigo_curso`) ON DELETE CASCADE,
  CONSTRAINT `periodos_ingreso_notas_ibfk_2` FOREIGN KEY (`tipo_eval_id`) REFERENCES `tipos_evaluacion` (`tipo_eval_id`) ON DELETE CASCADE,
  CONSTRAINT `periodos_ingreso_notas_ibfk_3` FOREIGN KEY (`creado_por`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `periodos_matricula_laboratorio` (
  `id_periodo` int NOT NULL AUTO_INCREMENT,
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime NOT NULL,
  `cupos_disponibles` int NOT NULL,
  `estado` enum('ACTIVO','FINALIZADO','PROGRAMADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PROGRAMADO',
  `fecha_creacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_periodo`),
  KEY `codigo_curso` (`codigo_curso`),
  KEY `idx_estado` (`estado`),
  KEY `idx_fechas` (`fecha_inicio`,`fecha_fin`),
  CONSTRAINT `periodos_matricula_laboratorio_ibfk_1` FOREIGN KEY (`codigo_curso`) REFERENCES `cursos` (`codigo_curso`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `porcentajes_evaluacion` (
  `id_porcentaje` int NOT NULL AUTO_INCREMENT,
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_ciclo` int NOT NULL,
  `tipo_eval_id` int NOT NULL,
  `porcentaje` decimal(5,2) NOT NULL,
  PRIMARY KEY (`id_porcentaje`),
  UNIQUE KEY `uk_curso_ciclo_evaluacion` (`codigo_curso`,`id_ciclo`,`tipo_eval_id`),
  KEY `id_ciclo` (`id_ciclo`),
  KEY `tipo_eval_id` (`tipo_eval_id`),
  KEY `idx_curso_ciclo` (`codigo_curso`,`id_ciclo`),
  CONSTRAINT `porcentajes_evaluacion_ibfk_1` FOREIGN KEY (`codigo_curso`) REFERENCES `cursos` (`codigo_curso`) ON DELETE CASCADE,
  CONSTRAINT `porcentajes_evaluacion_ibfk_2` FOREIGN KEY (`id_ciclo`) REFERENCES `ciclos_academicos` (`id_ciclo`) ON DELETE CASCADE,
  CONSTRAINT `porcentajes_evaluacion_ibfk_3` FOREIGN KEY (`tipo_eval_id`) REFERENCES `tipos_evaluacion` (`tipo_eval_id`)
) ENGINE=InnoDB AUTO_INCREMENT=151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reportes_generados` (
  `id_reporte` int NOT NULL AUTO_INCREMENT,
  `tipo_reporte` enum('ASISTENCIA_NOTAS','RENDIMIENTO','ACADEMICO_COMPLETO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `generado_por` int NOT NULL,
  `tipo_usuario` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_generacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `ruta_archivo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_reporte`),
  KEY `generado_por` (`generado_por`),
  KEY `codigo_curso` (`codigo_curso`),
  KEY `idx_fecha` (`fecha_generacion`),
  KEY `idx_tipo` (`tipo_reporte`),
  CONSTRAINT `reportes_generados_ibfk_1` FOREIGN KEY (`generado_por`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `reportes_generados_ibfk_2` FOREIGN KEY (`codigo_curso`) REFERENCES `cursos` (`codigo_curso`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reservas_salon` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `numero_salon` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_docente` int NOT NULL,
  `dia_semana` enum('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL,
  `fecha_reserva` date NOT NULL,
  `motivo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `estado_reserva` enum('PENDIENTE','CONFIRMADA','CANCELADA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDIENTE',
  `fecha_creacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_reserva`),
  KEY `idx_salon_fecha` (`numero_salon`,`fecha_reserva`),
  KEY `idx_docente_fecha` (`id_docente`,`fecha_reserva`),
  KEY `idx_estado` (`estado_reserva`),
  CONSTRAINT `reservas_salon_ibfk_1` FOREIGN KEY (`numero_salon`) REFERENCES `salones` (`numero_salon`),
  CONSTRAINT `reservas_salon_ibfk_2` FOREIGN KEY (`id_docente`) REFERENCES `docentes` (`id_docente`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `salones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salones` (
  `numero_salon` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_aula_id` int NOT NULL,
  `capacidad` int NOT NULL,
  `estado` enum('DISPONIBLE','OCUPADA','MANTENIMIENTO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'DISPONIBLE',
  PRIMARY KEY (`numero_salon`),
  KEY `idx_tipo` (`tipo_aula_id`),
  KEY `idx_estado` (`estado`),
  CONSTRAINT `salones_ibfk_1` FOREIGN KEY (`tipo_aula_id`) REFERENCES `tipos_aula` (`tipo_aula_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sesiones` (
  `id_sesion` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `token_sesion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_sesion` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_inicio` datetime DEFAULT CURRENT_TIMESTAMP,
  `fecha_expiracion` datetime NOT NULL,
  `activo` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id_sesion`),
  UNIQUE KEY `token_sesion` (`token_sesion`),
  KEY `idx_token` (`token_sesion`),
  KEY `idx_usuario_activo` (`id_usuario`,`activo`),
  KEY `idx_expiracion` (`fecha_expiracion`),
  CONSTRAINT `sesiones_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `silabos` (
  `id_silabo` int NOT NULL AUTO_INCREMENT,
  `codigo_curso` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_ciclo` int NOT NULL,
  `grupo_teoria` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ruta_archivo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_docente` int NOT NULL,
  `fecha_subida` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('PENDIENTE','APROBADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id_silabo`),
  UNIQUE KEY `uk_curso_ciclo_grupo` (`codigo_curso`,`id_ciclo`,`grupo_teoria`),
  KEY `id_ciclo` (`id_ciclo`),
  KEY `idx_docente` (`id_docente`),
  CONSTRAINT `silabos_ibfk_1` FOREIGN KEY (`codigo_curso`) REFERENCES `cursos` (`codigo_curso`) ON DELETE CASCADE,
  CONSTRAINT `silabos_ibfk_2` FOREIGN KEY (`id_ciclo`) REFERENCES `ciclos_academicos` (`id_ciclo`) ON DELETE CASCADE,
  CONSTRAINT `silabos_ibfk_3` FOREIGN KEY (`id_docente`) REFERENCES `docentes` (`id_docente`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `temas` (
  `id_tema` int NOT NULL AUTO_INCREMENT,
  `unidad_id` int NOT NULL,
  `numero_tema` int NOT NULL,
  `nombre_tema` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `duracion_estimada` int DEFAULT NULL,
  `estado` enum('PENDIENTE','EN_CURSO','COMPLETADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDIENTE',
  `fecha_completado` date DEFAULT NULL,
  PRIMARY KEY (`id_tema`),
  KEY `idx_unidad_numero` (`unidad_id`,`numero_tema`),
  CONSTRAINT `temas_ibfk_1` FOREIGN KEY (`unidad_id`) REFERENCES `unidades` (`unidad_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tipos_aula` (
  `tipo_aula_id` int NOT NULL AUTO_INCREMENT,
  `nombre_tipo` enum('AULA','LABORATORIO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`tipo_aula_id`),
  KEY `idx_nombre` (`nombre_tipo`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tipos_evaluacion` (
  `tipo_eval_id` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` enum('PARCIAL','CONTINUA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`tipo_eval_id`),
  UNIQUE KEY `codigo` (`codigo`),
  KEY `idx_codigo` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tipos_usuario` (
  `tipo_id` int NOT NULL AUTO_INCREMENT,
  `nombre_tipo` enum('ESTUDIANTE','DOCENTE','SECRETARIA','ADMINISTRADOR') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `permisos` json DEFAULT NULL,
  PRIMARY KEY (`tipo_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `unidades` (
  `unidad_id` int NOT NULL AUTO_INCREMENT,
  `id_silabo` int NOT NULL,
  `numero_unidad` int NOT NULL,
  `nombre_unidad` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`unidad_id`),
  KEY `idx_silabo_unidad` (`id_silabo`,`numero_unidad`),
  CONSTRAINT `unidades_ibfk_1` FOREIGN KEY (`id_silabo`) REFERENCES `silabos` (`id_silabo`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `correo_institucional` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `salt` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_id` int NOT NULL,
  `estado_cuenta` enum('ACTIVO','BLOQUEADO','ELIMINADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVO',
  `primer_acceso` tinyint(1) DEFAULT '1',
  `fecha_creacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_ultimo_acceso` datetime DEFAULT NULL,
  `ip_ultimo_acceso` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `correo_institucional` (`correo_institucional`),
  KEY `idx_correo` (`correo_institucional`),
  KEY `idx_tipo` (`tipo_id`),
  KEY `idx_estado` (`estado_cuenta`),
  CONSTRAINT `usuarios_ibfk_1` FOREIGN KEY (`tipo_id`) REFERENCES `tipos_usuario` (`tipo_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1702215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- Dumping routines for database 'lumina_bd'
--
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `actualizar_estados_periodos_notas`()
BEGIN
    -- Actualizar a ACTIVO los períodos cuya fecha de inicio ya pasó
    UPDATE periodos_ingreso_notas
    SET estado = 'ACTIVO'
    WHERE estado = 'PROGRAMADO'
      AND fecha_inicio <= NOW()
      AND fecha_fin > NOW();
    
    -- Actualizar a FINALIZADO los períodos cuya fecha de fin ya pasó
    UPDATE periodos_ingreso_notas
    SET estado = 'FINALIZADO'
    WHERE estado IN ('ACTIVO', 'PROGRAMADO')
      AND fecha_fin <= NOW();
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `notas_estudiante`(IN p_cui VARCHAR(20))
BEGIN
    /*
      Devuelve:
       - nombre_curso
       - codigo_curso
       - docente_del_curso (puede ser varios, concatenados)
       - tipo_evaluacion (nombre del tipo)
       - nota (calificacion)
       - porcentaje (porcentaje del tipo de evaluación para ese curso y ciclo)
       - fecha_registro (de la nota)
    */

    SELECT
        c.nombre_curso AS nombre_curso,
        c.codigo_curso AS codigo_curso,
        -- Si hay varios horarios/docentes para ese grupo los concatenamos (evita duplicados)
        TRIM(BOTH '; ' FROM GROUP_CONCAT(DISTINCT d.apellidos_nombres SEPARATOR '; ')) AS docente_del_curso,
        te.nombre AS tipo_evaluacion,
        n.calificacion AS nota,
        COALESCE(pe.porcentaje, 0) AS porcentaje,
        n.fecha_registro AS fecha_registro
    FROM notas n
    INNER JOIN matriculas m ON n.id_matricula = m.id_matricula
    INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
    INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
    LEFT JOIN porcentajes_evaluacion pe 
        ON pe.codigo_curso = c.codigo_curso
       AND pe.id_ciclo = g.id_ciclo
       AND pe.tipo_eval_id = n.tipo_eval_id
    LEFT JOIN tipos_evaluacion te ON n.tipo_eval_id = te.tipo_eval_id
    -- Unimos horarios y docentes para obtener el/los docentes que imparten ese grupo
    LEFT JOIN horarios h ON h.grupo_id = g.grupo_id
    LEFT JOIN docentes d ON h.id_docente = d.id_docente
    WHERE m.cui = p_cui
    GROUP BY
        n.id_nota,       -- agrupar por nota (clave primaria)
        c.nombre_curso,
        c.codigo_curso,
        te.nombre,
        n.calificacion,
        pe.porcentaje,
        n.fecha_registro
    ORDER BY n.fecha_registro DESC;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_actualizar_contrasena`(
    IN p_id_usuario INT,
    IN p_password_hash VARCHAR(255),
    IN p_salt VARCHAR(100)   -- puedes enviar NULL si no usas salt separado
)
BEGIN
    UPDATE usuarios
    SET password_hash = p_password_hash,
        salt = IFNULL(p_salt, salt),
        primer_acceso = FALSE,
        fecha_ultimo_acceso = COALESCE(fecha_ultimo_acceso, NOW())
    WHERE id_usuario = p_id_usuario;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_actualizar_perfil_estudiante`(
  IN p_id_usuario INT,
  IN p_telefono VARCHAR(50),
  IN p_direccion TEXT
)
BEGIN
  UPDATE estudiantes e
  JOIN usuarios u ON e.id_usuario = u.id_usuario
  SET e.telefono = p_telefono,
      e.direccion = p_direccion
  WHERE u.id_usuario = p_id_usuario;
  SELECT ROW_COUNT() AS filas_afectadas;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_cambiar_contrasena`(
  IN p_id_usuario INT,
  IN p_newhash VARCHAR(255)
)
BEGIN
  UPDATE usuarios
  SET password_hash = p_newhash,
      primer_acceso = 0
  WHERE id_usuario = p_id_usuario;
  SELECT ROW_COUNT() AS filas_afectadas;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_cursos_y_grupos_por_docente`(
    IN p_correo VARCHAR(100)
)
BEGIN
    SELECT DISTINCT 
        c.codigo_curso,
        c.nombre_curso,
        g.grupo_id,
        g.letra_grupo,
        g.tipo_clase
    FROM usuarios u
    INNER JOIN docentes d       ON d.id_usuario = u.id_usuario
    INNER JOIN horarios h       ON h.id_docente = d.id_docente
    INNER JOIN grupos_curso g   ON g.grupo_id = h.grupo_id
    INNER JOIN cursos c         ON c.codigo_curso = g.codigo_curso
    WHERE u.correo_institucional = p_correo
      AND h.estado = 'ACTIVO'
      AND g.estado = 'ACTIVO'
      AND c.estado = 'ACTIVO'
    ORDER BY c.codigo_curso, g.letra_grupo;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_horarios_docente`(IN p_id_usuario INT)
BEGIN
  DECLARE v_id_docente INT;

  -- obtener id_docente (puede ser NULL si no existe)
  SET v_id_docente = (SELECT id_docente FROM docentes WHERE id_usuario = p_id_usuario LIMIT 1);

  IF v_id_docente IS NULL THEN
    SELECT CONCAT('Docente no encontrado para id_usuario = ', COALESCE(p_id_usuario,'NULL')) AS mensaje;
  ELSE
    SELECT
      h.id_horario,
      g.grupo_id,
      g.codigo_curso,
      cu.nombre_curso,
      g.letra_grupo,
      g.tipo_clase,
      ca.nombre_ciclo AS ciclo,
      ca.anio,
      ca.semestre,
      h.dia_semana,
      h.hora_inicio,
      h.hora_fin,
      h.numero_salon,
      IFNULL(s.capacidad,'-') AS capacidad_salon,
      d.apellidos_nombres AS docente_nombre,
      h.estado
    FROM horarios h
    JOIN grupos_curso g ON h.grupo_id = g.grupo_id
    JOIN cursos cu ON g.codigo_curso = cu.codigo_curso
    LEFT JOIN salones s ON h.numero_salon = s.numero_salon
    JOIN docentes d ON h.id_docente = d.id_docente
    JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
    WHERE h.id_docente = v_id_docente
    ORDER BY FIELD(h.dia_semana,'LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'), h.hora_inicio;
  END IF;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_horarios_estudiante`(IN p_id_usuario INT)
BEGIN
  DECLARE v_cui VARCHAR(20);

  -- obtener cui del estudiante (puede ser NULL si no existe)
  SET v_cui = (SELECT cui FROM estudiantes WHERE id_usuario = p_id_usuario LIMIT 1);

  IF v_cui IS NULL THEN
    SELECT CONCAT('Estudiante no encontrado para id_usuario = ', COALESCE(p_id_usuario,'NULL')) AS mensaje;
  ELSE
    SELECT
      h.id_horario,
      m.id_matricula,
      g.grupo_id,
      g.codigo_curso,
      cu.nombre_curso,
      g.letra_grupo,
      g.tipo_clase,
      ca.nombre_ciclo AS ciclo,
      ca.anio,
      ca.semestre,
      h.dia_semana,
      h.hora_inicio,
      h.hora_fin,
      h.numero_salon,
      IFNULL(d.apellidos_nombres,'-') AS docente_nombre,
      h.estado
    FROM matriculas m
    JOIN grupos_curso g ON m.grupo_id = g.grupo_id
    JOIN horarios h ON h.grupo_id = g.grupo_id
    JOIN cursos cu ON g.codigo_curso = cu.codigo_curso
    LEFT JOIN docentes d ON h.id_docente = d.id_docente
    JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
    WHERE m.cui = v_cui
      AND m.estado_matricula = 'ACTIVO'
    ORDER BY FIELD(h.dia_semana,'LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'), h.hora_inicio;
  END IF;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_horarios_estudiantes`(IN p_id_usuario INT)
BEGIN
  DECLARE v_cui VARCHAR(20);

  -- obtener cui del estudiante (puede ser NULL si no existe)
  SET v_cui = (SELECT cui FROM estudiantes WHERE id_usuario = p_id_usuario LIMIT 1);

  IF v_cui IS NULL THEN
    SELECT CONCAT('Estudiante no encontrado para id_usuario = ', COALESCE(p_id_usuario,'NULL')) AS mensaje;
  ELSE
    SELECT
      h.id_horario,
      m.id_matricula,
      g.grupo_id,
      g.codigo_curso,
      cu.nombre_curso,
      g.letra_grupo,
      g.tipo_clase,
      ca.nombre_ciclo AS ciclo,
      ca.anio,
      ca.semestre,
      h.dia_semana,
      h.hora_inicio,
      h.hora_fin,
      h.numero_salon,
      IFNULL(d.apellidos_nombres,'-') AS docente_nombre,
      h.estado
    FROM matriculas m
    JOIN grupos_curso g ON m.grupo_id = g.grupo_id
    JOIN horarios h ON h.grupo_id = g.grupo_id
    JOIN cursos cu ON g.codigo_curso = cu.codigo_curso
    LEFT JOIN docentes d ON h.id_docente = d.id_docente
    JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
    WHERE m.cui = v_cui
      AND m.estado_matricula = 'ACTIVO'
    ORDER BY FIELD(h.dia_semana,'LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'), h.hora_inicio;
  END IF;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_obtener_contenido_estudiante`(IN p_cui VARCHAR(20))
BEGIN
  /*
    Devuelve JSON array con cursos del estudiante (matriculas),
    cada curso tiene: id(nombre codigo), nombre, descripcion, progreso, semanas (unidades) -> contenidos (temas).
  */

  SELECT COALESCE(
    JSON_ARRAYAGG(
      JSON_OBJECT(
        'id', c.codigo_curso,
        'nombre', c.nombre_curso,
        'descripcion', COALESCE(c.nombre_curso, ''),
        'progreso',
          COALESCE(
            ( -- progreso del curso: % de temas completados en el silabo correspondiente
              SELECT IFNULL(ROUND(100.0 *
                  (SELECT COUNT(*) FROM temas tt
                   WHERE tt.unidad_id IN (
                     SELECT u2.unidad_id FROM unidades u2 WHERE u2.id_silabo = s2.id_silabo
                   ) AND tt.estado = 'COMPLETADO'
                  ) /
                  GREATEST(
                    (SELECT COUNT(*) FROM temas tt2
                     WHERE tt2.unidad_id IN (
                       SELECT u3.unidad_id FROM unidades u3 WHERE u3.id_silabo = s2.id_silabo
                     )
                    ),1)
              ),0)
              FROM silabos s2
              WHERE s2.codigo_curso = c.codigo_curso
                AND s2.id_ciclo = g.id_ciclo
                AND s2.grupo_teoria = g.letra_grupo
              LIMIT 1
            ), 0
          ),
        'semanas',
          COALESCE(
            ( -- array de unidades (semanas)
              SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                  'numero', u2.numero_unidad,
                  'titulo', u2.nombre_unidad,
                  'progreso',
                    ( -- progreso unidad: % de temas completados en esta unidad
                      SELECT IFNULL(ROUND(100.0 * SUM(CASE WHEN t2.estado='COMPLETADO' THEN 1 ELSE 0 END) / GREATEST(COUNT(t2.id_tema),1)),0)
                      FROM temas t2 WHERE t2.unidad_id = u2.unidad_id
                    ),
                  'contenidos',
                    COALESCE(
                      (SELECT JSON_ARRAYAGG(
                          JSON_OBJECT(
                            'titulo', t3.nombre_tema,
                            'descripcion', COALESCE(t3.duracion_estimada,''),
                            'estado', LOWER(t3.estado),
                            'id', t3.id_tema
                          )
                        ) FROM temas t3 WHERE t3.unidad_id = u2.unidad_id
                      ), JSON_ARRAY()
                    )
                )
              )
              FROM unidades u2
              WHERE u2.id_silabo = (
                SELECT s3.id_silabo FROM silabos s3
                WHERE s3.codigo_curso = c.codigo_curso
                  AND s3.id_ciclo = g.id_ciclo
                  AND s3.grupo_teoria = g.letra_grupo
                LIMIT 1
              )
            ), JSON_ARRAY()
          )
      )
    ), JSON_ARRAY()
  ) AS resultado
  FROM matriculas m
  INNER JOIN grupos_curso g ON g.grupo_id = m.grupo_id
  INNER JOIN cursos c ON c.codigo_curso = g.codigo_curso
  WHERE m.cui = p_cui;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_obtener_datos_estudiante`(IN p_id_usuario INT)
BEGIN
    /*
    Devuelve:
      id_usuario, email, codigo (CUI), nombreCompleto, numero_matricula,
      fecha_creacion (usuario), cursosActivos, promedioGeneral, asistencia,
      creditosAprobados, carrera, facultad, semestre, iniciales
    NOTA: reemplaza subconsultas/valores por los cálculos reales según tus datos.
    */
    SELECT
        u.id_usuario                                AS idUsuario,
        u.correo_institucional                       AS email,
        s.cui                                       AS codigo,
        s.apellidos_nombres                         AS nombreCompleto,
        s.numero_matricula                          AS numeroMatricula,
        u.fecha_creacion                            AS fechaCreacion,

        -- ejemplo simple: número de matrículas activas del estudiante (puedes adaptar)
        (SELECT COUNT(*) FROM matriculas m WHERE m.cui = s.cui AND m.estado_matricula = 'ACTIVO') AS cursosActivos,

        -- placeholders (NULL) para campos que requieren lógica adicional
        NULL                                        AS promedioGeneral,
        NULL                                        AS asistencia,
        NULL                                        AS creditosAprobados,

        -- Si no tienes tablas de carreras/facultades relacionadas, puedes devolver valores por defecto
        'Ciencia de la Computación'                 AS carrera,
        'Ingeniería de Producción y Servicios'      AS facultad,

        ''                                          AS semestre,
        -- generar iniciales desde apellidos_nombres (simplemente primera letras)
        CONCAT(UPPER(LEFT(SUBSTRING_INDEX(s.apellidos_nombres,' ',1),1)),
               UPPER(LEFT(SUBSTRING_INDEX(SUBSTRING_INDEX(s.apellidos_nombres,' ',-1),' ',1),1))) AS iniciales

    FROM usuarios u
    JOIN estudiantes s ON s.id_usuario = u.id_usuario
    WHERE u.id_usuario = p_id_usuario
    LIMIT 1;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_obtener_silabos_por_curso`(
    IN p_codigo_curso VARCHAR(20)
)
BEGIN
    DECLARE v_count INT DEFAULT 0;

    -- contar si hay sílabos para el curso
    SELECT COUNT(*) INTO v_count
    FROM silabos
    WHERE codigo_curso = p_codigo_curso;

    IF v_count > 0 THEN
        -- devolver todas las columnas de las filas encontradas
        SELECT
            id_silabo,
            codigo_curso,
            id_ciclo,
            grupo_teoria,
            ruta_archivo,
            id_docente,
            fecha_subida,
            estado
        FROM silabos
        WHERE codigo_curso = p_codigo_curso
        ORDER BY id_silabo;
    ELSE
        -- devolver advertencia cuando no exista ningún sílabo
        SELECT CONCAT('No existe ningún sílabo registrado para el curso ', p_codigo_curso) AS advertencia;
    END IF;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_update_password`(IN p_id INT, IN p_new_hash VARCHAR(255))
BEGIN
  UPDATE usuarios SET password_hash = p_new_hash WHERE id_usuario = p_id;
END ;;
DELIMITER ;

