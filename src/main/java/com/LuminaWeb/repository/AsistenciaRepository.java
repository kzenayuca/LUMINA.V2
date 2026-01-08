package com.LuminaWeb.repository;

import com.LuminaWeb.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class AsistenciaRepository {

    private final JdbcTemplate jdbc;

    public AsistenciaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Llama al proc sp_cursos_y_grupos_por_docente
    public List<CursoGrupoDTO> getCursosYGruposPorDocente(String correo) {
        String sql = "CALL sp_cursos_y_grupos_por_docente(?)";
        return jdbc.query(sql, new Object[]{correo}, new RowMapper<CursoGrupoDTO>() {
            @Override
            public CursoGrupoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new CursoGrupoDTO(
                    rs.getString("codigo_curso"),
                    rs.getString("nombre_curso"),
                    rs.getInt("grupo_id"),
                    rs.getString("letra_grupo"),
                    rs.getString("tipo_clase")
                );
            }
        });
    }

    // Verifica silabo con sp_obtener_silabos_por_curso
    public List<Map<String, Object>> getSilabosPorCursoRaw(String codigoCurso) {
        String sql = "CALL sp_obtener_silabos_por_curso(?)";
        return jdbc.queryForList(sql, codigoCurso);
    }

    // Obtener horarios para el grupo y dia de semana actual
    public List<Map<String,Object>> getHorariosPorGrupoYDia(Integer grupoId, String diaSemana) {
        String sql = "SELECT * FROM horarios WHERE grupo_id = ? AND dia_semana = ? AND estado = 'ACTIVO'";
        return jdbc.queryForList(sql, grupoId, diaSemana);
    }

    // Insertar o recuperar control_asistencia (verifica unique)
    public int insertarControlAsistencia(int idHorario, String fecha, String horaApertura, String horaCierre, String estado) {
        // intenta insertar; si existe unique, ignora y devuelve el id existente
        String insert = "INSERT INTO control_asistencia (id_horario, fecha, hora_apertura, hora_cierre, estado) " +
                "VALUES (?, ?, ?, ?, ?)";
        try {
            jdbc.update(insert, idHorario, fecha, horaApertura, horaCierre, estado);
            Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
            return id != null ? id : -1;
        } catch (Exception ex) {
            // probablemente existe -> obtener id existente
            String q = "SELECT id_control FROM control_asistencia WHERE id_horario = ? AND fecha = ?";
            Integer id = jdbc.queryForObject(q, new Object[]{idHorario, fecha}, Integer.class);
            return id;
        }
    }

    // Insertar asistencia docente (unique key evita duplicados)
    public void insertarAsistenciaDocente(int idHorario, int idDocente, String fecha, String horaRegistro, String ip, String tipoUbicacion, boolean presente) {
        String sql = "INSERT INTO asistencias_docente (id_horario, id_docente, fecha, hora_registro, ip_registro, tipo_ubicacion, presente) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbc.update(sql, idHorario, idDocente, fecha, horaRegistro, ip, tipoUbicacion, presente);
        } catch (Exception ex) {
            // si ya existe unique, podemos ignorar o actualizar; para simplificar, ignore
        }
    }

    // Obtener id_docente por correo de usuario
    public Integer getIdDocentePorCorreo(String correo) {
        String sql = "SELECT d.id_docente FROM docentes d INNER JOIN usuarios u ON d.id_usuario = u.id_usuario WHERE u.correo_institucional = ?";
        try {
            return jdbc.queryForObject(sql, new Object[]{correo}, Integer.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // Obtener matriculas y estudiantes para el grupo_id
    public List<EstudianteAsistenciaDTO> getEstudiantesPorGrupo(int grupoId, String fecha, int idHorario, int registradoPorDocenteId) {
        String sql = "SELECT m.id_matricula, m.cui, m.numero_matricula, e.apellidos_nombres as nombre " +
                     "FROM matriculas m INNER JOIN estudiantes e ON m.cui = e.cui WHERE m.grupo_id = ? AND m.estado_matricula = 'ACTIVO'";
        return jdbc.query(sql, new Object[]{grupoId}, (rs, rowNum) -> {
            EstudianteAsistenciaDTO s = new EstudianteAsistenciaDTO();
            s.setIdMatricula(rs.getInt("id_matricula"));
            s.setCui(rs.getString("cui"));
            s.setNumeroMatricula(rs.getString("numero_matricula"));
            s.setNombre(rs.getString("nombre"));
            s.setEstado_asistencia("FALTA"); // por defecto
            return s;
        });
    }

    // Insertar asistencias_estudiante en bloque
    public void insertarAsistenciasEstudianteBulk(int idHorario, String fecha, List<EstudianteAsistenciaDTO> estudiantes, int registradoPorDocenteId) {
        String insert = "INSERT INTO asistencias_estudiante (id_matricula, id_horario, fecha, estado_asistencia, registrado_por) VALUES (?, ?, ?, ?, ?)";
        for (EstudianteAsistenciaDTO s : estudiantes) {
            try {
                jdbc.update(insert, s.getIdMatricula(), idHorario, fecha, s.getEstado_asistencia(), registradoPorDocenteId);
            } catch (Exception ex) {
                // si existe unique, se ignora
            }
        }
    }

    // Update asistencias_estudiante (guardar cambios)
    public void actualizarAsistenciaEstudiante(Integer idMatricula, Integer idHorario, String fecha, String estadoAsistencia) {
        String update = "UPDATE asistencias_estudiante SET estado_asistencia = ? WHERE id_matricula = ? AND id_horario = ? AND fecha = ?";
        jdbc.update(update, estadoAsistencia, idMatricula, idHorario, fecha);
    }

    // Obtener codigo_curso por id_horario
    public String getCodigoCursoPorHorario(int idHorario) {
        String sql = "SELECT gc.codigo_curso FROM horarios h INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id WHERE h.id_horario = ?";
        try {
            return jdbc.queryForObject(sql, new Object[]{idHorario}, String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // Obtener grupo_id por id_horario
    public Integer getGrupoIdPorHorario(int idHorario) {
        String sql = "SELECT grupo_id FROM horarios WHERE id_horario = ?";
        try {
            return jdbc.queryForObject(sql, new Object[]{idHorario}, Integer.class);
        } catch (Exception ex) {
            return null;
        }
    }

    //Cambios en AsistenciaService.java
    // Marcar el primer tema pendiente como completado para el grupo
    public void marcarTemaCompletado(int grupoId) {
        // Obtener codigo_curso por grupo_id
        String sqlCurso = "SELECT codigo_curso FROM grupos_curso WHERE grupo_id = ?";
        String codigoCurso = null;
        try {
            System.out.println("Obteniendo codigo_curso para grupoId: " + grupoId);
            codigoCurso = jdbc.queryForObject(sqlCurso, new Object[]{grupoId}, String.class);
            System.out.println("codigo_curso obtenido: " + codigoCurso);
        } catch (Exception ex) {
            System.out.println("No se encontró codigo_curso para grupoId: " + grupoId);
            return; // No grupo encontrado
        }
        if (codigoCurso == null) return;

        // Obtener id_silabo activo para el curso
        String sqlSilabo = "SELECT id_silabo FROM silabos WHERE codigo_curso = ? AND estado = 'APROBADO' LIMIT 1";
        Integer idSilabo = null;
        try {
            System.out.println("Obteniendo id_silabo activo para codigoCurso: " + codigoCurso);
            idSilabo = jdbc.queryForObject(sqlSilabo, new Object[]{codigoCurso}, Integer.class);
            System.out.println("id_silabo obtenido: " + idSilabo);
        } catch (Exception ex) {
            System.out.println("No se encontró silabo activo para codigoCurso: " + codigoCurso);
            return; // No silabo activo
        }
        if (idSilabo == null) return;

        // Verificar si ya se completó un tema hoy para este silabo
        String sqlCheck = "SELECT COUNT(*) FROM temas t INNER JOIN unidades u ON t.unidad_id = u.unidad_id WHERE u.id_silabo = ? AND t.estado = 'COMPLETADO' AND t.fecha_completado = CURDATE()";
        Integer countCompletadosHoy = null;
        try {
            countCompletadosHoy = jdbc.queryForObject(sqlCheck, new Object[]{idSilabo}, Integer.class);
        } catch (Exception ex) {
            System.out.println("Error al verificar temas completados hoy: " + ex.getMessage());
            return;
        }
        if (countCompletadosHoy != null && countCompletadosHoy > 0) {
            System.out.println("Ya se completó un tema hoy para el silabo " + idSilabo + ", no se marca otro.");
            return;
        }

        // Obtener el primer tema pendiente
        String sqlTema = "SELECT t.id_tema FROM temas t INNER JOIN unidades u ON t.unidad_id = u.unidad_id WHERE u.id_silabo = ? AND t.estado = 'PENDIENTE' ORDER BY t.id_tema LIMIT 1";
        Integer idTema = null;
        try {
            System.out.println("Obteniendo primer tema pendiente para idSilabo: " + idSilabo);
            idTema = jdbc.queryForObject(sqlTema, new Object[]{idSilabo}, Integer.class);
        } catch (Exception ex) {
            System.out.println("No se encontró tema pendiente para idSilabo: " + idSilabo);
            return; // No tema pendiente
        }
        if (idTema != null) {
            String update = "UPDATE temas SET estado = 'COMPLETADO', fecha_completado = CURDATE() WHERE id_tema = ?";
            //linea en consola para ver si logro insertar
            System.out.println("Marcando tema como COMPLETADO, id_tema: " + idTema);
            jdbc.update(update, idTema);
        }
    }

}
