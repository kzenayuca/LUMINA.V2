package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import com.LuminaWeb.dao.ProfesorDAO.CursoDocente;
import com.LuminaWeb.dao.ProfesorDAO.NotaEstudiante;

import java.io.BufferedReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.time.LocalDateTime;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;


@Repository
public class ProfesorDAO {

    private static final Logger logger = LoggerFactory.getLogger(ProfesorDAO.class);
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProfesorDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

// DTOs    
    public static class Profesor {
        public int idUsuario;
        public String apellidosNombres;
        public String correoInstitucional;
        public String departamento;
    }

    public static class Horario {
        public int id_horario;
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
        public String estado;
    }

    public static class CursoDocente {
        public String codigoCurso;
        public String nombreCurso;
        public String letraGrupo;
        public String tipoClase;
        public int grupoId;
        public int totalEstudiantes;
    }

    public static class EstadisticasProfesor {
        public int totalCursos;
        public int totalEstudiantes;
        public double asistenciaPromedio;
        public int notasPendientes;
    }

    public static class EstudianteCurso {
        public String cui;
        public int numeroMatricula;
        public String apellidosNombres;
        public String correoInstitucional;
    }

    public static class NotaEstudiante {
        public int idNota;
        public int idMatricula;
        public String cui;
        public String nombreEstudiante;
        public int numeroMatricula;
        public Map<String, Double> notas = new HashMap<>();
        public double promedio;
    }

    public static class PeriodoIngresoNotas {
        public int idPeriodo;
        public String codigoCurso;
        public String nombreCurso;
        public Integer tipoEvalId;
        public String nombreEvaluacion;
        public LocalDateTime fechaInicio;
        public LocalDateTime fechaFin;
        public String estado;
    }

// 0. INICIO SESIÓN

