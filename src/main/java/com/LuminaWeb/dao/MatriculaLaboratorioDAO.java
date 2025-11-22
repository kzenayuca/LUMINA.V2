package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MatriculaLaboratorioDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ========================================
    // DTOs
    // ========================================

    public static class LaboratorioDisponible {
        public String codigoCurso;
        public String nombreCurso;
        public List<GrupoLab> grupos = new ArrayList<>();
    }

    public static class GrupoLab {
        public int grupoId;
        public String letraGrupo;
        public int capacidadMaxima;
        public int matriculados;
        public int cuposDisponibles;
        public String diaSemana;
        public String horaInicio;
        public String horaFin;
        public String numeroSalon;
        public String docenteNombre;
        public boolean tieneCruce;
    }

    public static class MatriculaLab {
        public int idMatricula;
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public String diaSemana;
        public String horaInicio;
        public String horaFin;
        public String numeroSalon;
        public String docenteNombre;
    }

    // ========================================
    // MÉTODOS PRINCIPALES
    // ========================================

    /**
     * Obtiene cursos de teoría con laboratorio donde el estudiante aún no tiene laboratorio.
     */
    public List<LaboratorioDisponible> obtenerLaboratoriosDisponibles(String cui) {

        String sqlCursos = """
            SELECT DISTINCT 
                c.codigo_curso,
                c.nombre_curso
            FROM matriculas m
            INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            WHERE m.cui = ?
              AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
              AND ca.estado = 'ACTIVO'
              AND c.tiene_laboratorio = 1
              AND g.tipo_clase = 'TEORIA'
              AND NOT EXISTS (
                  SELECT 1
                  FROM matriculas m2
                  INNER JOIN grupos_curso g2 ON m2.grupo_id = g2.grupo_id
                  WHERE m2.cui = m.cui
                    AND m2.estado_matricula IN ('ACTIVO','PENDIENTE')
                    AND g2.codigo_curso = c.codigo_curso
                    AND g2.tipo_clase = 'LABORATORIO'
              )
            ORDER BY c.codigo_curso
        """;

        List<LaboratorioDisponible> laboratorios = jdbcTemplate.query(
            sqlCursos,
            new Object[]{cui},
            (rs, rowNum) -> {
                LaboratorioDisponible lab = new LaboratorioDisponible();
                lab.codigoCurso = rs.getString("codigo_curso");
                lab.nombreCurso = rs.getString("nombre_curso");
                return lab;
            }
        );

        for (LaboratorioDisponible lab : laboratorios) {
            lab.grupos = obtenerGruposLaboratorio(lab.codigoCurso, cui);
        }

        return laboratorios;
    }

    /**
     * Obtiene grupos de laboratorio disponibles para un curso.
     */
    private List<GrupoLab> obtenerGruposLaboratorio(String codigoCurso, String cui) {

        String sql = """
            SELECT 
                g.grupo_id,
                g.letra_grupo,
                g.capacidad_maxima,
                COUNT(DISTINCT m.id_matricula) AS matriculados,
                h.dia_semana,
                h.hora_inicio,
                h.hora_fin,
                h.numero_salon,
                d.apellidos_nombres AS docente_nombre
            FROM grupos_curso g
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            INNER JOIN horarios h ON g.grupo_id = h.grupo_id
            INNER JOIN docentes d ON h.id_docente = d.id_docente
            LEFT JOIN matriculas m ON g.grupo_id = m.grupo_id 
                AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
            WHERE g.codigo_curso = ?
              AND g.tipo_clase = 'LABORATORIO'
              AND ca.estado = 'ACTIVO'
              AND g.estado = 'ACTIVO'
            GROUP BY g.grupo_id, g.letra_grupo, g.capacidad_maxima,
                     h.dia_semana, h.hora_inicio, h.hora_fin,
                     h.numero_salon, d.apellidos_nombres
            HAVING COUNT(DISTINCT m.id_matricula) < g.capacidad_maxima
            ORDER BY g.letra_grupo
        """;

        List<GrupoLab> grupos = jdbcTemplate.query(
            sql,
            new Object[]{codigoCurso},
            (rs, rowNum) -> {
                GrupoLab grupo = new GrupoLab();
                grupo.grupoId = rs.getInt("grupo_id");
                grupo.letraGrupo = rs.getString("letra_grupo");
                grupo.capacidadMaxima = rs.getInt("capacidad_maxima");
                grupo.matriculados = rs.getInt("matriculados");
                grupo.cuposDisponibles = grupo.capacidadMaxima - grupo.matriculados;
                grupo.diaSemana = rs.getString("dia_semana");
                grupo.horaInicio = rs.getString("hora_inicio");
                grupo.horaFin = rs.getString("hora_fin");
                grupo.numeroSalon = rs.getString("numero_salon");
                grupo.docenteNombre = rs.getString("docente_nombre");
                return grupo;
            }
        );

        for (GrupoLab grupo : grupos) {
            grupo.tieneCruce = verificarCruceHorario(
                    cui, grupo.diaSemana, grupo.horaInicio, grupo.horaFin);
        }

        return grupos;
    }

    /**
     * Verifica si existe cruce de horario.
     */
    private boolean verificarCruceHorario(String cui, String diaSemana,
                                         String horaInicio, String horaFin) {

        String sql = """
            SELECT COUNT(*) AS cruces
            FROM matriculas m
            INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
            INNER JOIN horarios h ON g.grupo_id = h.grupo_id
            WHERE m.cui = ?
              AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
              AND h.dia_semana = ?
              AND (
                    (TIME(?) >= TIME(h.hora_inicio) AND TIME(?) < TIME(h.hora_fin)) OR
                    (TIME(?) > TIME(h.hora_inicio) AND TIME(?) <= TIME(h.hora_fin)) OR
                    (TIME(?) <= TIME(h.hora_inicio) AND TIME(?) >= TIME(h.hora_fin))
              )
        """;

        Integer cruces = jdbcTemplate.queryForObject(
            sql,
            new Object[]{
                cui, diaSemana,
                horaInicio, horaInicio,   // Caso 1
                horaFin, horaFin,         // Caso 2
                horaInicio, horaFin       // Caso 3
            },
            Integer.class
        );

        return cruces != null && cruces > 0;
    }

    /**
     * Matricular al estudiante en un laboratorio + insertar en estudiante_horario.
     */
    public Map<String, Object> matricularEnLaboratorio(String cui, int grupoId) {

        Map<String, Object> result = new HashMap<>();

        try {

            // 1. Validar estudiante
            String sqlEstudiante = """
                SELECT COUNT(*)
                FROM estudiantes e
                INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
                WHERE e.cui = ?
                  AND e.estado_estudiante = 'VIGENTE'
                  AND u.estado_cuenta = 'ACTIVO'
            """;

            Integer existe = jdbcTemplate.queryForObject(sqlEstudiante, new Object[]{cui}, Integer.class);

            if (existe == null || existe == 0) {
                result.put("success", false);
                result.put("mensaje", "Estudiante no encontrado o inactivo");
                return result;
            }

            // 2. Validar grupo
            String sqlGrupo = """
                SELECT 
                    g.codigo_curso,
                    g.capacidad_maxima,
                    COUNT(m.id_matricula) AS matriculados
                FROM grupos_curso g
                LEFT JOIN matriculas m ON g.grupo_id = m.grupo_id
                  AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
                WHERE g.grupo_id = ?
                  AND g.tipo_clase = 'LABORATORIO'
                  AND g.estado = 'ACTIVO'
                GROUP BY g.grupo_id, g.codigo_curso, g.capacidad_maxima
            """;

            Map<String, Object> grupo = jdbcTemplate.queryForMap(sqlGrupo, grupoId);

            int capacidad = ((Number) grupo.get("capacidad_maxima")).intValue();
            int matriculados = ((Number) grupo.get("matriculados")).intValue();
            String codigoCurso = (String) grupo.get("codigo_curso");

            if (matriculados >= capacidad) {
                result.put("success", false);
                result.put("mensaje", "No hay cupos disponibles en este grupo");
                return result;
            }

            // 3. Verificar teoría
            String sqlTeoria = """
                SELECT COUNT(*)
                FROM matriculas m
                INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
                WHERE m.cui = ?
                  AND g.codigo_curso = ?
                  AND g.tipo_clase = 'TEORIA'
                  AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
            """;

            Integer enTeoria = jdbcTemplate.queryForObject(
                    sqlTeoria, new Object[]{cui, codigoCurso}, Integer.class);

            if (enTeoria == 0) {
                result.put("success", false);
                result.put("mensaje", "Debes estar matriculado en la teoría");
                return result;
            }

            // 4. Evitar doble laboratorio
            String sqlYa = """
                SELECT COUNT(*)
                FROM matriculas m
                INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
                WHERE m.cui = ?
                  AND g.codigo_curso = ?
                  AND g.tipo_clase = 'LABORATORIO'
                  AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
            """;

            Integer ya = jdbcTemplate.queryForObject(sqlYa,
                    new Object[]{cui, codigoCurso}, Integer.class);

            if (ya > 0) {
                result.put("success", false);
                result.put("mensaje", "Ya estás matriculado en un laboratorio");
                return result;
            }

            // 5. Verificar cruce REAL
            List<Map<String, Object>> horarios = jdbcTemplate.queryForList(
                    "SELECT dia_semana, hora_inicio, hora_fin FROM horarios WHERE grupo_id = ?", grupoId
            );

            for (Map<String, Object> h : horarios) {
                if (verificarCruceHorario(cui,
                        h.get("dia_semana").toString(),
                        h.get("hora_inicio").toString(),
                        h.get("hora_fin").toString())) {

                    result.put("success", false);
                    result.put("mensaje", "Cruce detectado en " + h.get("dia_semana"));
                    return result;
                }
            }

            // 6. Número matrícula
            Integer numeroMatricula = jdbcTemplate.queryForObject(
                    "SELECT numero_matricula FROM estudiantes WHERE cui = ?",
                    new Object[]{cui},
                    Integer.class
            );

            // 7. Insertar matrícula
            jdbcTemplate.update("""
                INSERT INTO matriculas (cui, grupo_id, numero_matricula, estado_matricula, fecha_matricula)
                VALUES (?, ?, ?, 'ACTIVO', NOW())
            """, cui, grupoId, numeroMatricula);

            // 8. Insertar horarios
            int horariosInsertados = jdbcTemplate.update("""
                INSERT INTO estudiante_horario (cui_est, id_horario)
                SELECT ?, h.id_horario
                FROM horarios h
                WHERE h.grupo_id = ?
            """, cui, grupoId);

            result.put("success", true);
            result.put("mensaje", "Matriculado exitosamente. Horarios añadidos: " + horariosInsertados);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("mensaje", "Error: " + e.getMessage());
        }

        return result;
    }

    // ========================================
    // MATRÍCULAS ACTUALES
    // ========================================

    public List<MatriculaLab> obtenerMatriculasLaboratorio(String cui) {

        String sql = """
            SELECT 
                m.id_matricula,
                c.codigo_curso,
                c.nombre_curso,
                g.letra_grupo,
                h.dia_semana,
                h.hora_inicio,
                h.hora_fin,
                h.numero_salon,
                d.apellidos_nombres AS docente_nombre
            FROM matriculas m
            INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN horarios h ON g.grupo_id = h.grupo_id
            INNER JOIN docentes d ON h.id_docente = d.id_docente
            WHERE m.cui = ?
              AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
              AND g.tipo_clase = 'LABORATORIO'
            ORDER BY c.codigo_curso
        """;

        return jdbcTemplate.query(
            sql, new Object[]{cui},
            (rs, rowNum) -> {
                MatriculaLab mat = new MatriculaLab();
                mat.idMatricula = rs.getInt("id_matricula");
                mat.codigoCurso = rs.getString("codigo_curso");
                mat.nombreCurso = rs.getString("nombre_curso");
                mat.letraGrupo = rs.getString("letra_grupo");
                mat.diaSemana = rs.getString("dia_semana");
                mat.horaInicio = rs.getString("hora_inicio");
                mat.horaFin = rs.getString("hora_fin");
                mat.numeroSalon = rs.getString("numero_salon");
                mat.docenteNombre = rs.getString("docente_nombre");
                return mat;
            }
        );
    }

    // ========================================
    // CANCELAR MATRÍCULA
    // ========================================

    public boolean cancelarMatriculaLaboratorio(int idMatricula, String cui) {

        try {

            String sqlVerificar = """
                SELECT g.grupo_id
                FROM matriculas m
                INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
                WHERE m.id_matricula = ?
                  AND m.cui = ?
                  AND m.estado_matricula IN ('ACTIVO','PENDIENTE')
                  AND g.tipo_clase = 'LABORATORIO'
            """;

            List<Integer> ids = jdbcTemplate.queryForList(
                sqlVerificar, new Object[]{idMatricula, cui}, Integer.class);

            if (ids.isEmpty()) return false;

            int grupoId = ids.get(0);

            jdbcTemplate.update("""
                DELETE FROM estudiante_horario
                WHERE cui_est = ?
                  AND id_horario IN (
                      SELECT id_horario FROM horarios WHERE grupo_id = ?
                  )
            """, cui, grupoId);

            jdbcTemplate.update("""
                DELETE FROM matriculas
                WHERE id_matricula = ?
            """, idMatricula);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========================================
    // PERÍODO
    // ========================================

    public boolean hayPeriodoActivo() {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM periodos_matricula_laboratorio
            WHERE estado = 'ACTIVO'
              AND fecha_inicio <= NOW()
              AND fecha_fin >= NOW()
        """, Integer.class);

        return count != null && count > 0;
    }

    public Map<String, Object> obtenerPeriodoActivo() {

        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
            SELECT id_periodo, codigo_curso, fecha_inicio, fecha_fin, cupos_disponibles, estado
            FROM periodos_matricula_laboratorio
            WHERE estado = 'ACTIVO'
              AND fecha_inicio <= NOW()
              AND fecha_fin >= NOW()
            LIMIT 1
        """);

        return result.isEmpty() ? null : result.get(0);
    }
}
