package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import com.LuminaWeb.dao.AdministradorDAO.ActividadReciente;
import com.LuminaWeb.dao.AdministradorDAO.Curso;
import com.LuminaWeb.dao.AdministradorDAO.CursoAsignado;
import com.LuminaWeb.dao.AdministradorDAO.Estadisticas;
import com.LuminaWeb.dao.AdministradorDAO.GrupoCurso;
import com.LuminaWeb.dao.AdministradorDAO.Usuario;

import java.util.*;

import java.io.BufferedReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

@Repository
public class AdministradorDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // DTOs ADMIN
    public static class Usuario {
        public int idUsuario;
        public String correoInstitucional;
        public String tipoUsuario;
        public String estadoCuenta;
        public String nombreCompleto;
        public String fechaCreacion;
        public String fechaUltimoAcceso;
    }

    public static class ActividadReciente {
        public int idLog;
        public String usuario;
        public String accion;
        public String tablaAfectada;
        public String descripcion;
        public String fechaAccion;
    }

    public static class Profesor {
        public int idDocente;
        public int idUsuario;
        public String apellidosNombres;
        public String departamento;
        public String correoInstitucional;
        public boolean esResponsableTeoria;
        public String tipoClase; //TEORIA, LABORATORIO, o LOS DOS
        public int cursosAsignados;
        public String cursosDetalleRaw; 
        public List<GrupoCurso> cursosDetalle; 
    }

    public static class Curso {
        public String codigoCurso;
        public String nombreCurso;
        public boolean tieneLaboratorio;
        public int numeroGruposTeoria;
        public int numeroGruposLaboratorio;
        public String estado;
        public List<GrupoCurso> grupos;
    }

    public static class GrupoCurso {
        public int grupoId;
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public String tipoClase;
        public int capacidadMaxima;
        public String estado;
        public Integer idDocente;
        public String nombreDocente;
    }

    public static class CursoAsignado {
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public String tipoClase;
        public int grupoId;
    }

    public static class Horario {
        public int idHorario;
        public int grupoId;
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public String tipoClase;
        public String numeroSalon;
        public String diaSemana;
        public String horaInicio;
        public String horaFin;
        public Integer idDocente;
        public String nombreDocente;
        public String estado;
        public String motivo;
        public String descripcion;
    }

    public static class Estudiante {
        public String cui;
        public int idUsuario;
        public String apellidosNombres;
        public String correoInstitucional;
        public int numeroMatricula;
        public String estadoEstudiante;
        public int cursosMatriculados;
        public List<CursoAsignado> cursosDetalle;            
        public List<CursoAsignado> laboratoriosMatriculados; 
        public List<CursoAsignado> laboratoriosDisponibles;
    }

    public static class Estadisticas {
        public int totalUsuarios;
        public int estudiantesActivos;
        public int docentesActivos;
        public int docentesTeoria;        
        public int docentesLaboratorio;
        public int cursosActivos;
        public int aulasDisponibles;
    }

// 1. DASHBOARD
public Estadisticas obtenerEstadisticas() {
    String sql = """
        SELECT 
            (SELECT COUNT(*) FROM usuarios WHERE estado_cuenta = 'ACTIVO') as total_usuarios,
            (SELECT COUNT(*) FROM estudiantes e 
             INNER JOIN usuarios u ON e.id_usuario = u.id_usuario 
             WHERE u.estado_cuenta = 'ACTIVO' AND e.estado_estudiante = 'VIGENTE') as estudiantes_activos,
            (SELECT COUNT(*) FROM docentes d 
             INNER JOIN usuarios u ON d.id_usuario = u.id_usuario 
             WHERE u.estado_cuenta = 'ACTIVO') as docentes_activos,
            (SELECT COUNT(DISTINCT d.id_docente) FROM docentes d 
             INNER JOIN usuarios u ON d.id_usuario = u.id_usuario 
             INNER JOIN horarios h ON d.id_docente = h.id_docente
             INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
             WHERE u.estado_cuenta = 'ACTIVO' AND gc.tipo_clase = 'TEORIA') as docentes_teoria,
            (SELECT COUNT(DISTINCT d.id_docente) FROM docentes d 
             INNER JOIN usuarios u ON d.id_usuario = u.id_usuario 
             INNER JOIN horarios h ON d.id_docente = h.id_docente
             INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
             WHERE u.estado_cuenta = 'ACTIVO' AND gc.tipo_clase = 'LABORATORIO') as docentes_laboratorio,
            (SELECT COUNT(*) FROM cursos WHERE estado = 'ACTIVO') as cursos_activos,
            (SELECT COUNT(*) FROM salones WHERE estado = 'DISPONIBLE') as aulas_disponibles
    """;
    
    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
        Estadisticas e = new Estadisticas();
        e.totalUsuarios = rs.getInt("total_usuarios");
        e.estudiantesActivos = rs.getInt("estudiantes_activos");
        e.docentesActivos = rs.getInt("docentes_activos");
        e.docentesTeoria = rs.getInt("docentes_teoria");
        e.docentesLaboratorio = rs.getInt("docentes_laboratorio");
        e.cursosActivos = rs.getInt("cursos_activos");
        e.aulasDisponibles = rs.getInt("aulas_disponibles");
        return e;
    });
}

public List<ActividadReciente> obtenerActividadReciente(int limite) {
        String sql = """
            SELECT l.id_log, u.correo_institucional as usuario, l.accion, 
                   l.tabla_afectada, l.descripcion, l.fecha_accion
            FROM log_actividades l
            INNER JOIN usuarios u ON l.id_usuario = u.id_usuario
            ORDER BY l.fecha_accion DESC
            LIMIT ?
        """;
        
        return jdbcTemplate.query(sql, new Object[]{limite}, (rs, rowNum) -> {
            ActividadReciente a = new ActividadReciente();
            a.idLog = rs.getInt("id_log");
            a.usuario = rs.getString("usuario");
            a.accion = rs.getString("accion");
            a.tablaAfectada = rs.getString("tabla_afectada");
            a.descripcion = rs.getString("descripcion");
            a.fechaAccion = rs.getString("fecha_accion");
            return a;
        });
    }