    //Obtiene todos los docentes activos 
    public List<Profesor> obtenerTodos() {
        String sql = """
            SELECT d.id_usuario, d.apellidos_nombres, d.departamento, 
                   u.correo_institucional
            FROM docentes d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            WHERE u.estado_cuenta = 'ACTIVO'
            ORDER BY d.apellidos_nombres
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Profesor p = new Profesor();
            p.idUsuario = rs.getInt("id_usuario");
            p.apellidosNombres = rs.getString("apellidos_nombres");
            p.correoInstitucional = rs.getString("correo_institucional");
            p.departamento = rs.getString("departamento");
            return p;
        });
    }

    //Obtiene profesor por correo institucional 
    public Profesor obtenerPorCorreo(String correo) {
        String sql = """
            SELECT d.id_usuario, d.apellidos_nombres, d.departamento,
                   u.correo_institucional
            FROM docentes d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            WHERE u.estado_cuenta = 'ACTIVO' AND u.correo_institucional = ?
        """;

        List<Profesor> lista = jdbcTemplate.query(sql, new Object[]{correo}, (rs, rowNum) -> {
            Profesor p = new Profesor();
            p.idUsuario = rs.getInt("id_usuario");
            p.apellidosNombres = rs.getString("apellidos_nombres");
            p.correoInstitucional = rs.getString("correo_institucional");
            p.departamento = rs.getString("departamento");
            return p;
        });

        return lista.isEmpty() ? null : lista.get(0);
    }

// 1. DASHBOARD
 //Obtiene cursos que dicta el docente 
    public List<CursoDocente> obtenerCursosDocente(int idUsuario) {
        //Primero obtenemos el id_docente
        String sqlIdDocente = """
            SELECT id_docente FROM docentes WHERE id_usuario = ?
        """;
        
        List<Integer> ids = jdbcTemplate.queryForList(sqlIdDocente, new Object[]{idUsuario}, Integer.class);
        if (ids.isEmpty()) return new ArrayList<>();
        
        int idDocente = ids.get(0);

        String sql = """
            SELECT DISTINCT
                c.codigo_curso,
                c.nombre_curso,
                g.letra_grupo,
                g.tipo_clase,
                g.grupo_id,
                COUNT(DISTINCT m.id_matricula) AS total_estudiantes
            FROM horarios h
            INNER JOIN grupos_curso g ON h.grupo_id = g.grupo_id
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            LEFT JOIN matriculas m ON g.grupo_id = m.grupo_id 
                AND m.estado_matricula = 'ACTIVO'
            WHERE h.id_docente = ?
              AND ca.estado = 'ACTIVO'
              AND g.estado = 'ACTIVO'
            GROUP BY c.codigo_curso, c.nombre_curso, g.letra_grupo, 
                     g.tipo_clase, g.grupo_id
            ORDER BY c.codigo_curso, g.tipo_clase, g.letra_grupo
        """;

        return jdbcTemplate.query(sql, new Object[]{idDocente}, (rs, rowNum) -> {
            CursoDocente curso = new CursoDocente();
            curso.codigoCurso = rs.getString("codigo_curso");
            curso.nombreCurso = rs.getString("nombre_curso");
            curso.letraGrupo = rs.getString("letra_grupo");
            curso.tipoClase = rs.getString("tipo_clase");
            curso.grupoId = rs.getInt("grupo_id");
            curso.totalEstudiantes = rs.getInt("total_estudiantes");
            return curso;
        });
    }

    /** Obtiene estadísticas del profesor */
    public EstadisticasProfesor obtenerEstadisticasProfesor(int idUsuario) {
        String sqlIdDocente = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        List<Integer> ids = jdbcTemplate.queryForList(sqlIdDocente, new Object[]{idUsuario}, Integer.class);
        
        if (ids.isEmpty()) {
            EstadisticasProfesor stats = new EstadisticasProfesor();
            stats.totalCursos = 0;
            stats.totalEstudiantes = 0;
            stats.asistenciaPromedio = 0.0;
            stats.notasPendientes = 0;
            return stats;
        }
        
        int idDocente = ids.get(0);

        EstadisticasProfesor stats = new EstadisticasProfesor();

        //Total de cursos (grupos únicos)
        String sqlCursos = """
            SELECT COUNT(DISTINCT g.grupo_id) AS total
            FROM horarios h
            INNER JOIN grupos_curso g ON h.grupo_id = g.grupo_id
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            WHERE h.id_docente = ? AND ca.estado = 'ACTIVO'
        """;
        stats.totalCursos = jdbcTemplate.queryForObject(sqlCursos, new Object[]{idDocente}, Integer.class);

        //Total de estudiantes
        String sqlEstudiantes = """
            SELECT COUNT(DISTINCT m.cui) AS total
            FROM horarios h
            INNER JOIN grupos_curso g ON h.grupo_id = g.grupo_id
            INNER JOIN matriculas m ON g.grupo_id = m.grupo_id
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            WHERE h.id_docente = ? 
              AND ca.estado = 'ACTIVO'
              AND m.estado_matricula = 'ACTIVO'
        """;
        stats.totalEstudiantes = jdbcTemplate.queryForObject(sqlEstudiantes, new Object[]{idDocente}, Integer.class);

        //Asistencia promedio (simplificado)
        stats.asistenciaPromedio = 92.0; 

        //Notas pendientes (estudiantes sin todas las notas)
        stats.notasPendientes = 0;

        return stats;
    }

// 2. ASISTENCIA en otro archivo...

// 3. GESTIÓN DE NOTAS (INCLUYE SUBIDA DE EXÁMENES)
//Devuelve optional id_docente por id_usuario
    public Optional<Integer> obtenerIdDocentePorIdUsuario(int idUsuario) {
        String sql = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        List<Integer> ids = jdbcTemplate.queryForList(sql, new Object[]{idUsuario}, Integer.class);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
    //Obtiene estudiantes de un grupo con sus notas
    public List<NotaEstudiante> obtenerEstudiantesConNotas(int grupoId) {
        String sqlEstudiantes = """
            SELECT DISTINCT
                e.cui,
                e.numero_matricula,
                e.apellidos_nombres,
                u.correo_institucional,
                m.id_matricula
            FROM matriculas m
            INNER JOIN estudiantes e ON m.cui = e.cui
            INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
            WHERE m.grupo_id = ?
              AND m.estado_matricula = 'ACTIVO'
            ORDER BY e.apellidos_nombres
        """;

        List<NotaEstudiante> estudiantes = jdbcTemplate.query(
            sqlEstudiantes, 
            new Object[]{grupoId}, 
            (rs, rowNum) -> {
                NotaEstudiante ne = new NotaEstudiante();
                ne.cui = rs.getString("cui");
                ne.numeroMatricula = rs.getInt("numero_matricula");
                ne.nombreEstudiante = rs.getString("apellidos_nombres");
                ne.idMatricula = rs.getInt("id_matricula");
                ne.notas = new HashMap<>(); //Inicializar el Map
                return ne;
            }
        );

        //Para cada estudiante, obtener sus notas
        String sqlNotas = """
            SELECT 
                te.tipo_eval_id,
                te.codigo,
                n.calificacion
            FROM notas n
            INNER JOIN tipos_evaluacion te ON n.tipo_eval_id = te.tipo_eval_id
            WHERE n.id_matricula = ?
        """;

        for (NotaEstudiante est : estudiantes) {
            List<Map<String, Object>> notasRows = jdbcTemplate.queryForList(
                sqlNotas, est.idMatricula
            );
            
            for (Map<String, Object> row : notasRows) {
                //Guardamos la nota usando el tipo_eval_id como clave (p. ej. "3")
                String codigo = String.valueOf(safeInt(row.get("tipo_eval_id")));
                Double calificacion = safeDouble(row.get("calificacion"));
                est.notas.put(codigo, calificacion);
            }

            //Calcular promedio
            if (!est.notas.isEmpty()) {
                double suma = est.notas.values().stream()
                    .filter(v -> v != null)
                    .mapToDouble(Double::doubleValue).sum();
                long count = est.notas.values().stream()
                    .filter(v -> v != null)
                    .count();
                est.promedio = count > 0 ? suma / count : 0.0;
            } else {
                est.promedio = 0.0;
            }
        }

        return estudiantes;
    }

    //Guarda o actualiza una nota individual 
    public boolean guardarNota(int idMatricula, int tipoEvalId, double calificacion, int idDocente) {
        try {
            //Verificar si ya existe la nota
            String sqlExiste = """
                SELECT COUNT(*) FROM notas 
                WHERE id_matricula = ? AND tipo_eval_id = ?
            """;
            
            Integer count = jdbcTemplate.queryForObject(
                sqlExiste, 
                new Object[]{idMatricula, tipoEvalId}, 
                Integer.class
            );

            if (count != null && count > 0) {
                //Actualizar
                String sqlUpdate = """
                    UPDATE notas 
                    SET calificacion = ?, 
                        fecha_registro = NOW(),
                        docente_registro_id = ?
                    WHERE id_matricula = ? AND tipo_eval_id = ?
                """;
                jdbcTemplate.update(sqlUpdate, calificacion, idDocente, idMatricula, tipoEvalId);
            } else {
                //Insertar
                String sqlInsert = """
                    INSERT INTO notas (id_matricula, tipo_eval_id, calificacion, docente_registro_id)
                    VALUES (?, ?, ?, ?)
                """;
                jdbcTemplate.update(sqlInsert, idMatricula, tipoEvalId, calificacion, idDocente);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //Procesa un archivo Excel/CSV de notas 
    public Map<String, Object> procesarArchivoNotas(
            MultipartFile file, 
            int grupoId, 
            int tipoEvalId,
            int idDocente) {
        
        Map<String, Object> result = new HashMap<>();
        int procesados = 0;
        int errores = 0;
        List<String> mensajesError = new ArrayList<>();

        if (file.isEmpty()) {
            result.put("success", false);
            result.put("mensaje", "El archivo está vacío");
            return result;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            result.put("success", false);
            result.put("mensaje", "Nombre de archivo inválido");
            return result;
        }

        filename = filename.toLowerCase();

        try {
            //PROCESAR EXCEL (.xls o .xlsx)
            if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
                Workbook workbook = null;
                
                try (InputStream is = file.getInputStream()) {
                    //Crear el workbook según la extensión
                    if (filename.endsWith(".xlsx")) {
                        workbook = new XSSFWorkbook(is);
                    } else {
                        workbook = new HSSFWorkbook(is);
                    }
                    
                    Sheet sheet = workbook.getSheetAt(0);
                    int rowNum = 0;

                    for (Row row : sheet) {
                        rowNum++;
                        
                        //Saltar la primera fila (encabezados)
                        if (rowNum == 1) continue;

                        //Verificar si la fila está completamente vacía
                        if (esFilaVacia(row)) continue;

                        try {
                            //Columna 0: CUI
                            Cell cellCui = row.getCell(0);
                            if (cellCui == null) {
                                mensajesError.add("Fila " + rowNum + ": CUI vacío");
                                errores++;
                                continue;
                            }
                            String cui = obtenerValorCelda(cellCui);
                            if (cui == null || cui.trim().isEmpty()) {
                                mensajesError.add("Fila " + rowNum + ": CUI inválido");
                                errores++;
                                continue;
                            }
                            cui = cui.trim();

                            //Columna 2: Calificación
                            Cell cellCal = row.getCell(2);
                            if (cellCal == null) {
                                mensajesError.add("Fila " + rowNum + ": Calificación vacía");
                                errores++;
                                continue;
                            }
                            String calificacionStr = obtenerValorCelda(cellCal);
                            if (calificacionStr == null || calificacionStr.trim().isEmpty()) {
                                mensajesError.add("Fila " + rowNum + ": Calificación inválida");
                                errores++;
                                continue;
                            }
                            //Convertir calificación
                            double calificacion;
                            try {
                                calificacion = Double.parseDouble(calificacionStr.replace(',', '.'));
                            } catch (NumberFormatException e) {
                                mensajesError.add("Fila " + rowNum + ": Calificación no es un número válido: " + calificacionStr);
                                errores++;
                                continue;
                            }
                            //Validar rango de calificación
                            if (calificacion < 0 || calificacion > 20) {
                                mensajesError.add("Fila " + rowNum + ": Calificación fuera de rango (0-20): " + calificacion);
                                errores++;
                                continue;
                            }
                            //Buscar id_matricula usando CUI
                            String sqlMatricula = """
                                SELECT m.id_matricula
                                FROM matriculas m
                                WHERE m.cui = ?
                                  AND m.grupo_id = ?
                                  AND m.estado_matricula = 'ACTIVO'
                            """;

                            List<Integer> matriculas = jdbcTemplate.queryForList(
                                sqlMatricula,
                                new Object[]{cui, grupoId},
                                Integer.class
                            );

                            if (matriculas.isEmpty()) {
                                mensajesError.add("Fila " + rowNum + ": Estudiante no encontrado con CUI: " + cui);
                                errores++;
                                continue;
                            }

                            int idMatricula = matriculas.get(0);
                            if (guardarNota(idMatricula, tipoEvalId, calificacion, idDocente)) {
                                procesados++;
                            } else {
                                mensajesError.add("Fila " + rowNum + ": Error al guardar nota para CUI: " + cui);
                                errores++;
                            }

                        } catch (Exception e) {
                            mensajesError.add("Fila " + rowNum + ": Error inesperado: " + e.getMessage());
                            errores++;
                            e.printStackTrace();
                        }
                    }

                } finally {
                    if (workbook != null) {
                        try {
                            workbook.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            //PROCESAR CSV/TXT
            } else if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                    String line;
                    int lineNum = 0;

                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        
                        //Saltar encabezado
                        if (lineNum == 1) continue;

                        //Saltar líneas vacías
                        if (line.trim().isEmpty()) continue;

                        String[] campos = line.split("[,;\\t]");

                        if (campos.length < 3) {
                            mensajesError.add("Línea " + lineNum + ": Formato incorrecto (se esperan al menos 3 columnas)");
                            errores++;
                            continue;
                        }

                        try {
                            String cui = campos[0].trim();
                            String calificacionStr = campos[2].trim();

                            if (cui.isEmpty() || calificacionStr.isEmpty()) {
                                mensajesError.add("Línea " + lineNum + ": Campos vacíos");
                                errores++;
                                continue;
                            }

                            double calificacion = Double.parseDouble(calificacionStr.replace(',', '.'));

                            if (calificacion < 0 || calificacion > 20) {
                                mensajesError.add("Línea " + lineNum + ": Calificación fuera de rango: " + calificacion);
                                errores++;
                                continue;
                            }

                            //Buscar id_matricula usando CUI
                            String sqlMatricula = """
                                SELECT m.id_matricula
                                FROM matriculas m
                                WHERE m.cui = ?
                                  AND m.grupo_id = ?
                                  AND m.estado_matricula = 'ACTIVO'
                            """;

                            List<Integer> matriculas = jdbcTemplate.queryForList(
                                sqlMatricula,
                                new Object[]{cui, grupoId},
                                Integer.class
                            );

                            if (matriculas.isEmpty()) {
                                mensajesError.add("Línea " + lineNum + ": Estudiante no encontrado con CUI: " + cui);
                                errores++;
                                continue;
                            }

                            int idMatricula = matriculas.get(0);
                            if (guardarNota(idMatricula, tipoEvalId, calificacion, idDocente)) {
                                procesados++;
                            } else {
                                mensajesError.add("Línea " + lineNum + ": Error al guardar nota para CUI: " + cui);
                                errores++;
                            }

                        } catch (NumberFormatException e) {
                            mensajesError.add("Línea " + lineNum + ": Formato de número inválido");
                            errores++;
                        }
                    }
                }
            } else {
                result.put("success", false);
                result.put("mensaje", "Formato de archivo no soportado. Use .xlsx, .xls, .csv o .txt");
                return result;
            }
            
            result.put("success", true);
            result.put("procesados", procesados);
            result.put("errores", errores);
            result.put("mensajes", mensajesError);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("mensaje", "Error procesando archivo: " + e.getMessage());
            result.put("detalleError", e.getClass().getName());
        }

        return result;
    }

//PERÍODOS DE INGRESO DE NOTAS
    //Verifica si hay período activo para un curso y tipo de evaluación 
    public boolean hayPeriodoActivoNotas(String codigoCurso, Integer tipoEvalId) {
        //Primero actualizar estados
        actualizarEstadosPeriodosNotas();

        String sql = """
            SELECT COUNT(*) 
            FROM periodos_ingreso_notas
            WHERE estado = 'ACTIVO'
              AND (codigo_curso IS NULL OR codigo_curso = ?)
              AND (tipo_eval_id IS NULL OR tipo_eval_id = ?)
        """;

        Integer count = jdbcTemplate.queryForObject(
            sql, 
            new Object[]{codigoCurso, tipoEvalId}, 
            Integer.class
        );

        return count != null && count > 0;
    }

    //Obtiene períodos activos para el docente 
    public List<PeriodoIngresoNotas> obtenerPeriodosActivosDocente(int idUsuario) {
        actualizarEstadosPeriodosNotas();

        //Obtener cursos del docente
        List<CursoDocente> cursos = obtenerCursosDocente(idUsuario);
        if (cursos.isEmpty()) return new ArrayList<>();

        //Construir lista de códigos de curso
        List<String> codigosCurso = new ArrayList<>();
        for (CursoDocente c : cursos) {
            if (!codigosCurso.contains(c.codigoCurso)) {
                codigosCurso.add(c.codigoCurso);
            }
        }

        //Construir placeholders para IN clause
        String placeholders = String.join(",", Collections.nCopies(codigosCurso.size(), "?"));

        String sql = String.format("""
            SELECT 
                p.id_periodo,
                p.codigo_curso,
                c.nombre_curso,
                p.tipo_eval_id,
                te.nombre AS nombre_evaluacion,
                p.fecha_inicio,
                p.fecha_fin,
                p.estado
            FROM periodos_ingreso_notas p
            LEFT JOIN cursos c ON p.codigo_curso = c.codigo_curso
            LEFT JOIN tipos_evaluacion te ON p.tipo_eval_id = te.tipo_eval_id
            WHERE p.estado = 'ACTIVO'
              AND (p.codigo_curso IS NULL OR p.codigo_curso IN (%s))
            ORDER BY p.fecha_inicio DESC
        """, placeholders);

        return jdbcTemplate.query(sql, codigosCurso.toArray(), (rs, rowNum) -> {
            PeriodoIngresoNotas p = new PeriodoIngresoNotas();
            p.idPeriodo = rs.getInt("id_periodo");
            p.codigoCurso = rs.getString("codigo_curso");
            p.nombreCurso = rs.getString("nombre_curso");
            
            Object tipoEvalIdObj = rs.getObject("tipo_eval_id");
            p.tipoEvalId = tipoEvalIdObj != null ? rs.getInt("tipo_eval_id") : null;
            
            p.nombreEvaluacion = rs.getString("nombre_evaluacion");
            p.fechaInicio = rs.getTimestamp("fecha_inicio").toLocalDateTime();
            p.fechaFin = rs.getTimestamp("fecha_fin").toLocalDateTime();
            p.estado = rs.getString("estado");
            return p;
        });
    }

    //Actualiza estados de períodos 
    public void actualizarEstadosPeriodosNotas() {
        try {
            jdbcTemplate.execute("CALL actualizar_estados_periodos_notas()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Obtiene tipos de evaluación habilitados para un curso 
    public List<Map<String, Object>> obtenerTiposEvaluacionHabilitados(String codigoCurso) {
        actualizarEstadosPeriodosNotas();

        String sql = """
            SELECT DISTINCT
                te.tipo_eval_id,
                te.codigo,
                te.nombre,
                te.tipo
            FROM periodos_ingreso_notas p
            INNER JOIN tipos_evaluacion te ON p.tipo_eval_id = te.tipo_eval_id
            WHERE p.estado = 'ACTIVO'
              AND (p.codigo_curso IS NULL OR p.codigo_curso = ?)
            ORDER BY te.tipo_eval_id
        """;

        return jdbcTemplate.queryForList(sql, codigoCurso);
    }

//Guarda un archivo de examen PDF en el servidor y BD
public boolean guardarExamenPDF(MultipartFile file, int grupoId, int tipoEvalId, String tipo, String rutaBase, int idDocente) {
    try {
        //Validar que sea evaluación parcial (tipo 1, 2 o 3)
        if (tipoEvalId < 1 || tipoEvalId > 3) {
            logger.warn("Intento de subir examen para evaluación no parcial: {}", tipoEvalId);
            return false;
        }
        
        //Verificar si ya existe
        if (existeExamenSubido(grupoId, tipoEvalId, tipo)) {
            logger.warn("Ya existe un examen {} para grupo {} y evaluación {}", tipo, grupoId, tipoEvalId);
            return false;
        }
        
        //Obtener información del curso y grupo
        String sqlCurso = """
            SELECT c.codigo_curso, c.nombre_curso, g.letra_grupo
            FROM grupos_curso g
            JOIN cursos c ON g.codigo_curso = c.codigo_curso
            WHERE g.grupo_id = ?
        """;
        
        Map<String, Object> cursoInfo = jdbcTemplate.queryForMap(sqlCurso, grupoId);
        String codigoCurso = (String) cursoInfo.get("codigo_curso");
        String nombreCurso = (String) cursoInfo.get("nombre_curso");
        String letraGrupo = (String) cursoInfo.get("letra_grupo");
        
        //Crear directorio "notas" si no existe 
        Path directorioNotas = Paths.get(rutaBase, "notas");
        Files.createDirectories(directorioNotas);        
        //Generar nombre según formato solicitado: NOTA-[Curso]_[Grupo]_[tipo]_[Unidad].pdf
        //tipoEvalId 1=EP1(Unidad 1), 2=EP2(Unidad 2), 3=EP3(Unidad 3)
        String nombreArchivo = String.format("NOTA-%s_%s_%s_%d.pdf", 
            nombreCurso.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ0-9 ]", ""), //Mantener espacios y caracteres especiales del español
            letraGrupo,
            tipo.toLowerCase(), //"alta" o "baja" en minúsculas
            tipoEvalId //1, 2 o 3 como número de unidad
        );
        
        Path destino = directorioNotas.resolve(nombreArchivo);
        
        //Guardar archivo físico
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        
        //Guardar en BD
        String sqlInsert = """
            INSERT INTO examenes_pdf (grupo_id, tipo_eval_id, tipo_nota, ruta_archivo, subido_por)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        int rowsAffected = jdbcTemplate.update(
            sqlInsert, 
            grupoId, 
            tipoEvalId, 
            tipo.toUpperCase(), //"ALTA" o "BAJA" en mayúsculas para la BD
            destino.toString(), 
            idDocente
        );
        
        if (rowsAffected > 0) {
            logger.info("✓ Examen PDF guardado exitosamente: {}", destino.toString());
            logger.info("✓ Registro insertado en BD: grupo_id={}, tipo_eval_id={}, tipo_nota={}", 
                grupoId, tipoEvalId, tipo.toUpperCase());
            return true;
        } else {
            logger.error("✗ No se insertó ninguna fila en examenes_pdf");
            return false;
        }
        
    } catch (org.springframework.dao.DuplicateKeyException dke) {
        logger.error("✗ Error: Ya existe un registro con estos datos (violación de unique key)", dke);
        return false;
    } catch (Exception e) {
        logger.error("✗ Error al guardar examen PDF", e);
        e.printStackTrace();
        return false;
    }
}

