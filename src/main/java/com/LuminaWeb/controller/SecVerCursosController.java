package com.LuminaWeb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/sec/api/cursos")
@CrossOrigin(origins = "*")
public class SecVerCursosController {
    // 6. VER INFORMACIÓN DE LOS CURSOS
    @GetMapping("/listar")
    public ResponseEntity<?> listarCursos() {
        System.out.println("=== LISTAR CURSOS ===");
        
        try {
            List<Map<String, Object>> cursos = new ArrayList<>();
            
            String sql = "SELECT " +
                        "c.codigo_curso, " +
                        "c.nombre_curso, " +
                        "c.tiene_laboratorio, " +
                        "c.estado " +
                        "FROM cursos c " +
                        "WHERE c.estado = 'ACTIVO' " +
                        "ORDER BY c.codigo_curso";
            
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Map<String, Object> curso = new HashMap<>();
                    curso.put("codigo_curso", rs.getString("codigo_curso"));
                    curso.put("nombre_curso", rs.getString("nombre_curso"));
                    curso.put("tiene_laboratorio", rs.getInt("tiene_laboratorio"));
                    curso.put("estado", rs.getString("estado"));
                    
                    cursos.add(curso);
                }
            }
            
            return ResponseEntity.ok(cursos);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error en la base de datos"));
        }
    }
    
    @GetMapping("/{codigo}/info")
    public ResponseEntity<?> obtenerInfoCurso(@PathVariable("codigo") String codigoCurso) {
        System.out.println("=== INFO CURSO: " + codigoCurso + " ===");
        
        try {
            Map<String, Object> infoCurso = new HashMap<>();
            
            try (Connection conn = getConnection()) {
                //Información básica del curso
                String sql = "SELECT c.* FROM cursos c WHERE c.codigo_curso = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, codigoCurso);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        infoCurso.put("codigo_curso", rs.getString("codigo_curso"));
                        infoCurso.put("nombre_curso", rs.getString("nombre_curso"));
                        infoCurso.put("tiene_laboratorio", rs.getInt("tiene_laboratorio"));
                        infoCurso.put("estado", rs.getString("estado"));
                        
                        //Obtener grupos del curso
                        List<Map<String, Object>> grupos = obtenerGruposCurso(conn, codigoCurso);
                        infoCurso.put("grupos", grupos);
                        
                        //Obtener todos los horarios del curso
                        List<Map<String, Object>> horarios = obtenerTodosHorariosCurso(conn, codigoCurso);
                        infoCurso.put("horarios", horarios);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Collections.singletonMap("error", "Curso no encontrado"));
                    }
                }
            }
            
            return ResponseEntity.ok(infoCurso);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error en la base de datos"));
        }
    }
    
    private List<Map<String, Object>> obtenerGruposCurso(Connection conn, String codigoCurso) throws SQLException {
        List<Map<String, Object>> grupos = new ArrayList<>();
        
        String sql = "SELECT DISTINCT " +
                    "g.grupo_id, " +
                    "g.letra_grupo, " +
                    "g.tipo_clase, " +
                    "g.capacidad_maxima " +
                    "FROM grupos_curso g " +
                    "WHERE g.codigo_curso = ? " +
                    "AND g.estado = 'ACTIVO' " +
                    "ORDER BY g.tipo_clase, g.letra_grupo";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoCurso);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> grupo = new HashMap<>();
                grupo.put("grupo_id", rs.getInt("grupo_id"));
                grupo.put("letra_grupo", rs.getString("letra_grupo"));
                grupo.put("tipo_clase", rs.getString("tipo_clase"));
                grupo.put("capacidad_maxima", rs.getInt("capacidad_maxima"));
                
                grupos.add(grupo);
            }
        }
        
        return grupos;
    }
    
    private List<Map<String, Object>> obtenerTodosHorariosCurso(Connection conn, String codigoCurso) throws SQLException {
        List<Map<String, Object>> horarios = new ArrayList<>();
        
        String sql = "SELECT " +
                    "h.dia_semana, " +
                    "h.hora_inicio, " +
                    "h.hora_fin, " +
                    "h.numero_salon, " +
                    "g.letra_grupo, " +
                    "g.tipo_clase, " +
                    "d.apellidos_nombres as docente " +
                    "FROM horarios h " +
                    "INNER JOIN grupos_curso g ON h.grupo_id = g.grupo_id " +
                    "LEFT JOIN docentes d ON h.id_docente = d.id_docente " +
                    "WHERE g.codigo_curso = ? " +
                    "AND h.estado = 'ACTIVO' " +
                    "ORDER BY FIELD(h.dia_semana, 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'), " +
                    "h.hora_inicio";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoCurso);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> horario = new HashMap<>();
                horario.put("dia_semana", rs.getString("dia_semana"));
                horario.put("hora_inicio", rs.getString("hora_inicio"));
                horario.put("hora_fin", rs.getString("hora_fin"));
                horario.put("numero_salon", rs.getString("numero_salon"));
                horario.put("letra_grupo", rs.getString("letra_grupo"));
                horario.put("tipo_clase", rs.getString("tipo_clase"));
                horario.put("docente", rs.getString("docente"));
                
                horarios.add(horario);
            }
        }
        
        return horarios;
    }
    
    @GetMapping("/{codigo}/grupos")
    public ResponseEntity<?> obtenerGruposDetallados(@PathVariable("codigo") String codigoCurso) {
        System.out.println("=== GRUPOS DETALLADOS: " + codigoCurso + " ===");
        
        try {
            List<Map<String, Object>> grupos = new ArrayList<>();
            
            try (Connection conn = getConnection()) {
                //Verificar si el curso tiene laboratorio
                boolean tieneLab = false;
                String sqlCheckLab = "SELECT tiene_laboratorio FROM cursos WHERE codigo_curso = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlCheckLab)) {
                    stmt.setString(1, codigoCurso);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        tieneLab = rs.getInt("tiene_laboratorio") == 1;
                    }
                }
                
                //Obtener grupos de teoría
                String sqlTeoria = "SELECT " +
                                  "g.grupo_id, " +
                                  "g.letra_grupo, " +
                                  "g.tipo_clase, " +
                                  "g.capacidad_maxima as capacidad, " +
                                  "(SELECT COUNT(DISTINCT m.cui) " +
                                  " FROM matriculas m " +
                                  " WHERE m.grupo_id = g.grupo_id " +
                                  " AND m.estado_matricula = 'ACTIVO') as estudiantes_matriculados " +
                                  "FROM grupos_curso g " +
                                  "WHERE g.codigo_curso = ? " +
                                  "AND g.tipo_clase = 'TEORIA' " +
                                  "AND g.estado = 'ACTIVO' " +
                                  "ORDER BY g.letra_grupo";
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlTeoria)) {
                    stmt.setString(1, codigoCurso);
                    ResultSet rs = stmt.executeQuery();
                    
                    while (rs.next()) {
                        Map<String, Object> grupo = new HashMap<>();
                        int grupoId = rs.getInt("grupo_id");
                        
                        grupo.put("grupo_id", grupoId);
                        grupo.put("letra_grupo", rs.getString("letra_grupo"));
                        grupo.put("tipo_clase", rs.getString("tipo_clase"));
                        grupo.put("capacidad", rs.getInt("capacidad"));
                        grupo.put("estudiantes_matriculados", rs.getInt("estudiantes_matriculados"));
                        
                        //Obtener docente principal y horarios
                        Map<String, Object> infoGrupo = obtenerInfoGrupo(conn, grupoId);
                        grupo.put("docente", infoGrupo.get("docente"));
                        grupo.put("salon", infoGrupo.get("salon"));
                        grupo.put("horarios", infoGrupo.get("horarios"));
                        
                        grupos.add(grupo);
                    }
                }
                
                //Obtener grupos de laboratorio (si el curso tiene lab)
                if (tieneLab) {
                    String sqlLab = "SELECT " +
                                   "g.grupo_id, " +
                                   "g.letra_grupo, " +
                                   "g.tipo_clase, " +
                                   "g.capacidad_maxima as capacidad, " +
                                   "(SELECT COUNT(DISTINCT m.cui) " +
                                   " FROM matriculas m " +
                                   " WHERE m.grupo_id = g.grupo_id " +
                                   " AND m.estado_matricula = 'ACTIVO') as estudiantes_matriculados " +
                                   "FROM grupos_curso g " +
                                   "WHERE g.codigo_curso = ? " +
                                   "AND g.tipo_clase = 'LABORATORIO' " +
                                   "AND g.estado = 'ACTIVO' " +
                                   "ORDER BY g.letra_grupo";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sqlLab)) {
                        stmt.setString(1, codigoCurso);
                        ResultSet rs = stmt.executeQuery();
                        
                        while (rs.next()) {
                            Map<String, Object> grupo = new HashMap<>();
                            int grupoId = rs.getInt("grupo_id");
                            
                            grupo.put("grupo_id", grupoId);
                            grupo.put("letra_grupo", rs.getString("letra_grupo"));
                            grupo.put("tipo_clase", rs.getString("tipo_clase"));
                            grupo.put("capacidad", rs.getInt("capacidad"));
                            grupo.put("estudiantes_matriculados", rs.getInt("estudiantes_matriculados"));
                            
                            //Obtener docente y horarios
                            Map<String, Object> infoGrupo = obtenerInfoGrupo(conn, grupoId);
                            grupo.put("docente", infoGrupo.get("docente"));
                            grupo.put("salon", infoGrupo.get("salon"));
                            grupo.put("horarios", infoGrupo.get("horarios"));
                            
                            grupos.add(grupo);
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(grupos);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error en la base de datos"));
        }
    }
    
    private Map<String, Object> obtenerInfoGrupo(Connection conn, int grupoId) throws SQLException {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> horarios = new ArrayList<>();
        
        //Obtener horarios del grupo
        String sqlHorarios = "SELECT " +
                            "h.dia_semana, " +
                            "h.hora_inicio, " +
                            "h.hora_fin, " +
                            "h.numero_salon as salon, " +
                            "d.apellidos_nombres as docente " +
                            "FROM horarios h " +
                            "LEFT JOIN docentes d ON h.id_docente = d.id_docente " +
                            "WHERE h.grupo_id = ? " +
                            "AND h.estado = 'ACTIVO' " +
                            "ORDER BY FIELD(h.dia_semana, 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'), " +
                            "h.hora_inicio";
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlHorarios)) {
            stmt.setInt(1, grupoId);
            ResultSet rs = stmt.executeQuery();
            
            String primerDocente = null;
            String primerSalon = null;
            
            while (rs.next()) {
                Map<String, Object> horario = new HashMap<>();
                horario.put("dia_semana", rs.getString("dia_semana"));
                horario.put("hora_inicio", rs.getString("hora_inicio"));
                horario.put("hora_fin", rs.getString("hora_fin"));
                horario.put("salon", rs.getString("salon"));
                horario.put("docente", rs.getString("docente"));
                
                horarios.add(horario);
                
                //Tomar el primer docente y salón como referencia
                if (primerDocente == null && rs.getString("docente") != null) {
                    primerDocente = rs.getString("docente");
                }
                if (primerSalon == null && rs.getString("salon") != null) {
                    primerSalon = rs.getString("salon");
                }
            }
            
            info.put("docente", primerDocente);
            info.put("salon", primerSalon);
            info.put("horarios", horarios);
        }
        
        return info;
    }
    
    @GetMapping("/grupo/{grupoId}/estudiantes")
    public ResponseEntity<?> obtenerEstudiantesGrupo(@PathVariable("grupoId") int grupoId) {
        System.out.println("=== ESTUDIANTES GRUPO: " + grupoId + " ===");
        
        try {
            List<Map<String, Object>> estudiantes = new ArrayList<>();
            
            try (Connection conn = getConnection()) {
                //Obtener información del grupo
                String sqlInfo = "SELECT g.tipo_clase, g.codigo_curso FROM grupos_curso g WHERE g.grupo_id = ?";
                String tipoClase = "";
                String codigoCurso = "";
                
                try (PreparedStatement stmt = conn.prepareStatement(sqlInfo)) {
                    stmt.setInt(1, grupoId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        tipoClase = rs.getString("tipo_clase");
                        codigoCurso = rs.getString("codigo_curso");
                    }
                }
                
                //Verificar si el curso tiene laboratorio
                boolean tieneLab = false;
                if (tipoClase.equals("TEORIA")) {
                    String sqlCheckLab = "SELECT tiene_laboratorio FROM cursos WHERE codigo_curso = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlCheckLab)) {
                        stmt.setString(1, codigoCurso);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            tieneLab = rs.getInt("tiene_laboratorio") == 1;
                        }
                    }
                }
                
                //Obtener estudiantes del grupo
                String sql = "SELECT " +
                            "e.cui, " +
                            "e.apellidos_nombres, " +
                            "u.correo_institucional, " +
                            "e.numero_matricula, " +
                            "e.estado_estudiante " +
                            "FROM estudiantes e " +
                            "INNER JOIN usuarios u ON e.id_usuario = u.id_usuario " +
                            "INNER JOIN matriculas m ON e.cui = m.cui " +
                            "WHERE m.grupo_id = ? " +
                            "AND m.estado_matricula = 'ACTIVO' " +
                            "ORDER BY e.apellidos_nombres";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, grupoId);
                    ResultSet rs = stmt.executeQuery();
                    
                    while (rs.next()) {
                        Map<String, Object> estudiante = new HashMap<>();
                        String cui = rs.getString("cui");
                        
                        estudiante.put("cui", cui);
                        estudiante.put("apellidos_nombres", rs.getString("apellidos_nombres"));
                        estudiante.put("correo_institucional", rs.getString("correo_institucional"));
                        estudiante.put("numero_matricula", rs.getInt("numero_matricula"));
                        estudiante.put("estado_estudiante", rs.getString("estado_estudiante"));
                        
                        //Verificar si está matriculado en laboratorio (solo para grupos de teoría)
                        if (tipoClase.equals("TEORIA") && tieneLab) {
                            boolean matriculadoLab = verificarMatriculaLab(conn, codigoCurso, cui);
                            estudiante.put("matriculadoLab", matriculadoLab);
                        }
                        
                        estudiantes.add(estudiante);
                    }
                }
            }
            
            return ResponseEntity.ok(estudiantes);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error en la base de datos"));
        }
    }
    
    private boolean verificarMatriculaLab(Connection conn, String codigoCurso, String cui) throws SQLException {
        String sql = "SELECT COUNT(*) as count " +
                    "FROM matriculas m " +
                    "INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id " +
                    "WHERE m.cui = ? " +
                    "AND g.codigo_curso = ? " +
                    "AND g.tipo_clase = 'LABORATORIO' " +
                    "AND m.estado_matricula = 'ACTIVO'";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cui);
            stmt.setString(2, codigoCurso);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        
        return false;
    }
    
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/lumina_bd?useSSL=false&serverTimezone=UTC";
            String user = "root";
            String password = "Piudz2012";
            
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado", e);
        }
    }
}