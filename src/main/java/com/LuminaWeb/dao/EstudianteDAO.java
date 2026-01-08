package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * DAO para operaciones relacionadas con estudiantes.
 * Usa JdbcTemplate y SimpleJdbcCall (Spring) en lugar de DBUtil.
 */
@Repository
public class EstudianteDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // -----------------------------
    // Clases internas (DTOs)
    // -----------------------------
    public static class Estudiante {
        public String cui;
        public int idUsuario;
        public String apellidosNombres;
        public String correoInstitucional;
        public int numeroMatricula;
    }

    public static class Horario {
        public int id_horario;
        public int id_matricula;
        public int grupo_id;
        public String codigo_curso;
        public String nombre_curso;
        public String letra_grupo;
        public String tipo_clase;
        public String ciclo;
        public int anio;
        public String semestre;
        public String dia_semana;
        public String hora_inicio;
        public String hora_fin;
        public String numero_salon;
        public String docente_nombre;
        public String estado;
    }

    public static class NotaDTO {
        public String tipo;
        public double valor;
        public double peso;
        public String fecha;
    }

    public static class CursoNotasDTO {
        public String nombre;
        public String codigo;
        public String docente;
        public double promedio;
        public List<NotaDTO> notas = new ArrayList<>();
    }

    public static class SilaboEstudiante {
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public String rutaArchivo;
    }