public boolean existeExamenSubido(int grupoId, int tipoEvalId, String tipoNota) {
    try {
        String sql = """
            SELECT COUNT(*) FROM examenes_pdf
            WHERE grupo_id = ? AND tipo_eval_id = ? AND tipo_nota = ?
        """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, grupoId, tipoEvalId, tipoNota.toUpperCase());
        return count != null && count > 0;
        
    } catch (Exception e) {
        logger.error("Error al verificar examen", e);
        return false;
    }
}

//Obtiene información de exámenes subidos para un grupo y evaluación
public Map<String, Object> obtenerExamenesSubidos(int grupoId, int tipoEvalId) {
    try {
        String sql = """
            SELECT tipo_nota, ruta_archivo, fecha_subida
            FROM examenes_pdf
            WHERE grupo_id = ? AND tipo_eval_id = ?
        """;
        
        List<Map<String, Object>> examenes = jdbcTemplate.queryForList(sql, grupoId, tipoEvalId);
        
        Map<String, Object> resultado = new HashMap<>();
        for (Map<String, Object> examen : examenes) {
            String tipoNota = (String) examen.get("tipo_nota");
            resultado.put(tipoNota.toLowerCase(), examen);
        }
        
        return resultado;
        
    } catch (Exception e) {
        logger.error("Error al obtener exámenes subidos", e);
        return new HashMap<>();
    }
}

