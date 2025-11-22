package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class SecretariaDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // -------------------------------------
    // DTOs
    // -------------------------------------

    public static class CursoConLaboratorio {
        public String codigoCurso;
        public String nombreCurso;
        public int numeroGruposLaboratorio;
        public String estado;
    }

    public static class PeriodoMatriculaLab {
        public int idPeriodo;
        public LocalDateTime fechaInicio;
        public LocalDateTime fechaFin;
        public String codigoCurso;
        public String nombreCurso;
        public int cuposDisponibles;
        public String estado;
    }

    public static class GrupoLaboratorio {
        public int grupoId;
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public int capacidadMaxima;
        public int matriculados;
        public String estado;
    }

    // -------------------------------------
    // MÉTODOS PRINCIPALES
    // -------------------------------------

    /**
     * Obtiene cursos con laboratorio ACTIVO
     */
    public List<CursoConLaboratorio> obtenerCursosConLaboratorio() {
        String sql = """
            SELECT codigo_curso, nombre_curso, numero_grupos_laboratorio, estado
            FROM cursos
            WHERE tiene_laboratorio = 1 AND estado = 'ACTIVO'
            ORDER BY nombre_curso
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CursoConLaboratorio curso = new CursoConLaboratorio();
            curso.codigoCurso = rs.getString("codigo_curso");
            curso.nombreCurso = rs.getString("nombre_curso");
            curso.numeroGruposLaboratorio = rs.getInt("numero_grupos_laboratorio");
            curso.estado = rs.getString("estado");
            return curso;
        });
    }

    /**
     * Obtiene grupos de laboratorio del ciclo activo
     */
    public List<GrupoLaboratorio> obtenerGruposLaboratorio(String codigoCurso) {
        String sql = """
            SELECT 
                g.grupo_id,
                g.codigo_curso,
                c.nombre_curso,
                g.letra_grupo,
                g.capacidad_maxima,
                COUNT(m.id_matricula) AS matriculados,
                g.estado
            FROM grupos_curso g
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            LEFT JOIN matriculas m ON g.grupo_id = m.grupo_id 
                AND m.estado_matricula = 'ACTIVO'
            WHERE g.tipo_clase = 'LABORATORIO'
                AND ca.estado = 'ACTIVO'
                AND (? IS NULL OR g.codigo_curso = ?)
            GROUP BY g.grupo_id, g.codigo_curso, c.nombre_curso, 
                     g.letra_grupo, g.capacidad_maxima, g.estado
            ORDER BY g.codigo_curso, g.letra_grupo
        """;

        return jdbcTemplate.query(
            sql,
            new Object[]{codigoCurso, codigoCurso},
            (rs, rowNum) -> {
                GrupoLaboratorio grupo = new GrupoLaboratorio();
                grupo.grupoId = rs.getInt("grupo_id");
                grupo.codigoCurso = rs.getString("codigo_curso");
                grupo.nombreCurso = rs.getString("nombre_curso");
                grupo.letraGrupo = rs.getString("letra_grupo");
                grupo.capacidadMaxima = rs.getInt("capacidad_maxima");
                grupo.matriculados = rs.getInt("matriculados");
                grupo.estado = rs.getString("estado");
                return grupo;
            }
        );
    }

    /**
     * Verifica si algún período está activo AHORA
     */
    public boolean hayPeriodoActivoLaboratorios() {
        // Primero actualiza estados automáticamente
        actualizarEstadosPeriodos();
        
        String sql = """
            SELECT COUNT(*) 
            FROM periodos_matricula_laboratorio
            WHERE estado = 'ACTIVO'
        """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }

    /**
     * Estadísticas generales
     */
    public Map<String, Object> obtenerEstadisticasMatricula() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalEstudiantes",
            jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM estudiantes e
                INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
                WHERE u.estado_cuenta = 'ACTIVO' 
                  AND e.estado_estudiante = 'VIGENTE'
            """, Integer.class)
        );

        stats.put("totalDocentes",
            jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM docentes d
                INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
                WHERE u.estado_cuenta = 'ACTIVO'
            """, Integer.class)
        );

        stats.put("totalCursos",
            jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM cursos WHERE estado='ACTIVO'
            """, Integer.class)
        );

        stats.put("totalLaboratorios",
            jdbcTemplate.queryForObject("""
                SELECT COUNT(*) 
                FROM grupos_curso g
                INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
                WHERE g.tipo_clase='LABORATORIO'
                  AND ca.estado='ACTIVO'
            """, Integer.class)
        );

        return stats;
    }

    /**
     * Activa o desactiva matrícula de laboratorio
     */
    public boolean habilitarMatriculaLaboratorio(String codigoCurso, boolean habilitar) {
        String nuevoEstado = habilitar ? "ACTIVO" : "CERRADO";

        String sql = """
            UPDATE grupos_curso g
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            SET g.estado = ?
            WHERE g.tipo_clase='LABORATORIO'
              AND ca.estado='ACTIVO'
              AND (? IS NULL OR g.codigo_curso = ?)
        """;

        int rows = jdbcTemplate.update(sql, nuevoEstado, codigoCurso, codigoCurso);
        return rows > 0;
    }

    // -----------------------------------------------------------------
    // GUARDAR PERÍODO EN BD (SIN creado_por)
    // -----------------------------------------------------------------
    public int guardarPeriodoMatricula(
            String codigoCurso,
            LocalDateTime inicio,
            LocalDateTime fin,
            int cupos) {

        String sql = """
            INSERT INTO periodos_matricula_laboratorio 
            (codigo_curso, fecha_inicio, fecha_fin, cupos_disponibles, estado)
            VALUES (?, ?, ?, ?, 'PROGRAMADO')
        """;

        jdbcTemplate.update(sql, codigoCurso, inicio, fin, cupos);

        // Actualizar estados después de insertar
        actualizarEstadosPeriodos();

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /**
     * Listar períodos guardados (con estado actualizado)
     */
    public List<PeriodoMatriculaLab> obtenerPeriodosMatricula() {
        // Actualizar estados antes de listar
        actualizarEstadosPeriodos();
        
        String sql = """
            SELECT 
                p.id_periodo,
                p.codigo_curso,
                c.nombre_curso,
                p.fecha_inicio,
                p.fecha_fin,
                p.cupos_disponibles,
                p.estado
            FROM periodos_matricula_laboratorio p
            LEFT JOIN cursos c ON p.codigo_curso = c.codigo_curso
            ORDER BY p.fecha_inicio DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            PeriodoMatriculaLab p = new PeriodoMatriculaLab();
            p.idPeriodo = rs.getInt("id_periodo");
            p.codigoCurso = rs.getString("codigo_curso");
            p.nombreCurso = rs.getString("nombre_curso");
            p.fechaInicio = rs.getTimestamp("fecha_inicio").toLocalDateTime();
            p.fechaFin = rs.getTimestamp("fecha_fin").toLocalDateTime();
            p.cuposDisponibles = rs.getInt("cupos_disponibles");
            p.estado = rs.getString("estado");
            return p;
        });
    }

    /**
     * Eliminar período
     */
    public boolean eliminarPeriodoMatricula(int idPeriodo) {
        int rows = jdbcTemplate.update(
            "DELETE FROM periodos_matricula_laboratorio WHERE id_periodo = ?",
            idPeriodo
        );
        return rows > 0;
    }

    // -----------------------------------------------------------------
    // ACTUALIZACIÓN AUTOMÁTICA DE ESTADOS
    // -----------------------------------------------------------------
    /**
     * Actualiza automáticamente los estados según fecha/hora actual
     * PROGRAMADO → ACTIVO → FINALIZADO
     */
    public void actualizarEstadosPeriodos() {
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Marcar como ACTIVO los que ya iniciaron pero no terminaron
        String sqlActivar = """
            UPDATE periodos_matricula_laboratorio
            SET estado = 'ACTIVO'
            WHERE estado = 'PROGRAMADO'
              AND fecha_inicio <= ?
              AND fecha_fin > ?
        """;
        jdbcTemplate.update(sqlActivar, ahora, ahora);

        // 2. Marcar como FINALIZADO los que ya terminaron
        String sqlFinalizar = """
            UPDATE periodos_matricula_laboratorio
            SET estado = 'FINALIZADO'
            WHERE estado IN ('ACTIVO', 'PROGRAMADO')
              AND fecha_fin <= ?
        """;
        jdbcTemplate.update(sqlFinalizar, ahora);
    }
}