public List<SilaboEstudiante> obtenerSilabosPorCUI(String cui) {
    String sql = """
        SELECT DISTINCT s.codigo_curso, c.nombre_curso, s.grupo_teoria as letra_grupo, s.ruta_archivo
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN silabos s ON gc.codigo_curso = s.codigo_curso 
            AND gc.id_ciclo = s.id_ciclo 
            AND gc.letra_grupo = s.grupo_teoria
        INNER JOIN cursos c ON s.codigo_curso = c.codigo_curso
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO' AND s.estado = 'APROBADO'
        ORDER BY c.nombre_curso
    """;
    
    List<SilaboEstudiante> silabos = jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        SilaboEstudiante s = new SilaboEstudiante();
        s.codigoCurso = rs.getString("codigo_curso");
        s.nombreCurso = rs.getString("nombre_curso");
        s.letraGrupo = rs.getString("letra_grupo");
        s.rutaArchivo = rs.getString("ruta_archivo");
        
        // ⭐ LOG para ver qué viene de la BD
        System.out.println("=== DESDE BD ===");
        System.out.println("Ruta string: [" + s.rutaArchivo + "]");
        if (s.rutaArchivo != null) {
            System.out.println("Ruta length: " + s.rutaArchivo.length());
            System.out.println("Ruta bytes: " + java.util.Arrays.toString(s.rutaArchivo.getBytes()));
        }
        
        return s;
    });
    
    return silabos;
}

    // -----------------------------
    // MÉTODOS PRINCIPALES
    // -----------------------------

    /** Obtiene todos los estudiantes activos */
    public List<Estudiante> obtenerTodos() {
        String sql = """
            SELECT e.cui, e.id_usuario, e.apellidos_nombres, e.numero_matricula, u.correo_institucional
            FROM estudiantes e
            INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
            WHERE u.estado_cuenta = 'ACTIVO'
            ORDER BY e.apellidos_nombres
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Estudiante e = new Estudiante();
            e.cui = rs.getString("cui");
            e.idUsuario = rs.getInt("id_usuario");
            e.apellidosNombres = rs.getString("apellidos_nombres");
            e.numeroMatricula = rs.getInt("numero_matricula");
            e.correoInstitucional = rs.getString("correo_institucional");
            return e;
        });
    }

    /** Obtiene estudiante por correo institucional */
    public Estudiante obtenerPorCorreo(String correo) {
        String sql = """
            SELECT e.cui, e.id_usuario, e.apellidos_nombres, e.numero_matricula, u.correo_institucional
            FROM estudiantes e
            INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
            WHERE u.estado_cuenta = 'ACTIVO' AND u.correo_institucional = ?
        """;

        List<Estudiante> lista = jdbcTemplate.query(sql, new Object[]{correo}, (rs, rowNum) -> {
            Estudiante e = new Estudiante();
            e.cui = rs.getString("cui");
            e.idUsuario = rs.getInt("id_usuario");
            e.apellidosNombres = rs.getString("apellidos_nombres");
            e.numeroMatricula = rs.getInt("numero_matricula");
            e.correoInstitucional = rs.getString("correo_institucional");
            return e;
        });

        return lista.isEmpty() ? null : lista.get(0);
    }

    /** Llama al procedimiento almacenado sp_horarios_estudiante */
    public List<Horario> obtenerHorariosPorIdUsuario(int idUsuario) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_horarios_estudiantes");

        Map<String, Object> params = new HashMap<>();
        params.put("p_id_usuario", idUsuario);

        Map<String, Object> result = call.execute(params);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");
        if (rows == null) return new ArrayList<>();

        List<Horario> lista = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Horario h = new Horario();
            h.id_horario = safeInt(row.get("id_horario"));
            h.id_matricula = safeInt(row.get("id_matricula"));
            h.grupo_id = safeInt(row.get("grupo_id"));
            h.codigo_curso = safeStr(row.get("codigo_curso"));
            h.nombre_curso = safeStr(row.get("nombre_curso"));
            h.letra_grupo = safeStr(row.get("letra_grupo"));
            h.tipo_clase = safeStr(row.get("tipo_clase"));
            h.ciclo = safeStr(row.get("ciclo"));
            h.anio = safeInt(row.get("anio"));
            h.semestre = safeStr(row.get("semestre"));
            h.dia_semana = safeStr(row.get("dia_semana"));
            h.hora_inicio = safeStr(row.get("hora_inicio"));
            h.hora_fin = safeStr(row.get("hora_fin"));
            h.numero_salon = safeStr(row.get("numero_salon"));
            h.docente_nombre = safeStr(row.get("docente_nombre"));
            h.estado = safeStr(row.get("estado"));
            lista.add(h);
        }
        return lista;
    }

/** Llama al procedimiento notas_estudiante(p_cui) */
public List<CursoNotasDTO> getNotasPorCUI(String cui, String semestre) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("notas_estudiante");

    Map<String, Object> params = new HashMap<>();
    params.put("p_cui", cui);

    Map<String, Object> result = call.execute(params);
    List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");
    if (rows == null) return new ArrayList<>();

    Map<String, CursoNotasDTO> cursos = new LinkedHashMap<>();
    
    for (Map<String, Object> row : rows) {
        String codigo = safeStr(row.get("codigo_curso"));
        if (codigo == null) continue;

        CursoNotasDTO curso = cursos.computeIfAbsent(codigo, k -> {
            CursoNotasDTO c = new CursoNotasDTO();
            c.nombre = safeStr(row.get("nombre_curso"));
            c.codigo = codigo;
            c.docente = safeStr(row.get("docente_del_curso"));
            return c;
        });

        NotaDTO nota = new NotaDTO();
        nota.tipo = safeStr(row.get("tipo_evaluacion"));
        nota.valor = safeDouble(row.get("nota"));
        nota.peso = safeDouble(row.get("porcentaje")); 
        nota.fecha = safeStr(row.get("fecha_registro"));
        curso.notas.add(nota);
    }

    // CALCULAR PROMEDIO PONDERADO 
    for (CursoNotasDTO c : cursos.values()) {
        double sumaPonderada = 0.0;
        double sumaPorcentajes = 0.0;
        
        for (NotaDTO n : c.notas) {
            if (n.valor > 0 && n.peso > 0) {
                // Multiplicar nota por su porcentaje
                sumaPonderada += n.valor * (n.peso / 100.0);
                sumaPorcentajes += n.peso;
            }
        }
        
        // El promedio ponderado es la suma de (nota × porcentaje/100)
        if (sumaPorcentajes > 0) {
            c.promedio = Math.round(sumaPonderada * 100.0) / 100.0;
        } else {
            c.promedio = 0.0;
        }
    }

    return new ArrayList<>(cursos.values());
}

// estudiante_dashboard
    /* Obtiene resumen de cursos matriculados para un estudiante */
    public Map<String, Integer> obtenerResumenCursos(String cui) {
        String sql = """
            SELECT 
                COUNT(DISTINCT CASE WHEN g.tipo_clase = 'TEORIA' THEN g.codigo_curso END) as cursos_teoria,
                COUNT(DISTINCT CASE WHEN g.tipo_clase = 'LABORATORIO' THEN g.grupo_id END) as laboratorios
            FROM matriculas m
            INNER JOIN grupos_curso g ON m.grupo_id = g.grupo_id
            WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO'
        """;
    
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{cui}, (rs, rowNum) -> {
                Map<String, Integer> resumen = new HashMap<>();
                resumen.put("cursos", rs.getInt("cursos_teoria"));
                resumen.put("laboratorios", rs.getInt("laboratorios"));
                return resumen;
            });
        } catch (Exception e) {
            Map<String, Integer> resumen = new HashMap<>();
            resumen.put("cursos", 0);
            resumen.put("laboratorios", 0);
            return resumen;
        }
    }

    // -----------------------------
    // Métodos auxiliares
    // -----------------------------
    private static String safeStr(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static int safeInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }

    private static double safeDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0.0; }
    }
}