//Métodos auxiliares    
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

    //Devuelve true si la fila está vacía
    private boolean esFilaVacia(Row row) {
        if (row == null) return true;
        DataFormatter fmt = new DataFormatter();
        for (Cell c : row) {
            if (c == null) continue;
            String v = fmt.formatCellValue(c);
            if (v != null && !v.trim().isEmpty()) return false;
        }
        return true;
    }

    //Obtiene el valor textual de una celda usando DataFormatter. 
    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return null;
        DataFormatter fmt = new DataFormatter();
        String val = fmt.formatCellValue(cell);
        return val == null ? null : val.trim();
    }

// 4. ESTADÍSTICAS
//Obtiene código de curso y ciclo por grupo ID
public Map<String, Object> obtenerDatosGrupo(int grupoId) {
    try {
        String sql = "SELECT codigo_curso, id_ciclo FROM grupos_curso WHERE grupo_id = ?";
        return jdbcTemplate.queryForMap(sql, grupoId);
    } catch (Exception e) {
        logger.error("Error al obtener datos del grupo", e);
        return new HashMap<>();
    }
}
//Obtiene asistencias de estudiantes por grupo con fechas
public List<Map<String, Object>> obtenerAsistenciasPorGrupo(int grupoId) {
    try {
        //Primero obtener estudiantes del grupo
        String sqlEstudiantes = """
            SELECT DISTINCT
                e.cui,
                e.apellidos_nombres,
                m.id_matricula
            FROM matriculas m
            INNER JOIN estudiantes e ON m.cui = e.cui
            WHERE m.grupo_id = ?
              AND m.estado_matricula = 'ACTIVO'
            ORDER BY e.apellidos_nombres
        """;

        List<Map<String, Object>> estudiantes = jdbcTemplate.queryForList(sqlEstudiantes, grupoId);

        //Para cada estudiante, obtener sus asistencias con fechas
        String sqlAsistencias = """
            SELECT 
                a.fecha,
                a.estado_asistencia
            FROM asistencias_estudiante a
            INNER JOIN horarios h ON a.id_horario = h.id_horario
            WHERE a.id_matricula = ?
              AND h.grupo_id = ?
            ORDER BY a.fecha
        """;

        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Map<String, Object> est : estudiantes) {
            int idMatricula = ((Number) est.get("id_matricula")).intValue();
            
            Map<String, Object> estudianteData = new HashMap<>();
            estudianteData.put("cui", est.get("cui"));
            estudianteData.put("nombreEstudiante", est.get("apellidos_nombres"));
            estudianteData.put("idMatricula", idMatricula);

            //Obtener asistencias
            List<Map<String, Object>> asistencias = jdbcTemplate.queryForList(
                sqlAsistencias, idMatricula, grupoId
            );

            //Crear mapa de fecha -> valor (1=presente, 0=falta)
            Map<String, Integer> asistenciasMap = new HashMap<>();
            for (Map<String, Object> asis : asistencias) {
                String fecha = asis.get("fecha").toString();
                String estado = (String) asis.get("estado_asistencia");
                asistenciasMap.put(fecha, "PRESENTE".equals(estado) ? 1 : 0);
            }

            estudianteData.put("asistencias", asistenciasMap);
            resultado.add(estudianteData);
        }

        return resultado;

    } catch (Exception e) {
        logger.error("Error al obtener asistencias por grupo", e);
        return new ArrayList<>();
    }
}