// 2. GESTIÓN DE USUARIOS 
    public List<Usuario> obtenerTodosUsuarios() {
        String sql = """
            SELECT u.id_usuario, u.correo_institucional, t.nombre_tipo, u.estado_cuenta,
                   u.fecha_creacion, u.fecha_ultimo_acceso,
                   COALESCE(e.apellidos_nombres, d.apellidos_nombres, 'Sin nombre') as nombre_completo
            FROM usuarios u
            INNER JOIN tipos_usuario t ON u.tipo_id = t.tipo_id
            LEFT JOIN estudiantes e ON u.id_usuario = e.id_usuario
            LEFT JOIN docentes d ON u.id_usuario = d.id_usuario
            ORDER BY u.fecha_creacion DESC
        """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Usuario u = new Usuario();
            u.idUsuario = rs.getInt("id_usuario");
            u.correoInstitucional = rs.getString("correo_institucional");
            u.tipoUsuario = rs.getString("nombre_tipo");
            u.estadoCuenta = rs.getString("estado_cuenta");
            u.nombreCompleto = rs.getString("nombre_completo");
            u.fechaCreacion = safeStr(rs.getObject("fecha_creacion"));
            u.fechaUltimoAcceso = safeStr(rs.getObject("fecha_ultimo_acceso"));
            return u;
        });
    }

    public boolean crearUsuario(String correo, String password, String salt, int tipoId, String nombreCompleto) {
        try {
            //Insertar usuario
            String sqlUsuario = """
                INSERT INTO usuarios (correo_institucional, password_hash, salt, tipo_id, estado_cuenta, primer_acceso)
                VALUES (?, ?, ?, ?, 'ACTIVO', 1)
            """;            
            jdbcTemplate.update(sqlUsuario, correo, password, salt, tipoId);            
            //Obtener id del usuario recién creado
            Integer idUsuario = jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM usuarios WHERE correo_institucional = ?", 
                Integer.class, correo
            );
            
            if (idUsuario == null) return false;            
            //Insertar en tabla específica según tipo
            if (tipoId == 1) { 
                String cui = generarCUI();
                int numeroMatricula = generarNumeroMatricula();
                String sqlEst = """
                    INSERT INTO estudiantes (cui, id_usuario, apellidos_nombres, numero_matricula, estado_estudiante)
                    VALUES (?, ?, ?, ?, 'VIGENTE')
                """;
                jdbcTemplate.update(sqlEst, cui, idUsuario, nombreCompleto, numeroMatricula);
                
            } else if (tipoId == 2) { 
                String sqlDoc = """
                    INSERT INTO docentes (id_usuario, apellidos_nombres, departamento, es_responsable_teoria)
                    VALUES (?, ?, 'Sin asignar', 0)
                """;
                jdbcTemplate.update(sqlDoc, idUsuario, nombreCompleto);
            }            
            //Log de actividad
            registrarActividad(idUsuario, "CREAR_USUARIO", "usuarios", 
                "Usuario creado: " + correo + " - Tipo: " + tipoId);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        try {
            String sql = "UPDATE usuarios SET estado_cuenta = ? WHERE id_usuario = ?";
            int rows = jdbcTemplate.update(sql, nuevoEstado, idUsuario);
            
            if (rows > 0) {
                registrarActividad(idUsuario, "CAMBIAR_ESTADO", "usuarios", 
                    "Estado cambiado a: " + nuevoEstado);
            }
            
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminarUsuario(int idUsuario) {
        try {
            String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
            int rows = jdbcTemplate.update(sql, idUsuario);
            
            if (rows > 0) {
                registrarActividad(idUsuario, "ELIMINAR_USUARIO", "usuarios", 
                    "Usuario eliminado: ID " + idUsuario);
            }
            
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

// 3. GESTIÓN DE PROFESORES
public List<CursoAsignado> obtenerCursosProfesor(int idDocente) {
    String sql = """
        SELECT DISTINCT gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase, gc.grupo_id
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE h.id_docente = ?
        ORDER BY gc.tipo_clase, gc.codigo_curso, gc.letra_grupo
    """;

    List<CursoAsignado> cursos = jdbcTemplate.query(sql, new Object[]{idDocente}, (rs, rowNum) -> {
        CursoAsignado c = new CursoAsignado();
        c.codigoCurso = rs.getString("codigo_curso");
        c.nombreCurso = rs.getString("nombre_curso");
        c.letraGrupo = rs.getString("letra_grupo");
        c.tipoClase = rs.getString("tipo_clase");
        c.grupoId = rs.getInt("grupo_id"); 
        return c;
    });
    
    System.out.println("=== Cursos del docente " + idDocente + " ===");
    for (CursoAsignado c : cursos) {
        System.out.println("Curso: " + c.codigoCurso + ", Grupo: " + c.letraGrupo + ", GrupoID: " + c.grupoId);
    }
    
    return cursos;
}

public boolean actualizarProfesor(int idDocente, String apellidosNombres, String correo, 
                                 String departamento, String password) {
    try {
        String sqlDoc = "UPDATE docentes SET apellidos_nombres = ?, departamento = ? WHERE id_docente = ?";
        jdbcTemplate.update(sqlDoc, apellidosNombres, departamento, idDocente);

        //id_usuario
        Integer idUsuario = jdbcTemplate.queryForObject(
            "SELECT id_usuario FROM docentes WHERE id_docente = ?", Integer.class, idDocente
        );

        if (idUsuario != null) {
            //correo y opcionalmente password
            if (password != null && !password.isEmpty()) {
                jdbcTemplate.update(
                    "UPDATE usuarios SET correo_institucional = ?, password_hash = ? WHERE id_usuario = ?",
                    correo, password, idUsuario
                );
            } else {
                jdbcTemplate.update(
                    "UPDATE usuarios SET correo_institucional = ? WHERE id_usuario = ?",
                    correo, idUsuario
                );
            }
            registrarActividad(idUsuario, "ACTUALIZAR_DOCENTE", "docentes", 
                "Docente actualizado: " + apellidosNombres);
        }
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

public boolean asignarCursoAProfesor(int idDocente, int grupoId) {
    try {
        String sql = "UPDATE horarios SET id_docente = ? WHERE grupo_id = ?";
        int rows = jdbcTemplate.update(sql, idDocente, grupoId);
        
        if (rows > 0) {
            registrarActividad(obtenerIdUsuarioPorDocente(idDocente), "ASIGNAR_CURSO", "horarios", 
                "Grupo " + grupoId + " asignado al docente " + idDocente);
        }
        return rows > 0;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

public boolean desasignarCursoDeProfesor(int idDocente, int grupoId) {
    try {
        System.out.println("=== DESASIGNAR CURSO ===");
        System.out.println("ID Docente: " + idDocente);
        System.out.println("Grupo ID: " + grupoId);        
        //Verificar que existen horarios para este grupo
        String checkSql = "SELECT COUNT(*) FROM horarios WHERE grupo_id = ?";
        Integer totalHorarios = jdbcTemplate.queryForObject(checkSql, Integer.class, grupoId);
        System.out.println("Total horarios del grupo: " + totalHorarios);        
        //Verificar que el docente tiene ese grupo asignado
        String checkDocenteSql = "SELECT COUNT(*) FROM horarios WHERE grupo_id = ? AND id_docente = ?";
        Integer horariosDocente = jdbcTemplate.queryForObject(checkDocenteSql, Integer.class, grupoId, idDocente);
        System.out.println("Horarios asignados al docente: " + horariosDocente);
        
        if (horariosDocente == null || horariosDocente == 0) {
            System.out.println("ERROR: El docente " + idDocente + " no tiene asignado el grupo " + grupoId);
            return false;
        }        
        //Desasignar 
        String sql = "UPDATE horarios SET id_docente = NULL WHERE grupo_id = ? AND id_docente = ?";
        int rows = jdbcTemplate.update(sql, grupoId, idDocente);
        
        System.out.println("Filas actualizadas: " + rows);
        
        if (rows > 0) {
            registrarActividad(obtenerIdUsuarioPorDocente(idDocente), "DESASIGNAR_CURSO", "horarios", 
                "Grupo " + grupoId + " desasignado del docente " + idDocente);
            System.out.println("Desasignación exitosa");
        } else {
            System.out.println("ERROR: No se actualizó ninguna fila");
        }
        
        return rows > 0;
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("EXCEPCIÓN al desasignar curso: " + e.getMessage());
        return false;
    }
}

public List<Profesor> obtenerTodosProfesores() {
    String sql = """
    SELECT d.id_docente, d.id_usuario, d.apellidos_nombres, d.departamento, 
        u.correo_institucional, d.es_responsable_teoria,
        COUNT(DISTINCT h.id_horario) as cursos_asignados,
        GROUP_CONCAT(DISTINCT gc.tipo_clase) as tipos_clase,
        -- Incluimos el grupo_id al detalle para que el frontend pueda identificar el grupo correctamente
        GROUP_CONCAT(DISTINCT CONCAT(gc.codigo_curso,'||',c.nombre_curso,'||',gc.letra_grupo,'||',gc.tipo_clase,'||',gc.grupo_id) SEPARATOR ';') as cursos_detalle
       FROM docentes d
       INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
       LEFT JOIN horarios h ON d.id_docente = h.id_docente
       LEFT JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
       LEFT JOIN cursos c ON gc.codigo_curso = c.codigo_curso          -- <<--- AÑADIDO
       WHERE u.estado_cuenta = 'ACTIVO'
       GROUP BY d.id_docente, d.id_usuario, d.apellidos_nombres, d.departamento, 
                u.correo_institucional, d.es_responsable_teoria
       ORDER BY d.apellidos_nombres
    """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        Profesor p = new Profesor();
        p.idDocente = rs.getInt("id_docente");
        p.idUsuario = rs.getInt("id_usuario");
        p.apellidosNombres = rs.getString("apellidos_nombres");
        p.departamento = rs.getString("departamento");
        p.correoInstitucional = rs.getString("correo_institucional");
        p.esResponsableTeoria = rs.getBoolean("es_responsable_teoria");
        p.cursosAsignados = rs.getInt("cursos_asignados");

        String tipos = rs.getString("tipos_clase");
        p.cursosDetalleRaw = rs.getString("cursos_detalle");
        p.cursosDetalle = new ArrayList<>();
        if (p.cursosDetalleRaw != null && !p.cursosDetalleRaw.isEmpty()) {
            String[] parts = p.cursosDetalleRaw.split(";");
            for (String part : parts) {
                String[] fields = part.split("\\|\\|");
                if (fields.length >= 4) {
                    GrupoCurso g = new GrupoCurso();
                    g.codigoCurso = fields[0];
                    g.nombreCurso = fields[1];
                    g.letraGrupo = fields[2];
                    g.tipoClase = fields[3];
                    //Si hay un quinto campo, es el grupo_id
                    if (fields.length >= 5) {
                        try {
                            g.grupoId = Integer.parseInt(fields[4]);
                        } catch (NumberFormatException nfe) {
                            g.grupoId = 0;
                        }
                    }
                    p.cursosDetalle.add(g);
                }
            }
        }
        if (tipos == null) p.tipoClase = "SIN ASIGNAR";
        else if (tipos.contains("TEORIA") && tipos.contains("LABORATORIO")) p.tipoClase = "AMBOS";
        else p.tipoClase = tipos;

        return p;
    });
}

    public boolean actualizarProfesor(int idDocente, String apellidosNombres, String departamento, boolean esResponsableTeoria) {
        try {
            String sql = """
                UPDATE docentes 
                SET apellidos_nombres = ?, departamento = ?, es_responsable_teoria = ?
                WHERE id_docente = ?
            """;
            
            int rows = jdbcTemplate.update(sql, apellidosNombres, departamento, esResponsableTeoria ? 1 : 0, idDocente);
            
            if (rows > 0) {
                registrarActividad(obtenerIdUsuarioPorDocente(idDocente), "ACTUALIZAR_DOCENTE", "docentes", 
                    "Docente actualizado: " + apellidosNombres);
            }
            
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int asignarGrupoAProfesor(int grupoId, int idDocente) {
        try {
            String sql = "UPDATE horarios SET id_docente = ? WHERE grupo_id = ?";
            int rows = jdbcTemplate.update(sql, idDocente, grupoId);
            if (rows > 0) {
                registrarActividad(obtenerIdUsuarioPorDocente(idDocente), "ASIGNAR_GRUPO", "grupos_curso/horarios", "Grupo " + grupoId + " asignado al docente " + idDocente);
            }
            return rows;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

// 4. GESTIÓN DE ESTUDIANTES
public List<Estudiante> obtenerTodosEstudiantes() {
    String sql = """
        SELECT e.cui, e.id_usuario, e.apellidos_nombres, u.correo_institucional, 
               e.numero_matricula, e.estado_estudiante,
               COUNT(DISTINCT m.id_matricula) as cursos_matriculados
        FROM estudiantes e
        INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
        LEFT JOIN matriculas m ON e.cui = m.cui AND m.estado_matricula = 'ACTIVO'
        WHERE u.estado_cuenta = 'ACTIVO'
        GROUP BY e.cui, e.id_usuario, e.apellidos_nombres, u.correo_institucional, 
                 e.numero_matricula, e.estado_estudiante
        ORDER BY e.apellidos_nombres
    """;
    
    List<Estudiante> estudiantes = jdbcTemplate.query(sql, (rs, rowNum) -> {
        Estudiante est = new Estudiante();
        est.cui = rs.getString("cui");
        est.idUsuario = rs.getInt("id_usuario");
        est.apellidosNombres = rs.getString("apellidos_nombres");
        est.correoInstitucional = rs.getString("correo_institucional");
        est.numeroMatricula = rs.getInt("numero_matricula");
        est.estadoEstudiante = rs.getString("estado_estudiante");
        est.cursosMatriculados = rs.getInt("cursos_matriculados");
        est.cursosDetalle = new ArrayList<>();               
        est.laboratoriosMatriculados = new ArrayList<>();    
        est.laboratoriosDisponibles = new ArrayList<>();     
        return est;
    });

 
    for (Estudiante e : estudiantes) {
        e.cursosDetalle = obtenerCursosEstudiante(e.cui);
        e.laboratoriosMatriculados = obtenerLaboratoriosMatriculados(e.cui);
        e.laboratoriosDisponibles = obtenerLaboratoriosDisponibles(e.cui);
    }

    return estudiantes;
} 

    public boolean actualizarEstudiante(String cui, String apellidosNombres, int numeroMatricula, String estado) {
        try {
            String sql = """
                UPDATE estudiantes 
                SET apellidos_nombres = ?, numero_matricula = ?, estado_estudiante = ?
                WHERE cui = ?
            """;
            
            int rows = jdbcTemplate.update(sql, apellidosNombres, numeroMatricula, estado, cui);
            
            if (rows > 0) {
                Integer idUsuario = jdbcTemplate.queryForObject(
                    "SELECT id_usuario FROM estudiantes WHERE cui = ?", Integer.class, cui
                );
                if (idUsuario != null) {
                    registrarActividad(idUsuario, "ACTUALIZAR_ESTUDIANTE", "estudiantes", 
                        "Estudiante actualizado: " + apellidosNombres);
                }
            }
            
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

public List<GrupoCurso> obtenerGruposDisponibles() {
    String sql = """
        SELECT gc.grupo_id, gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase
        FROM grupos_curso gc
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE gc.estado = 'ACTIVO'
        ORDER BY gc.codigo_curso, gc.tipo_clase, gc.letra_grupo
    """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        GrupoCurso g = new GrupoCurso();
        g.grupoId = rs.getInt("grupo_id");
        g.codigoCurso = rs.getString("codigo_curso");
        g.nombreCurso = rs.getString("nombre_curso");
        g.letraGrupo = rs.getString("letra_grupo");
        g.tipoClase = rs.getString("tipo_clase");
        return g;
    });
}

public List<CursoAsignado> obtenerLaboratoriosMatriculados(String cui) {
    String sql = """
        SELECT DISTINCT gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase, gc.grupo_id
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO' AND gc.tipo_clase = 'LABORATORIO'
        ORDER BY gc.codigo_curso, gc.letra_grupo
    """;

    return jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        CursoAsignado c = new CursoAsignado();
        c.codigoCurso = rs.getString("codigo_curso");
        c.nombreCurso = rs.getString("nombre_curso");
        c.letraGrupo = rs.getString("letra_grupo");
        c.tipoClase = rs.getString("tipo_clase");
        c.grupoId = rs.getInt("grupo_id");
        return c;
    });
}

public List<CursoAsignado> obtenerLaboratoriosDisponibles(String cui) {
    String sql = """
        SELECT DISTINCT gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase, gc.grupo_id
        FROM grupos_curso gc
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE gc.tipo_clase = 'LABORATORIO' 
        AND gc.estado = 'ACTIVO'
        AND gc.grupo_id NOT IN (
            SELECT grupo_id FROM matriculas WHERE cui = ? AND estado_matricula = 'ACTIVO'
        )
        ORDER BY gc.codigo_curso, gc.letra_grupo
    """;

    return jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        CursoAsignado c = new CursoAsignado();
        c.codigoCurso = rs.getString("codigo_curso");
        c.nombreCurso = rs.getString("nombre_curso");
        c.letraGrupo = rs.getString("letra_grupo");
        c.tipoClase = rs.getString("tipo_clase");
        c.grupoId = rs.getInt("grupo_id");
        return c;
    });
}

public boolean actualizarEstudiante(String cui, String apellidosNombres, String correo, 
                                   int numeroMatricula, String estado, String password) {
    try {
        String sqlEst = """
            UPDATE estudiantes 
            SET apellidos_nombres = ?, numero_matricula = ?, estado_estudiante = ?
            WHERE cui = ?
        """;
        jdbcTemplate.update(sqlEst, apellidosNombres, numeroMatricula, estado, cui);

        Integer idUsuario = jdbcTemplate.queryForObject(
            "SELECT id_usuario FROM estudiantes WHERE cui = ?", Integer.class, cui
        );

        if (idUsuario != null) {
            if (password != null && !password.isEmpty()) {
                jdbcTemplate.update(
                    "UPDATE usuarios SET correo_institucional = ?, password_hash = ? WHERE id_usuario = ?",
                    correo, password, idUsuario
                );
            } else {
                jdbcTemplate.update(
                    "UPDATE usuarios SET correo_institucional = ? WHERE id_usuario = ?",
                    correo, idUsuario
                );
            }
            registrarActividad(idUsuario, "ACTUALIZAR_ESTUDIANTE", "estudiantes", 
                "Estudiante actualizado: " + apellidosNombres);
        }
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

public boolean asignarCursoAEstudiante(String cui, int grupoId) {
    try {
        Integer numMatricula = jdbcTemplate.queryForObject(
            "SELECT numero_matricula FROM estudiantes WHERE cui = ?", Integer.class, cui
        );

        if (numMatricula == null) return false;

        String sql = """
            INSERT INTO matriculas (cui, grupo_id, numero_matricula, prioridad_matricula, estado_matricula)
            VALUES (?, ?, ?, 0, 'ACTIVO')
        """;
        jdbcTemplate.update(sql, cui, grupoId, numMatricula);

        Integer idUsuario = jdbcTemplate.queryForObject(
            "SELECT id_usuario FROM estudiantes WHERE cui = ?", Integer.class, cui
        );

        if (idUsuario != null) {
            registrarActividad(idUsuario, "ASIGNAR_CURSO", "matriculas", 
                "Grupo " + grupoId + " asignado al estudiante " + cui);
        }
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

public boolean desasignarCursoDeEstudiante(String cui, int grupoId) {
    try {
        String sql = "DELETE FROM matriculas WHERE cui = ? AND grupo_id = ?";
        int rows = jdbcTemplate.update(sql, cui, grupoId);

        if (rows > 0) {
            Integer idUsuario = jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM estudiantes WHERE cui = ?", Integer.class, cui
            );

            if (idUsuario != null) {
                registrarActividad(idUsuario, "DESASIGNAR_CURSO", "matriculas", 
                    "Grupo " + grupoId + " desasignado del estudiante " + cui);
            }
        }
        return rows > 0;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

// 5. GESTIÓN DE EDITAR CURSOS 
    public List<Curso> obtenerTodosCursos() {
        String sql = """
            SELECT codigo_curso, nombre_curso, tiene_laboratorio, 
                   numero_grupos_teoria, numero_grupos_laboratorio, estado
            FROM cursos
            ORDER BY codigo_curso
        """;
        
        List<Curso> cursos = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Curso c = new Curso();
            c.codigoCurso = rs.getString("codigo_curso");
            c.nombreCurso = rs.getString("nombre_curso");
            c.tieneLaboratorio = rs.getBoolean("tiene_laboratorio");
            c.numeroGruposTeoria = rs.getInt("numero_grupos_teoria");
            c.numeroGruposLaboratorio = rs.getInt("numero_grupos_laboratorio");
            c.estado = rs.getString("estado");
            c.grupos = new ArrayList<>();
            return c;
        });
        
        for (Curso curso : cursos) {
            curso.grupos = obtenerGruposCurso(curso.codigoCurso);
        }
        
        return cursos;
    }

    public List<GrupoCurso> obtenerGruposCurso(String codigoCurso) {
        String sql = """
            SELECT gc.grupo_id, gc.letra_grupo, gc.tipo_clase, gc.capacidad_maxima, gc.estado,
                   h.id_docente, d.apellidos_nombres as nombre_docente
            FROM grupos_curso gc
            LEFT JOIN horarios h ON gc.grupo_id = h.grupo_id
            LEFT JOIN docentes d ON h.id_docente = d.id_docente
            WHERE gc.codigo_curso = ?
            GROUP BY gc.grupo_id, gc.letra_grupo, gc.tipo_clase, gc.capacidad_maxima, gc.estado, h.id_docente, d.apellidos_nombres
            ORDER BY gc.tipo_clase, gc.letra_grupo
        """;
        
        return jdbcTemplate.query(sql, new Object[]{codigoCurso}, (rs, rowNum) -> {
            GrupoCurso g = new GrupoCurso();
            g.grupoId = rs.getInt("grupo_id");
            g.letraGrupo = rs.getString("letra_grupo");
            g.tipoClase = rs.getString("tipo_clase");
            g.capacidadMaxima = rs.getInt("capacidad_maxima");
            g.estado = rs.getString("estado");
            g.idDocente = (Integer) rs.getObject("id_docente");
            g.nombreDocente = rs.getString("nombre_docente");
            return g;
        });
    }

    public boolean crearCurso(String codigoCurso, String nombreCurso, boolean tieneLaboratorio, 
                             int numeroGruposTeoria, int numeroGruposLaboratorio) {
        try {
            String sql = """
                INSERT INTO cursos (codigo_curso, nombre_curso, tiene_laboratorio, 
                                   numero_grupos_teoria, numero_grupos_laboratorio, estado)
                VALUES (?, ?, ?, ?, ?, 'ACTIVO')
            """;
            
            jdbcTemplate.update(sql, codigoCurso, nombreCurso, tieneLaboratorio ? 1 : 0, 
                              numeroGruposTeoria, numeroGruposLaboratorio);
            
            registrarActividadSistema("CREAR_CURSO", "cursos", "Curso creado: " + codigoCurso);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean crearGrupoCurso(String codigoCurso, int idCiclo, String letraGrupo, 
                                  String tipoClase, int capacidadMaxima) {
        try {
            String sql = """
                INSERT INTO grupos_curso (codigo_curso, id_ciclo, letra_grupo, tipo_clase, capacidad_maxima, estado)
                VALUES (?, ?, ?, ?, ?, 'ACTIVO')
            """;
            
            jdbcTemplate.update(sql, codigoCurso, idCiclo, letraGrupo, tipoClase, capacidadMaxima);
            
            registrarActividadSistema("CREAR_GRUPO", "grupos_curso", 
                "Grupo creado: " + codigoCurso + " - " + letraGrupo + " (" + tipoClase + ")");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

public List<CursoAsignado> obtenerCursosEstudiante(String cui) {
    String sql = """
        SELECT DISTINCT gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase, gc.grupo_id
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO'
        ORDER BY gc.tipo_clase, gc.codigo_curso, gc.letra_grupo
    """;

    return jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        CursoAsignado c = new CursoAsignado();
        c.codigoCurso = rs.getString("codigo_curso");
        c.nombreCurso = rs.getString("nombre_curso");
        c.letraGrupo = rs.getString("letra_grupo");
        c.tipoClase = rs.getString("tipo_clase");
        c.grupoId = rs.getInt("grupo_id");
        return c;
    });
}

// 6. En otro archivo...

// 7. GESTIÓN DE HORARIOS 
    public List<Horario> obtenerTodosHorarios() {
        String sql = """
            SELECT h.id_horario, h.grupo_id, gc.codigo_curso, c.nombre_curso, gc.letra_grupo,
                gc.tipo_clase, h.numero_salon, h.dia_semana, h.hora_inicio, h.hora_fin,
                h.id_docente, d.apellidos_nombres as nombre_docente, h.estado, 
                NULL as motivo, NULL as descripcion
            FROM horarios h
            INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
            INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
            INNER JOIN docentes d ON h.id_docente = d.id_docente
            WHERE h.id_docente = ?

            UNION ALL
    
            SELECT r.id_reserva as id_horario, 0 as grupo_id, 'RESERVA' as codigo_curso, 
                CONCAT('Reserva: ', COALESCE(r.motivo, 'Sin motivo')) as nombre_curso, 
                '' as letra_grupo, 'RESERVA' as tipo_clase, r.numero_salon, r.dia_semana, 
                r.hora_inicio, r.hora_fin, r.id_docente,
                d.apellidos_nombres as nombre_docente, r.estado_reserva as estado,
                r.motivo, r.descripcion
            FROM reservas_salon r
            INNER JOIN docentes d ON r.id_docente = d.id_docente
            WHERE r.id_docente = ? AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        
            ORDER BY dia_semana, hora_inicio
        """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Horario h = new Horario();
            h.idHorario = rs.getInt("id_horario");
            h.grupoId = rs.getInt("grupo_id");
            h.codigoCurso = rs.getString("codigo_curso");
            h.nombreCurso = rs.getString("nombre_curso");
            h.letraGrupo = rs.getString("letra_grupo");
            h.tipoClase = rs.getString("tipo_clase");
            h.numeroSalon = rs.getString("numero_salon");
            h.diaSemana = rs.getString("dia_semana");
            h.horaInicio = rs.getString("hora_inicio");
            h.horaFin = rs.getString("hora_fin");
            h.idDocente = (Integer) rs.getObject("id_docente");
            h.nombreDocente = rs.getString("nombre_docente");
            h.estado = rs.getString("estado");
            h.motivo = rs.getString("motivo");
            h.descripcion = rs.getString("descripcion");
            return h;
        });
    }

public List<Horario> obtenerHorariosPorDocente(int idDocente) {
    String sql = """
        SELECT h.id_horario, h.grupo_id, gc.codigo_curso, c.nombre_curso, gc.letra_grupo,
            gc.tipo_clase, h.numero_salon, h.dia_semana, h.hora_inicio, h.hora_fin,
            h.id_docente, d.apellidos_nombres as nombre_docente, h.estado, 
            NULL as motivo, NULL as descripcion
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        INNER JOIN docentes d ON h.id_docente = d.id_docente
        WHERE h.id_docente = ?

        UNION ALL
    
        SELECT r.id_reserva as id_horario, 0 as grupo_id, 'RESERVA' as codigo_curso, 
            CONCAT('Reserva: ', COALESCE(r.motivo, 'Sin motivo')) as nombre_curso, 
            '' as letra_grupo, 'RESERVA' as tipo_clase, r.numero_salon, r.dia_semana, 
            r.hora_inicio, r.hora_fin, r.id_docente,
            d.apellidos_nombres as nombre_docente, r.estado_reserva as estado,
            r.motivo, r.descripcion
        FROM reservas_salon r
        INNER JOIN docentes d ON r.id_docente = d.id_docente
        WHERE r.id_docente = ? AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        
        ORDER BY dia_semana, hora_inicio
    """;
    
    return jdbcTemplate.query(sql, new Object[]{idDocente, idDocente}, (rs, rowNum) -> {
        Horario h = new Horario();
        h.idHorario = rs.getInt("id_horario");
        h.grupoId = rs.getInt("grupo_id");
        h.codigoCurso = rs.getString("codigo_curso");
        h.nombreCurso = rs.getString("nombre_curso");
        h.letraGrupo = rs.getString("letra_grupo");
        h.tipoClase = rs.getString("tipo_clase");
        h.numeroSalon = rs.getString("numero_salon");
        h.diaSemana = rs.getString("dia_semana");
        h.horaInicio = rs.getString("hora_inicio");
        h.horaFin = rs.getString("hora_fin");
        h.idDocente = (Integer) rs.getObject("id_docente");
        h.nombreDocente = rs.getString("nombre_docente");
        h.estado = rs.getString("estado");
        h.motivo = rs.getString("motivo");
        h.descripcion = rs.getString("descripcion");
        return h;
    });
}

public boolean crearHorario(int grupoId, String numeroSalon, String diaSemana, 
                           String horaInicio, String horaFin, Integer idDocente) {
    try {
        //Verificar conflictos antes de crear
        if (verificarConflictoHorario(numeroSalon, diaSemana, horaInicio, horaFin, null)) {
            System.out.println("❌ Conflicto detectado al crear horario");
            return false;
        }
        
        String sql = """
            INSERT INTO horarios (grupo_id, numero_salon, dia_semana, hora_inicio, hora_fin, id_docente, estado)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
        """;
        
        jdbcTemplate.update(sql, grupoId, numeroSalon, diaSemana, horaInicio, horaFin, idDocente);
        
        if (idDocente != null) {
            registrarActividad(obtenerIdUsuarioPorDocente(idDocente), "CREAR_HORARIO", "horarios", 
                "Horario creado: " + diaSemana + " " + horaInicio + "-" + horaFin + " en " + numeroSalon);
        } else {
            registrarActividadSistema("CREAR_HORARIO", "horarios", 
                "Horario creado sin docente: " + diaSemana + " " + horaInicio + "-" + horaFin + " en " + numeroSalon);
        }
        
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

public boolean actualizarHorario(int idHorario, String numeroSalon, String diaSemana, 
                                String horaInicio, String horaFin, Integer idDocente) {
    try {
        //Verificar conflictos antes de actualizar
        if (verificarConflictoHorario(numeroSalon, diaSemana, horaInicio, horaFin, idHorario)) {
            System.out.println("❌ Conflicto detectado al actualizar horario " + idHorario);
            return false;
        }
        
        String sql = """
            UPDATE horarios 
            SET numero_salon = ?, dia_semana = ?, hora_inicio = ?, hora_fin = ?
        """;
        
        //Si se proporciona docente, lo actualiza también
        if (idDocente != null) {
            sql += ", id_docente = ?";
        }
        
        sql += " WHERE id_horario = ?";
        
        int rows;
        if (idDocente != null) {
            rows = jdbcTemplate.update(sql, numeroSalon, diaSemana, horaInicio, horaFin, idDocente, idHorario);
            registrarActividad(obtenerIdUsuarioPorDocente(idDocente), "ACTUALIZAR_HORARIO", "horarios", 
                "Horario actualizado: ID " + idHorario);
        } else {
            rows = jdbcTemplate.update(sql, numeroSalon, diaSemana, horaInicio, horaFin, idHorario);
            registrarActividadSistema("ACTUALIZAR_HORARIO", "horarios", "Horario actualizado: ID " + idHorario);
        }
        
        return rows > 0;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    public boolean eliminarHorario(int idHorario) {
        try {
            String sql = "DELETE FROM horarios WHERE id_horario = ?";
            int rows = jdbcTemplate.update(sql, idHorario);
            
            if (rows > 0) {
                registrarActividadSistema("ELIMINAR_HORARIO", "horarios", "Horario eliminado: ID " + idHorario);
            }
            
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminarReserva(int idReserva) {
        try {
            String sql = "UPDATE reservas_salon SET estado_reserva = 'CANCELADA' WHERE id_reserva = ?";
            int rows = jdbcTemplate.update(sql, idReserva);
            
            if (rows > 0) {
                registrarActividadSistema("CANCELAR_RESERVA", "reservas_salon", "Reserva cancelada: ID " + idReserva);
            }
            
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


//Obtiene conteo de cursos activos
public int contarCursosActivos() {
    String sql = "SELECT COUNT(*) FROM cursos WHERE estado = 'ACTIVO'";
    return jdbcTemplate.queryForObject(sql, Integer.class);
}

//Obtiene conteo de aulas disponibles
public int contarAulasDisponibles() {
    String sql = "SELECT COUNT(*) FROM salones WHERE estado IN ('DISPONIBLE', 'ACTIVO', 'HABILITADO')";
    return jdbcTemplate.queryForObject(sql, Integer.class);
}

//Obtiene lista simplificada de todos los docentes
public List<Map<String, Object>> obtenerListaDocentes() {
    String sql = """
        SELECT d.id_docente, d.apellidos_nombres, u.correo_institucional
        FROM docentes d
        INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
        WHERE u.estado_cuenta = 'ACTIVO'
        ORDER BY d.apellidos_nombres
    """;
    
    return jdbcTemplate.queryForList(sql);
}

//Obtiene lista simplificada de todos los estudiantes
public List<Map<String, Object>> obtenerListaEstudiantes() {
    String sql = """
        SELECT e.cui, e.apellidos_nombres, u.correo_institucional, e.numero_matricula
        FROM estudiantes e
        INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
        WHERE u.estado_cuenta = 'ACTIVO' AND e.estado_estudiante = 'VIGENTE'
        ORDER BY e.apellidos_nombres
    """;
    
    return jdbcTemplate.queryForList(sql);
}

//Obtiene lista de todos los salones
public List<Map<String, Object>> obtenerListaSalones() {
    String sql = """
        SELECT s.numero_salon, s.capacidad, ta.nombre_tipo
        FROM salones s
        INNER JOIN tipos_aula ta ON s.tipo_aula_id = ta.tipo_aula_id
        WHERE s.estado IN ('DISPONIBLE', 'ACTIVO', 'HABILITADO')
        ORDER BY s.numero_salon
    """;
    
    return jdbcTemplate.queryForList(sql);
}

//Obtiene horarios de un estudiante por CUI
public List<Horario> obtenerHorariosPorEstudiante(String cui) {
    String sql = """
        SELECT DISTINCT h.id_horario, h.grupo_id, gc.codigo_curso, c.nombre_curso, 
               gc.letra_grupo, gc.tipo_clase, h.numero_salon, h.dia_semana, 
               h.hora_inicio, h.hora_fin, h.id_docente, d.apellidos_nombres as nombre_docente, 
               h.estado, NULL as motivo, NULL as descripcion
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN horarios h ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        LEFT JOIN docentes d ON h.id_docente = d.id_docente
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO'
        ORDER BY h.dia_semana, h.hora_inicio
    """;
    
    return jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        Horario h = new Horario();
        h.idHorario = rs.getInt("id_horario");
        h.grupoId = rs.getInt("grupo_id");
        h.codigoCurso = rs.getString("codigo_curso");
        h.nombreCurso = rs.getString("nombre_curso");
        h.letraGrupo = rs.getString("letra_grupo");
        h.tipoClase = rs.getString("tipo_clase");
        h.numeroSalon = rs.getString("numero_salon");
        h.diaSemana = rs.getString("dia_semana");
        h.horaInicio = rs.getString("hora_inicio");
        h.horaFin = rs.getString("hora_fin");
        h.idDocente = (Integer) rs.getObject("id_docente");
        h.nombreDocente = rs.getString("nombre_docente");
        h.estado = rs.getString("estado");
        h.motivo = rs.getString("motivo");
        h.descripcion = rs.getString("descripcion");
        return h;
    });
}

//Obtiene horarios de un salón específico
public List<Horario> obtenerHorariosPorSalon(String numeroSalon) {
    String sql = """
        SELECT h.id_horario, h.grupo_id, gc.codigo_curso, c.nombre_curso, 
               gc.letra_grupo, gc.tipo_clase, h.numero_salon, h.dia_semana, 
               h.hora_inicio, h.hora_fin, h.id_docente, d.apellidos_nombres as nombre_docente, 
               h.estado, NULL as motivo, NULL as descripcion
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        LEFT JOIN docentes d ON h.id_docente = d.id_docente
        WHERE h.numero_salon = ? AND h.estado = 'ACTIVO'
        
        UNION ALL
        
        SELECT r.id_reserva as id_horario, 0 as grupo_id, 'RESERVA' as codigo_curso,
               CONCAT('Reserva: ', COALESCE(r.motivo, 'Sin motivo')) as nombre_curso, 
               '' as letra_grupo, 'RESERVA' as tipo_clase, r.numero_salon, r.dia_semana, 
               r.hora_inicio, r.hora_fin, r.id_docente, d.apellidos_nombres as nombre_docente,
               r.estado_reserva as estado, r.motivo, r.descripcion
        FROM reservas_salon r
        INNER JOIN docentes d ON r.id_docente = d.id_docente
        WHERE r.numero_salon = ? AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        
        ORDER BY dia_semana, hora_inicio
    """;
    
    return jdbcTemplate.query(sql, new Object[]{numeroSalon, numeroSalon}, (rs, rowNum) -> {
        Horario h = new Horario();
        h.idHorario = rs.getInt("id_horario");
        h.grupoId = rs.getInt("grupo_id");
        h.codigoCurso = rs.getString("codigo_curso");
        h.nombreCurso = rs.getString("nombre_curso");
        h.letraGrupo = rs.getString("letra_grupo");
        h.tipoClase = rs.getString("tipo_clase");
        h.numeroSalon = rs.getString("numero_salon");
        h.diaSemana = rs.getString("dia_semana");
        h.horaInicio = rs.getString("hora_inicio");
        h.horaFin = rs.getString("hora_fin");
        h.idDocente = (Integer) rs.getObject("id_docente");
        h.nombreDocente = rs.getString("nombre_docente");
        h.estado = rs.getString("estado");
        h.motivo = rs.getString("motivo");
        h.descripcion = rs.getString("descripcion");
        return h;
    });
}

//Verifica conflictos de horario antes de crear/actualizar
public boolean verificarConflictoHorario(String numeroSalon, String diaSemana, 
                                         String horaInicio, String horaFin, 
                                         Integer idHorarioExcluir) {
    String sql = """
        SELECT COUNT(*) FROM horarios 
        WHERE numero_salon = ? 
        AND dia_semana = ? 
        AND estado = 'ACTIVO'
        AND hora_inicio < ? 
        AND hora_fin > ?
        AND (? IS NULL OR id_horario != ?)
    """;
    
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
        numeroSalon, diaSemana, horaFin, horaInicio, idHorarioExcluir, idHorarioExcluir);
    
    //También verificar reservas
    String sqlReservas = """
        SELECT COUNT(*) FROM reservas_salon
        WHERE numero_salon = ?
        AND dia_semana = ?
        AND estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        AND hora_inicio < ?
        AND hora_fin > ?
    """;
    
    Integer countReservas = jdbcTemplate.queryForObject(sqlReservas, Integer.class,
        numeroSalon, diaSemana, horaFin, horaInicio);
    
    return (count != null && count > 0) || (countReservas != null && countReservas > 0);
}

//Obtiene información completa de un horario para edición
public Horario obtenerHorarioPorId(int idHorario) {
    String sql = """
        SELECT h.id_horario, h.grupo_id, gc.codigo_curso, c.nombre_curso, 
               gc.letra_grupo, gc.tipo_clase, h.numero_salon, h.dia_semana, 
               h.hora_inicio, h.hora_fin, h.id_docente, d.apellidos_nombres as nombre_docente, 
               h.estado, NULL as motivo, NULL as descripcion
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        LEFT JOIN docentes d ON h.id_docente = d.id_docente
        WHERE h.id_horario = ?
    """;
    
    List<Horario> lista = jdbcTemplate.query(sql, new Object[]{idHorario}, (rs, rowNum) -> {
        Horario h = new Horario();
        h.idHorario = rs.getInt("id_horario");
        h.grupoId = rs.getInt("grupo_id");
        h.codigoCurso = rs.getString("codigo_curso");
        h.nombreCurso = rs.getString("nombre_curso");
        h.letraGrupo = rs.getString("letra_grupo");
        h.tipoClase = rs.getString("tipo_clase");
        h.numeroSalon = rs.getString("numero_salon");
        h.diaSemana = rs.getString("dia_semana");
        h.horaInicio = rs.getString("hora_inicio");
        h.horaFin = rs.getString("hora_fin");
        h.idDocente = (Integer) rs.getObject("id_docente");
        h.nombreDocente = rs.getString("nombre_docente");
        h.estado = rs.getString("estado");
        h.motivo = rs.getString("motivo");
        h.descripcion = rs.getString("descripcion");
        return h;
    });
    
    return lista.isEmpty() ? null : lista.get(0);
}

// 8. REPORTES
//Obtiene reporte de estudiantes matriculados
public List<Map<String, Object>> obtenerReporteEstudiantesMatriculados() {
    String sql = """
        SELECT 
            e.cui,
            e.apellidos_nombres,
            u.correo_institucional,
            COUNT(DISTINCT m.id_matricula) as cursos_matriculados
        FROM estudiantes e
        INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
        LEFT JOIN matriculas m ON e.cui = m.cui AND m.estado_matricula = 'ACTIVO'
        WHERE u.estado_cuenta = 'ACTIVO'
        GROUP BY e.cui, e.apellidos_nombres, u.correo_institucional
        ORDER BY e.apellidos_nombres
    """;
    
    List<Map<String, Object>> estudiantes = jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> est = new HashMap<>();
        est.put("cui", rs.getString("cui"));
        est.put("apellidosNombres", rs.getString("apellidos_nombres"));
        est.put("correoInstitucional", rs.getString("correo_institucional"));
        est.put("cursosMatriculados", rs.getInt("cursos_matriculados"));
        return est;
    });
    //Agregar detalle de cursos
    for (Map<String, Object> est : estudiantes) {
        String cui = (String) est.get("cui");
        est.put("cursosDetalle", obtenerCursosDetalleEstudiante(cui));
    }

    return estudiantes;
}

//Obtiene detalle de cursos de un estudiante
private List<Map<String, Object>> obtenerCursosDetalleEstudiante(String cui) {
    String sql = """
        SELECT 
            gc.codigo_curso,
            c.nombre_curso,
            gc.letra_grupo,
            gc.tipo_clase
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO'
        ORDER BY gc.tipo_clase, gc.codigo_curso
    """;
    
    return jdbcTemplate.queryForList(sql, cui);
}

//Obtiene reporte completo de un curso específico
public Map<String, Object> obtenerReporteCurso(String codigoCurso) {
    Map<String, Object> resultado = new HashMap<>();
    
    //Información básica del curso
    String sqlCurso = """
        SELECT codigo_curso, nombre_curso
        FROM cursos
        WHERE codigo_curso = ?
    """;
    
    Map<String, Object> curso = jdbcTemplate.queryForMap(sqlCurso, codigoCurso);
    resultado.put("codigoCurso", curso.get("codigo_curso"));
    resultado.put("nombreCurso", curso.get("nombre_curso"));
    
    //Docentes del curso
    String sqlDocentes = """
        SELECT DISTINCT
            d.apellidos_nombres as nombreDocente,
            gc.letra_grupo,
            gc.tipo_clase
        FROM horarios h
        INNER JOIN docentes d ON h.id_docente = d.id_docente
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN ciclos_academicos ca ON gc.id_ciclo = ca.id_ciclo
        WHERE gc.codigo_curso = ?
          AND ca.estado = 'ACTIVO'
          AND h.estado = 'ACTIVO'
        ORDER BY gc.tipo_clase, gc.letra_grupo
    """;
    
    resultado.put("docentes", jdbcTemplate.queryForList(sqlDocentes, codigoCurso));
    
    //Obtener porcentajes del curso
    String sqlPorcentajes = """
        SELECT tipo_eval_id, porcentaje
        FROM porcentajes_evaluacion pe
        INNER JOIN ciclos_academicos ca ON pe.id_ciclo = ca.id_ciclo
        WHERE pe.codigo_curso = ? AND ca.estado = 'ACTIVO'
    """;
    
    List<Map<String, Object>> porcentajesRows = jdbcTemplate.queryForList(sqlPorcentajes, codigoCurso);
    Map<Integer, Double> porcentajes = new HashMap<>();
    for (Map<String, Object> row : porcentajesRows) {
        int tipoEvalId = ((Number) row.get("tipo_eval_id")).intValue();
        double porcentaje = ((Number) row.get("porcentaje")).doubleValue() / 100.0;
        porcentajes.put(tipoEvalId, porcentaje);
    }
    
    //Estudiantes con notas
    String sqlEstudiantes = """
        SELECT 
            e.cui,
            e.apellidos_nombres as nombreEstudiante,
            u.correo_institucional,
            gc.letra_grupo,
            m.id_matricula
        FROM matriculas m
        INNER JOIN estudiantes e ON m.cui = e.cui
        INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN ciclos_academicos ca ON gc.id_ciclo = ca.id_ciclo
        WHERE gc.codigo_curso = ?
          AND gc.tipo_clase = 'TEORIA'
          AND ca.estado = 'ACTIVO'
          AND m.estado_matricula = 'ACTIVO'
        ORDER BY e.apellidos_nombres
    """;
    
    List<Map<String, Object>> estudiantes = jdbcTemplate.query(sqlEstudiantes, new Object[]{codigoCurso}, (rs, rowNum) -> {
        Map<String, Object> est = new HashMap<>();
        est.put("cui", rs.getString("cui"));
        est.put("nombreEstudiante", rs.getString("nombreEstudiante"));
        est.put("correoInstitucional", rs.getString("correo_institucional"));
        est.put("letraGrupo", rs.getString("letra_grupo"));
        est.put("idMatricula", rs.getInt("id_matricula"));
        return est;
    });
    
    //Agregar notas a cada estudiante y calcular promedio PONDERADO
    for (Map<String, Object> est : estudiantes) {
        int idMatricula = (int) est.get("idMatricula");
        Map<String, Double> notas = obtenerNotasEstudiante(idMatricula);
        est.put("notas", notas);
        
        //CALCULAR PROMEDIO PONDERADO
        double sumaPonderada = 0.0;
        for (Map.Entry<String, Double> entry : notas.entrySet()) {
            if (entry.getValue() != null) {
                int tipoEvalId = Integer.parseInt(entry.getKey());
                Double porcentaje = porcentajes.get(tipoEvalId);
                if (porcentaje != null) {
                    sumaPonderada += entry.getValue() * porcentaje;
                }
            }
        }
        est.put("promedio", sumaPonderada);
    }
    
    resultado.put("estudiantes", estudiantes);
    
    return resultado;
}

//Obtiene notas de un estudiante
private Map<String, Double> obtenerNotasEstudiante(int idMatricula) {
    String sql = """
        SELECT tipo_eval_id, calificacion
        FROM notas
        WHERE id_matricula = ?
    """;
    
    List<Map<String, Object>> notasList = jdbcTemplate.queryForList(sql, idMatricula);
    Map<String, Double> notas = new HashMap<>();
    
    for (Map<String, Object> nota : notasList) {
        String tipoEvalId = String.valueOf(nota.get("tipo_eval_id"));
        Double calificacion = nota.get("calificacion") != null ? 
            ((Number) nota.get("calificacion")).doubleValue() : null;
        notas.put(tipoEvalId, calificacion);
    }
    
    return notas;
}

//Obtiene reporte general de todos los docentes
public List<Map<String, Object>> obtenerReporteDocentes() {
    String sql = """
        SELECT 
            d.id_docente,
            d.id_usuario,
            d.apellidos_nombres,
            d.departamento
        FROM docentes d
        INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
        WHERE u.estado_cuenta = 'ACTIVO'
        ORDER BY d.apellidos_nombres
    """;
    
    List<Map<String, Object>> docentes = jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> doc = new HashMap<>();
        doc.put("idDocente", rs.getInt("id_docente"));
        doc.put("idUsuario", rs.getInt("id_usuario"));
        doc.put("apellidosNombres", rs.getString("apellidos_nombres"));
        doc.put("departamento", rs.getString("departamento"));
        return doc;
    });
    
    //Agregar información de cursos para cada docente
    for (Map<String, Object> doc : docentes) {
        int idDocente = (int) doc.get("idDocente");
        doc.put("cursos", obtenerCursosDocente(idDocente));
        doc.put("totalCursos", ((List<?>) doc.get("cursos")).size());
    }
    
    return docentes;
}

//Obtiene cursos de un docente con estadísticas
private List<Map<String, Object>> obtenerCursosDocente(int idDocente) {
    String sql = """
        SELECT DISTINCT
            gc.grupo_id,
            gc.codigo_curso,
            c.nombre_curso,
            gc.letra_grupo,
            gc.tipo_clase
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        INNER JOIN ciclos_academicos ca ON gc.id_ciclo = ca.id_ciclo
        WHERE h.id_docente = ?
          AND ca.estado = 'ACTIVO'
          AND h.estado = 'ACTIVO'
        ORDER BY gc.codigo_curso, gc.tipo_clase, gc.letra_grupo
    """;
    
    List<Map<String, Object>> cursos = jdbcTemplate.query(sql, new Object[]{idDocente}, (rs, rowNum) -> {
        Map<String, Object> curso = new HashMap<>();
        curso.put("grupoId", rs.getInt("grupo_id"));
        curso.put("codigoCurso", rs.getString("codigo_curso"));
        curso.put("nombreCurso", rs.getString("nombre_curso"));
        curso.put("letraGrupo", rs.getString("letra_grupo"));
        curso.put("tipoClase", rs.getString("tipo_clase"));
        return curso;
    });
    
    //Agregar estadísticas a cada curso
    for (Map<String, Object> curso : cursos) {
        int grupoId = (int) curso.get("grupoId");
        
        //Porcentaje de asistencia
        curso.put("porcentajeAsistencia", calcularPorcentajeAsistencia(grupoId));
        
        //Aprobados y desaprobados
        Map<String, Integer> stats = calcularAprobadosDesaprobados(grupoId);
        curso.put("aprobados", stats.get("aprobados"));
        curso.put("desaprobados", stats.get("desaprobados"));
    }
    
    return cursos;
}

//Calcula porcentaje de asistencia de un grupo
private double calcularPorcentajeAsistencia(int grupoId) {
    try {
        String sql = """
            SELECT 
                COUNT(CASE WHEN ae.estado_asistencia = 'PRESENTE' THEN 1 END) as presentes,
                COUNT(ae.id_asistencia) as total
            FROM asistencias_estudiante ae
            INNER JOIN horarios h ON ae.id_horario = h.id_horario
            WHERE h.grupo_id = ?
        """;
        
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, grupoId);
        int presentes = ((Number) result.get("presentes")).intValue();
        int total = ((Number) result.get("total")).intValue();
        
        return total > 0 ? (presentes * 100.0 / total) : 0.0;
        
    } catch (Exception e) {
        return 0.0;
    }
}

//Calcula aprobados y desaprobados de un grupo
private Map<String, Integer> calcularAprobadosDesaprobados(int grupoId) {
    Map<String, Integer> stats = new HashMap<>();
    stats.put("aprobados", 0);
    stats.put("desaprobados", 0);
    
    try {
        String sql = """
            SELECT 
                m.id_matricula,
                AVG(n.calificacion) as promedio
            FROM matriculas m
            LEFT JOIN notas n ON m.id_matricula = n.id_matricula
            WHERE m.grupo_id = ?
              AND m.estado_matricula = 'ACTIVO'
            GROUP BY m.id_matricula
        """;
        
        List<Map<String, Object>> promedios = jdbcTemplate.queryForList(sql, grupoId);
        
        for (Map<String, Object> p : promedios) {
            Object promedioObj = p.get("promedio");
            if (promedioObj != null) {
                double promedio = ((Number) promedioObj).doubleValue();
                if (promedio >= 10.5) {
                    stats.put("aprobados", stats.get("aprobados") + 1);
                } else {
                    stats.put("desaprobados", stats.get("desaprobados") + 1);
                }
            } else {
                stats.put("desaprobados", stats.get("desaprobados") + 1);
            }
        }
        
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    return stats;
}

// 9. VER  PDFS
//Obtiene todos los cursos activos (para selector)
public List<Map<String, Object>> obtenerCursosActivos() {
    String sql = """
        SELECT codigo_curso, nombre_curso
        FROM cursos
        WHERE estado = 'ACTIVO'
        ORDER BY nombre_curso
    """;

    return jdbcTemplate.queryForList(sql);
}

//Obtiene todos los tipos de evaluación
public List<Map<String, Object>> obtenerTiposEvaluacion() {
    String sql = """
        SELECT tipo_eval_id, codigo, nombre, tipo
        FROM tipos_evaluacion
        ORDER BY tipo_eval_id
    """;

    return jdbcTemplate.queryForList(sql);
}

//Obtiene todos los sílabos del sistema
public List<Map<String, Object>> obtenerTodosSilabos() {
    String sql = """
        SELECT 
            s.id_silabo,
            s.codigo_curso,
            c.nombre_curso,
            s.grupo_teoria,
            s.ruta_archivo,
            s.fecha_subida,
            s.estado,
            d.apellidos_nombres as nombre_docente
        FROM silabos s
        INNER JOIN cursos c ON s.codigo_curso = c.codigo_curso
        INNER JOIN docentes d ON s.id_docente = d.id_docente
        ORDER BY s.fecha_subida DESC
    """;
    
    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> silabo = new HashMap<>();
        silabo.put("idSilabo", rs.getInt("id_silabo"));
        silabo.put("codigoCurso", rs.getString("codigo_curso"));
        silabo.put("nombreCurso", rs.getString("nombre_curso"));
        silabo.put("grupoTeoria", rs.getString("grupo_teoria"));
        
        String rutaArchivo = rs.getString("ruta_archivo");
        if (rutaArchivo != null) {
            rutaArchivo = rutaArchivo.replace("\\", "/");
        }
        silabo.put("rutaArchivo", rutaArchivo);
        
        silabo.put("fechaSubida", rs.getTimestamp("fecha_subida"));
        silabo.put("estado", rs.getString("estado"));
        silabo.put("nombreDocente", rs.getString("nombre_docente"));
        return silabo;
    });
}

//Obtiene todos los exámenes del sistema
public List<Map<String, Object>> obtenerTodosExamenes() {
    String sql = """
        SELECT 
            e.id_examen,
            e.grupo_id,
            gc.codigo_curso,
            c.nombre_curso,
            gc.letra_grupo,
            e.tipo_eval_id,
            te.nombre as tipo_evaluacion,
            e.tipo_nota,
            e.ruta_archivo,
            e.fecha_subida,
            d.apellidos_nombres as nombre_docente
        FROM examenes_pdf e
        INNER JOIN grupos_curso gc ON e.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        INNER JOIN tipos_evaluacion te ON e.tipo_eval_id = te.tipo_eval_id
        INNER JOIN docentes d ON e.subido_por = d.id_docente
        ORDER BY e.fecha_subida DESC
    """;
    
    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> examen = new HashMap<>();
        examen.put("idExamen", rs.getInt("id_examen"));
        examen.put("grupoId", rs.getInt("grupo_id"));
        examen.put("codigoCurso", rs.getString("codigo_curso"));
        examen.put("nombreCurso", rs.getString("nombre_curso"));
        examen.put("letraGrupo", rs.getString("letra_grupo"));
        examen.put("tipoEvalId", rs.getInt("tipo_eval_id"));
        examen.put("tipoEvaluacion", rs.getString("tipo_evaluacion"));
        examen.put("tipoNota", rs.getString("tipo_nota"));
        
        String rutaArchivo = rs.getString("ruta_archivo");
        if (rutaArchivo != null) {
            rutaArchivo = rutaArchivo.replace("\\", "/");
        }
        examen.put("rutaArchivo", rutaArchivo);
        
        examen.put("fechaSubida", rs.getTimestamp("fecha_subida"));
        examen.put("nombreDocente", rs.getString("nombre_docente"));
        return examen;
    });
}

// 0. MÉTODOS AUXILIARES (DE VARIAS SECCIONES)
    private String generarCUI() { 
        String prefix = "CUI";
        Integer maxNum = jdbcTemplate.queryForObject(
            "SELECT MAX(CAST(SUBSTRING(cui, 4) AS UNSIGNED)) FROM estudiantes WHERE cui LIKE 'CUI%'", 
            Integer.class
        );
        int next = (maxNum == null ? 0 : maxNum) + 1;
        return prefix + String.format("%07d", next);
    }

    private int generarNumeroMatricula() {
        Integer max = jdbcTemplate.queryForObject(
            "SELECT MAX(numero_matricula) FROM estudiantes", Integer.class
        );
        return (max == null ? 2025000 : max) + 1;
    }

    private int obtenerIdUsuarioPorDocente(int idDocente) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM docentes WHERE id_docente = ?", Integer.class, idDocente
            );
        } catch (Exception e) {
            return 1; //Fallback al admin
        }
    }

    private void registrarActividad(int idUsuario, String accion, String tabla, String descripcion) {
        try {
            String sql = """
                INSERT INTO log_actividades (id_usuario, accion, tabla_afectada, descripcion, ip_origen)
                VALUES (?, ?, ?, ?, '127.0.0.1')
            """;
            jdbcTemplate.update(sql, idUsuario, accion, tabla, descripcion);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registrarActividadSistema(String accion, String tabla, String descripcion) {
        registrarActividad(1, accion, tabla, descripcion); //ID 1 = admin sistema
    }

    private static String safeStr(Object obj) {
        return obj == null ? null : obj.toString();
    }
}