//Obtiene estadísticas de asistencia por grupo
public Map<String, Object> obtenerEstadisticasAsistencia(int grupoId) {
    try {
        //Total de clases realizadas
        String sqlClases = """
            SELECT COUNT(DISTINCT a.fecha) as total_clases
            FROM asistencias_estudiante a
            INNER JOIN horarios h ON a.id_horario = h.id_horario
            WHERE h.grupo_id = ?
        """;
        
        Integer totalClases = jdbcTemplate.queryForObject(sqlClases, Integer.class, grupoId);
        if (totalClases == null) totalClases = 0;

        //Estadísticas de asistencia
        String sqlEstadisticas = """
            SELECT 
                COUNT(DISTINCT m.cui) as total_estudiantes,
                SUM(CASE WHEN a.estado_asistencia = 'PRESENTE' THEN 1 ELSE 0 END) as total_presentes,
                SUM(CASE WHEN a.estado_asistencia = 'FALTA' THEN 1 ELSE 0 END) as total_faltas
            FROM matriculas m
            LEFT JOIN asistencias_estudiante a ON m.id_matricula = a.id_matricula
            LEFT JOIN horarios h ON a.id_horario = h.id_horario
            WHERE m.grupo_id = ?
              AND m.estado_matricula = 'ACTIVO'
        """;

        Map<String, Object> stats = jdbcTemplate.queryForMap(sqlEstadisticas, grupoId);
        
        int totalEstudiantes = ((Number) stats.get("total_estudiantes")).intValue();
        int totalPresentes = stats.get("total_presentes") != null ? 
            ((Number) stats.get("total_presentes")).intValue() : 0;
        int totalFaltas = stats.get("total_faltas") != null ? 
            ((Number) stats.get("total_faltas")).intValue() : 0;

        //Calcular porcentaje de asistencia
        int totalRegistros = totalPresentes + totalFaltas;
        double porcentajeAsistencia = totalRegistros > 0 ? 
            (totalPresentes * 100.0 / totalRegistros) : 0.0;

        //Estudiantes por rango de asistencia
        String sqlRangos = """
            SELECT 
                m.cui,
                COUNT(CASE WHEN a.estado_asistencia = 'PRESENTE' THEN 1 END) as presentes,
                COUNT(a.id_asistencia) as total
            FROM matriculas m
            LEFT JOIN asistencias_estudiante a ON m.id_matricula = a.id_matricula
            LEFT JOIN horarios h ON a.id_horario = h.id_horario
            WHERE m.grupo_id = ?
              AND m.estado_matricula = 'ACTIVO'
            GROUP BY m.cui
        """;

        List<Map<String, Object>> rangos = jdbcTemplate.queryForList(sqlRangos, grupoId);
        
        int mayores90 = 0;
        int entre70y90 = 0;
        int menores70 = 0;

        for (Map<String, Object> rango : rangos) {
            int presentes = ((Number) rango.get("presentes")).intValue();
            int total = ((Number) rango.get("total")).intValue();
            
            if (total > 0) {
                double porcentaje = (presentes * 100.0 / total);
                if (porcentaje > 90) mayores90++;
                else if (porcentaje >= 70) entre70y90++;
                else menores70++;
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalClases", totalClases);
        resultado.put("totalEstudiantes", totalEstudiantes);
        resultado.put("porcentajeAsistencia", porcentajeAsistencia);
        resultado.put("estudiantesMayor90", mayores90);
        resultado.put("estudiantesEntre70y90", entre70y90);
        resultado.put("estudiantesMenor70", menores70);

        return resultado;

    } catch (Exception e) {
        logger.error("Error al obtener estadísticas de asistencia", e);
        Map<String, Object> empty = new HashMap<>();
        empty.put("porcentajeAsistencia", 0.0);
        empty.put("totalClases", 0);
        return empty;
    }
}

//Obtiene indicadores rápidos del curso (promedio, aprobación, etc.)
public Map<String, Object> obtenerIndicadoresRapidos(int grupoId) {
    try {
        String sql = """
            SELECT 
                AVG(
                    (COALESCE(n1.calificacion, 0) + COALESCE(n2.calificacion, 0) + 
                     COALESCE(n3.calificacion, 0) + COALESCE(n4.calificacion, 0) + 
                     COALESCE(n5.calificacion, 0) + COALESCE(n6.calificacion, 0)) / 6
                ) as promedio_general,
                COUNT(DISTINCT m.cui) as total_estudiantes,
                SUM(CASE 
                    WHEN (COALESCE(n1.calificacion, 0) + COALESCE(n2.calificacion, 0) + 
                          COALESCE(n3.calificacion, 0) + COALESCE(n4.calificacion, 0) + 
                          COALESCE(n5.calificacion, 0) + COALESCE(n6.calificacion, 0)) / 6 >= 10.5 
                    THEN 1 ELSE 0 
                END) as aprobados,
                SUM(CASE 
                    WHEN (COALESCE(n1.calificacion, 0) + COALESCE(n2.calificacion, 0) + 
                          COALESCE(n3.calificacion, 0) + COALESCE(n4.calificacion, 0) + 
                          COALESCE(n5.calificacion, 0) + COALESCE(n6.calificacion, 0)) / 6 < 10.5 
                    THEN 1 ELSE 0 
                END) as desaprobados
            FROM matriculas m
            LEFT JOIN notas n1 ON m.id_matricula = n1.id_matricula AND n1.tipo_eval_id = 1
            LEFT JOIN notas n2 ON m.id_matricula = n2.id_matricula AND n2.tipo_eval_id = 2
            LEFT JOIN notas n3 ON m.id_matricula = n3.id_matricula AND n3.tipo_eval_id = 3
            LEFT JOIN notas n4 ON m.id_matricula = n4.id_matricula AND n4.tipo_eval_id = 4
            LEFT JOIN notas n5 ON m.id_matricula = n5.id_matricula AND n5.tipo_eval_id = 5
            LEFT JOIN notas n6 ON m.id_matricula = n6.id_matricula AND n6.tipo_eval_id = 6
            WHERE m.grupo_id = ?
              AND m.estado_matricula = 'ACTIVO'
        """;

        Map<String, Object> resultado = jdbcTemplate.queryForMap(sql, grupoId);
        
        Double promedioGeneral = resultado.get("promedio_general") != null ? 
            ((Number) resultado.get("promedio_general")).doubleValue() : 0.0;
        int totalEstudiantes = ((Number) resultado.get("total_estudiantes")).intValue();
        int aprobados = resultado.get("aprobados") != null ? 
            ((Number) resultado.get("aprobados")).intValue() : 0;
        int desaprobados = resultado.get("desaprobados") != null ? 
            ((Number) resultado.get("desaprobados")).intValue() : 0;

        double porcentajeAprobacion = totalEstudiantes > 0 ? 
            (aprobados * 100.0 / totalEstudiantes) : 0.0;

        Map<String, Object> indicadores = new HashMap<>();
        indicadores.put("promedioGeneral", promedioGeneral);
        indicadores.put("totalEstudiantes", totalEstudiantes);
        indicadores.put("aprobados", aprobados);
        indicadores.put("desaprobados", desaprobados);
        indicadores.put("porcentajeAprobacion", porcentajeAprobacion);

        return indicadores;

    } catch (Exception e) {
        logger.error("Error al obtener indicadores rápidos", e);
        Map<String, Object> empty = new HashMap<>();
        empty.put("promedioGeneral", 0.0);
        empty.put("porcentajeAprobacion", 0.0);
        return empty;
    }
}

//Obtiene el avance del temario por curso (basado en sílabos)
public Map<String, Object> obtenerAvanceTemario(String codigoCurso, int idCiclo) {
    try {
        String sql = """
            SELECT 
                u.unidad_id,
                u.numero_unidad,
                u.nombre_unidad,
                COUNT(t.id_tema) as total_temas,
                SUM(CASE WHEN t.estado = 'COMPLETADO' THEN 1 ELSE 0 END) as temas_completados
            FROM silabos s
            INNER JOIN unidades u ON s.id_silabo = u.id_silabo
            LEFT JOIN temas t ON u.unidad_id = t.unidad_id
            WHERE s.codigo_curso = ?
              AND s.id_ciclo = ?
              AND s.estado = 'APROBADO'
            GROUP BY u.unidad_id, u.numero_unidad, u.nombre_unidad
            ORDER BY u.numero_unidad
        """;

        List<Map<String, Object>> unidades = jdbcTemplate.queryForList(sql, codigoCurso, idCiclo);
        
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> unidadesData = new ArrayList<>();
        
        int totalTemasGeneral = 0;
        int temasCompletadosGeneral = 0;

        for (Map<String, Object> unidad : unidades) {
            Map<String, Object> unidadData = new HashMap<>();
            
            int numeroUnidad = ((Number) unidad.get("numero_unidad")).intValue();
            String nombreUnidad = (String) unidad.get("nombre_unidad");
            int totalTemas = ((Number) unidad.get("total_temas")).intValue();
            int temasCompletados = ((Number) unidad.get("temas_completados")).intValue();
            
            double porcentaje = totalTemas > 0 ? (temasCompletados * 100.0 / totalTemas) : 0.0;
            
            unidadData.put("numeroUnidad", numeroUnidad);
            unidadData.put("nombreUnidad", nombreUnidad);
            unidadData.put("totalTemas", totalTemas);
            unidadData.put("temasCompletados", temasCompletados);
            unidadData.put("porcentaje", porcentaje);
            
            unidadesData.add(unidadData);
            
            totalTemasGeneral += totalTemas;
            temasCompletadosGeneral += temasCompletados;
        }

        double porcentajeGeneral = totalTemasGeneral > 0 ? 
            (temasCompletadosGeneral * 100.0 / totalTemasGeneral) : 0.0;

        resultado.put("unidades", unidadesData);
        resultado.put("porcentajeGeneral", porcentajeGeneral);
        resultado.put("totalTemas", totalTemasGeneral);
        resultado.put("temasCompletados", temasCompletadosGeneral);

        return resultado;

    } catch (Exception e) {
        logger.error("Error al obtener avance del temario", e);
        Map<String, Object> empty = new HashMap<>();
        empty.put("unidades", new ArrayList<>());
        empty.put("porcentajeGeneral", 0.0);
        return empty;
    }
}

// 5. HORARIOS
    //Obtiene horarios del profesor 
    public List<Horario> obtenerHorariosPorIdUsuario(int idUsuario) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_horarios_docente");

        Map<String, Object> params = new HashMap<>();
        params.put("p_id_usuario", idUsuario);

        Map<String, Object> result = call.execute(params);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");
        
        if (rows == null) return new ArrayList<>();

        List<Horario> lista = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Horario h = new Horario();
            h.id_horario = safeInt(row.get("id_horario"));
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
            h.estado = safeStr(row.get("estado"));
            lista.add(h);
        }

        //Añadir reservas del docente (si las hubiera) 
        try {
            //Obtener id_docente
            String findIdDocenteSql = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
            Integer idDocente = jdbcTemplate.queryForObject(findIdDocenteSql, Integer.class, idUsuario);

            if (idDocente != null) {
                String sqlRes = "SELECT numero_salon, dia_semana, hora_inicio, hora_fin, fecha_reserva, estado_reserva FROM reservas_salon WHERE id_docente = ? AND estado_reserva IN ('PENDIENTE','CONFIRMADA')";
                List<Map<String, Object>> reservas = jdbcTemplate.queryForList(sqlRes, new Object[]{idDocente});
                for (Map<String, Object> r : reservas) {
                    Horario hr = new Horario();
                    hr.id_horario = 0; //no es horario fijo
                    hr.grupo_id = 0;
                    hr.codigo_curso = "RESERVA";
                    hr.nombre_curso = "Reserva: " + (r.get("fecha_reserva") != null ? r.get("fecha_reserva").toString() : "");
                    hr.letra_grupo = "";
                    hr.tipo_clase = "RESERVA";
                    hr.ciclo = "";
                    hr.anio = 0;
                    hr.semestre = "";
                    hr.dia_semana = safeStr(r.get("dia_semana"));
                    hr.hora_inicio = safeStr(r.get("hora_inicio"));
                    hr.hora_fin = safeStr(r.get("hora_fin"));
                    hr.numero_salon = safeStr(r.get("numero_salon"));
                    hr.estado = safeStr(r.get("estado_reserva"));
                    lista.add(hr);
                }
            }
        } catch (Exception ex) {
            //Si falla no es crítico para mostrar horarios principales
            ex.printStackTrace();
        }
        return lista;
    }

//Obtener horarios de todos los profesores Y RESERVAS 
public List<Map<String, Object>> obtenerHorariosGenerales() {
    try {
        System.out.println("=== OBTENIENDO HORARIOS GENERALES (COMPLETO) ===");
        
        //Primero obtener horarios fijos
        String sqlHorarios = """
            SELECT 
                h.numero_salon as ambiente,
                h.dia_semana as dia,
                h.hora_inicio as horaInicio,
                h.hora_fin as horaFin,
                COALESCE(c.nombre_curso, 'Clase') as motivo,
                COALESCE(d.apellidos_nombres, 'Docente') as profesor,
                CASE 
                    WHEN g.tipo_clase = 'LABORATORIO' THEN 'LABORATORIO'
                    ELSE 'TEORIA'
                END as tipo_clase,
                'FIJO' as origen
            FROM horarios h
            LEFT JOIN docentes d ON h.id_docente = d.id_docente
            LEFT JOIN grupos_curso g ON h.grupo_id = g.grupo_id
            LEFT JOIN cursos c ON g.codigo_curso = c.codigo_curso
            WHERE h.estado = 'ACTIVO'
        """;
        
        //Luego obtener reservas 
        String sqlReservas = """
            SELECT 
                r.numero_salon as ambiente,
                r.dia_semana as dia,
                r.hora_inicio as horaInicio,
                r.hora_fin as horaFin,
                COALESCE(r.motivo, 'Reserva') as motivo,
                COALESCE(d.apellidos_nombres, 'Docente') as profesor,
                'RESERVA' as tipo_clase,
                'RESERVA' as origen
            FROM reservas_salon r
            LEFT JOIN docentes d ON r.id_docente = d.id_docente
            WHERE r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        """;
        
        List<Map<String, Object>> horariosCompletos = new ArrayList<>();
        
        //Agregar horarios fijos
        List<Map<String, Object>> horariosFijos = jdbcTemplate.queryForList(sqlHorarios);
        System.out.println("✓ Horarios fijos obtenidos: " + horariosFijos.size());        
        //Agregar reservas
        List<Map<String, Object>> reservas = jdbcTemplate.queryForList(sqlReservas);
        System.out.println("✓ Reservas obtenidas: " + reservas.size());
        
        horariosCompletos.addAll(horariosFijos);
        horariosCompletos.addAll(reservas);        
               
        return horariosCompletos;
        
    } catch (Exception e) {
        System.out.println("❌ ERROR al obtener horarios generales: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}

// 6. RESERVAS DEL SALÓN
    //Cuenta reservas confirmadas/pendientes de un docente en la semana de la fecha dada
    public int contarReservasSemanaPorDocente(int idDocente, String fechaReserva) {
        try {
            String weekCountSql = "SELECT COUNT(*) FROM reservas_salon WHERE id_docente = ? AND YEARWEEK(fecha_reserva, 1) = YEARWEEK(?, 1) AND estado_reserva IN ('PENDIENTE','CONFIRMADA')";
            Integer count = jdbcTemplate.queryForObject(weekCountSql, Integer.class, idDocente, fechaReserva);
            return count == null ? 0 : count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

public boolean insertarReserva(int idUsuario, String numeroSalon, String diaSemana, String fechaReserva,
                              String horaInicio, String horaFin, String motivo, String descripcion) {
    try {
        System.out.println("=== INSERTANDO EN BD - VERSIÓN FINAL ===");
        System.out.println("idUsuario recibido: " + idUsuario);
        System.out.println("numeroSalon: " + numeroSalon);
        System.out.println("diaSemana: " + diaSemana);
        System.out.println("horaInicio: " + horaInicio);
        System.out.println("horaFin: " + horaFin);
        System.out.println("motivo: " + motivo);
        System.out.println("descripcion: " + descripcion);
        
        //Obtener id_docente desde id_usuario
        System.out.println("Paso 1: Buscando id_docente para id_usuario: " + idUsuario);
        String findIdDocenteSql = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        Integer idDocente;
        
        try {
            idDocente = jdbcTemplate.queryForObject(findIdDocenteSql, Integer.class, idUsuario);
            System.out.println("✓ id_docente encontrado: " + idDocente);
        } catch (Exception e) {
            System.out.println("✗ ERROR: No se encontró id_docente para id_usuario: " + idUsuario);
            return false;
        }
        
        if (idDocente == null) {
            System.out.println("✗ ERROR: id_docente es null para id_usuario: " + idUsuario);
            return false;
        }
        
        //Verificar que el salón existe
        System.out.println("Paso 2: Verificando salón...");
        String checkSalon = "SELECT COUNT(*) FROM salones WHERE numero_salon = ?";
        Integer countSalon = jdbcTemplate.queryForObject(checkSalon, Integer.class, numeroSalon);
        System.out.println("  Salón " + numeroSalon + " existe: " + (countSalon != null && countSalon > 0));
        
        if (countSalon == null || countSalon == 0) {
            System.out.println("✗ ERROR: Salón no encontrado");
            return false;
        }
        
        //Usar NULL si descripción está vacía
        System.out.println("Paso 3: Preparando datos...");
        String descripcionFinal = (descripcion == null || descripcion.trim().isEmpty()) ? null : descripcion;
        
        //Antes de insertar, comprobar conflictos: 1) horarios fijos en ese salón
        System.out.println("Paso 4: Verificando conflictos en horarios fijos...");
        String conflictHorarioSql = "SELECT COUNT(*) FROM horarios WHERE numero_salon = ? AND dia_semana = ? AND estado = 'ACTIVO' AND hora_inicio < ? AND hora_fin > ?";
        Integer conflictHorario = jdbcTemplate.queryForObject(conflictHorarioSql, Integer.class, numeroSalon, diaSemana, horaFin, horaInicio);
        if (conflictHorario != null && conflictHorario > 0) {
            System.out.println("✗ ERROR: Conflicto con horario fijo en el salón, no se permite reservar");
            return false;
        }

        //1.b) El docente que intenta reservar no puede tener una clase a la misma hora (aunque sea en otro salón)
        System.out.println("Paso 4b: Verificando si el docente tiene horario fijo en esa franja...");
        String conflictDocenteSql = "SELECT COUNT(*) FROM horarios WHERE id_docente = ? AND dia_semana = ? AND estado = 'ACTIVO' AND hora_inicio < ? AND hora_fin > ?";
        Integer conflictDocente = jdbcTemplate.queryForObject(conflictDocenteSql, Integer.class, idDocente, diaSemana, horaFin, horaInicio);
        if (conflictDocente != null && conflictDocente > 0) {
            System.out.println("✗ ERROR: Conflicto con horario fijo del docente (tiene clase en ese horario), no se permite reservar");
            return false;
        }

        //2) conflictos con otras reservas existentes en la misma fecha
        System.out.println("Paso 5: Verificando conflictos con otras reservas para la fecha: " + fechaReserva);
        String conflictReservaSql = "SELECT COUNT(*) FROM reservas_salon WHERE numero_salon = ? AND fecha_reserva = ? AND estado_reserva IN ('PENDIENTE','CONFIRMADA') AND hora_inicio < ? AND hora_fin > ?";
        Integer conflictReserva = jdbcTemplate.queryForObject(conflictReservaSql, Integer.class, numeroSalon, fechaReserva, horaFin, horaInicio);
        if (conflictReserva != null && conflictReserva > 0) {
            System.out.println("✗ ERROR: Conflicto con otra reserva existente en la misma fecha");
            return false;
        }

        //3) Limitar reservas semanales a 2 por docente por semana
        System.out.println("Paso 6: Verificando límite semanal de reservas (2/semana) para el docente...");
        String weekCountSql = "SELECT COUNT(*) FROM reservas_salon WHERE id_docente = ? AND YEARWEEK(fecha_reserva, 1) = YEARWEEK(?, 1) AND estado_reserva IN ('PENDIENTE','CONFIRMADA')";
        Integer weekCount = jdbcTemplate.queryForObject(weekCountSql, Integer.class, idDocente, fechaReserva);
        System.out.println("  Reservas en la misma semana: " + weekCount);
        if (weekCount != null && weekCount >= 2) {
            System.out.println("✗ ERROR: Límite semanal alcanzado (2 reservas/semana) para id_docente=" + idDocente);
            return false;
        }

        System.out.println("Paso 6: Ejecutando INSERT en reservas_salon...");

        //Insert básico en reservas_salon - motivo/descripcion pueden no existir en esquema
        String sqlInsert = "INSERT INTO reservas_salon (numero_salon, id_docente, dia_semana, hora_inicio, hora_fin, fecha_reserva, estado_reserva, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?, 'CONFIRMADA', CURRENT_TIMESTAMP)";

        System.out.println("  SQL (insert): " + sqlInsert);
        System.out.println("  Parámetros: [" + numeroSalon + ", " + idDocente + ", " + diaSemana + ", " + horaInicio + ", " + horaFin + ", " + fechaReserva + "]");

        int affectedRows = jdbcTemplate.update(sqlInsert, numeroSalon, idDocente, diaSemana, horaInicio, horaFin, fechaReserva);
        
        System.out.println("Paso 5: Resultado...");
        System.out.println("  Filas afectadas: " + affectedRows);
        
        if (affectedRows > 0) {
            System.out.println("✓ RESERVA INSERTADA EXITOSAMENTE");
            return true;
        } else {
            System.out.println("✗ ERROR: No se insertó ninguna fila");
            return false;
        }
        
    } catch (Exception e) {
        System.out.println("✗ ERROR EXCEPCIÓN EN INSERTAR RESERVA:");
        System.out.println("  Mensaje: " + e.getMessage());
        System.out.println("  Clase: " + e.getClass().getName());
        if (e.getCause() != null) {
            System.out.println("  Causa: " + e.getCause().getMessage());
        }
        e.printStackTrace();
        return false;
    }
}

//reservas del profesor 
public List<Map<String, Object>> obtenerReservasPorIdDocente(int idUsuario) {
    try {
        System.out.println("=== OBTENIENDO RESERVAS PARA USUARIO: " + idUsuario + " ===");
        
        //Obtener id_docente desde id_usuario
        String findIdDocenteSql = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        Integer idDocente;
        
        try {
            idDocente = jdbcTemplate.queryForObject(findIdDocenteSql, Integer.class, idUsuario);
            System.out.println("✓ id_docente encontrado: " + idDocente);
        } catch (Exception e) {
            System.out.println("✗ ERROR: No se encontró id_docente para id_usuario: " + idUsuario);
            return new ArrayList<>();
        }
        
        //Ahora consultamos la tabla de reservas reales
        String sql = """
            SELECT id_reserva, numero_salon, dia_semana, hora_inicio, hora_fin, fecha_reserva, motivo, descripcion, estado_reserva
            FROM reservas_salon
            WHERE id_docente = ? AND estado_reserva IN ('PENDIENTE','CONFIRMADA')
            ORDER BY fecha_reserva DESC, dia_semana, hora_inicio
        """;
        
        System.out.println("  SQL: " + sql);
        
        List<Map<String, Object>> reservas = jdbcTemplate.query(sql, new Object[]{idDocente}, (rs, rowNum) -> {
            Map<String, Object> reserva = new HashMap<>();
            reserva.put("id_reserva", rs.getInt("id_reserva"));
            reserva.put("numero_salon", rs.getString("numero_salon"));
            reserva.put("dia_semana", rs.getString("dia_semana"));
            reserva.put("hora_inicio", rs.getString("hora_inicio"));
            reserva.put("hora_fin", rs.getString("hora_fin"));
            reserva.put("fecha_reserva", rs.getString("fecha_reserva"));
            reserva.put("motivo", rs.getString("motivo"));
            reserva.put("descripcion", rs.getString("descripcion"));
            reserva.put("estado_reserva", rs.getString("estado_reserva"));
            return reserva;
        });
        
        System.out.println("✓ Reservas encontradas: " + reservas.size());
        return reservas;
        
    } catch (Exception e) {
        System.out.println("✗ ERROR al obtener reservas: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}

//Cancelar reserva 
public boolean cancelarReserva(int idUsuario, String numeroSalon, String diaSemana, String horaInicio, String fechaReserva) {
    try {
        System.out.println("=== CANCELANDO RESERVA ===");
        System.out.println("idUsuario: " + idUsuario);
        System.out.println("numeroSalon: " + numeroSalon);
        System.out.println("diaSemana: " + diaSemana);
        System.out.println("horaInicio: " + horaInicio);
        
        //Obtener id_docente desde id_usuario
        String findIdDocenteSql = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        Integer idDocente;
        
        try {
            idDocente = jdbcTemplate.queryForObject(findIdDocenteSql, Integer.class, idUsuario);
        } catch (Exception e) {
            System.out.println("✗ ERROR: No se encontró id_docente para id_usuario: " + idUsuario);
            return false;
        }
        
        //Cancelar una reserva real 
        String sql = """
            UPDATE reservas_salon
            SET estado_reserva = 'CANCELADA'
            WHERE id_docente = ? AND numero_salon = ? AND dia_semana = ? AND hora_inicio = ? AND fecha_reserva = ? AND estado_reserva IN ('PENDIENTE','CONFIRMADA')
        """;
        
        System.out.println("  SQL: " + sql);
        
        int affectedRows = jdbcTemplate.update(sql, idDocente, numeroSalon, diaSemana, horaInicio, fechaReserva);
        System.out.println("  Filas afectadas: " + affectedRows);
        
        return affectedRows > 0;
    } catch (Exception e) {
        System.out.println("✗ ERROR al cancelar reserva: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

//los ambientes disponibles 
public List<Map<String, Object>> obtenerAmbientes() {
    try {
        System.out.println("🎯 === DAO: OBTENIENDO AMBIENTES DESDE BD ===");        
        //SQL más flexible que incluya diferentes estados activos
        String sql = """
            SELECT 
                numero_salon as id,
                numero_salon as nombre,
                CASE 
                    WHEN tipo_aula_id = 1 THEN 'theory'
                    WHEN tipo_aula_id = 2 THEN 'lab'
                    ELSE 'theory'
                END as tipo,
                capacidad,
                estado
            FROM salones 
            WHERE estado IN ('DISPONIBLE', 'ACTIVO', 'HABILITADO')
            ORDER BY numero_salon
        """;
        
        System.out.println(" SQL ejecutado: " + sql);
        
        List<Map<String, Object>> ambientes = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> ambiente = new HashMap<>();
            ambiente.put("id", rs.getString("id"));
            ambiente.put("nombre", "Ambiente " + rs.getString("nombre"));
            ambiente.put("tipo", rs.getString("tipo"));
            ambiente.put("capacidad", rs.getInt("capacidad"));
            ambiente.put("estado", rs.getString("estado")); 
            
            System.out.println("Ambiente encontrado: " + ambiente.get("id") + 
                             " - Tipo: " + ambiente.get("tipo") + 
                             " - Estado: " + ambiente.get("estado"));
            return ambiente;
        });
        
        System.out.println(" Total ambientes encontrados en BD: " + ambientes.size());
        
        if (ambientes.isEmpty()) {
            System.out.println(" NO HAY AMBIENTES CON ESTADOS ACTIVOS");
            System.out.println(" Estados disponibles en la tabla:");
            
            //Consultar qué estados existen
            String estadosSql = "SELECT DISTINCT estado FROM salones";
            List<String> estados = jdbcTemplate.queryForList(estadosSql, String.class);
            System.out.println(" Estados encontrados: " + estados);
        }
        
        return ambientes;
        
    } catch (Exception e) {
        System.out.println("ERROR en DAO obtenerAmbientes: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}

// 7. SÍLABO Y CONTENIDO en otro archivo...

// 8. REPORTES no tiene nada en el DAO

}