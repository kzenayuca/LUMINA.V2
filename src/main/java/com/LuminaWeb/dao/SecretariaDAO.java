package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import com.LuminaWeb.dao.SecretariaDAO.CursoConLaboratorio;
import com.LuminaWeb.dao.SecretariaDAO.GrupoLaboratorio;
import com.LuminaWeb.dao.SecretariaDAO.PeriodoMatriculaLab;

import java.io.BufferedReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

@Repository
public class SecretariaDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SecretariaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // DTOs SECRETARIA
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

    public static class PeriodoIngresoNotas {
        public int idPeriodo;
        public String codigoCurso;
        public String nombreCurso;
        public Integer tipoEvalId;
        public String nombreEvaluacion;
        public LocalDateTime fechaInicio;
        public LocalDateTime fechaFin;
        public String estado;
        public int creadoPor;
    }

// 1. DASHBOARD (no confundirse por el nombre XD)
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

// 2. SUBIDA DE DATOS
//Procesa archivo Excel/CSV de estudiantes
//Formato: ID, CUI, INICIO, APELLIDOS Y NOMBRES, CORREO
public Map<String, Object> procesarArchivoEstudiantes(MultipartFile file) {
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
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            Workbook workbook = null;
            
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else {
                    workbook = new HSSFWorkbook(is);
                }
                
                Sheet sheet = workbook.getSheetAt(0);
                int rowNum = 0;

                for (Row row : sheet) {
                    rowNum++;
                    
                    //Saltar encabezado
                    if (rowNum == 1) continue;
                    
                    //Verificar fila vacía
                    if (esFilaVacia(row)) continue;

                    try {
                        //Columna 1: CUI
                        String cui = obtenerValorCelda(row.getCell(1));
                        //Columna 2: Año de inicio
                        String anioInicio = obtenerValorCelda(row.getCell(2));
                        //Columna 3: Apellidos y nombres
                        String apellidosNombres = obtenerValorCelda(row.getCell(3));
                        //Columna 4: Correo
                        String correo = obtenerValorCelda(row.getCell(4));

                        if (cui == null || cui.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": CUI vacío");
                            errores++;
                            continue;
                        }

                        cui = cui.trim();
                        
                        if (apellidosNombres == null || apellidosNombres.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Apellidos y nombres vacío");
                            errores++;
                            continue;
                        }

                        if (correo == null || correo.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Correo vacío");
                            errores++;
                            continue;
                        }

                        //La contraseña será el CUI
                        String password = cui;

                        //Crear usuario y estudiante
                        if (insertarEstudiante(cui, apellidosNombres.trim(), correo.trim(), password)) {
                            procesados++;
                        } else {
                            mensajesError.add("Fila " + rowNum + ": Error al insertar estudiante " + cui);
                            errores++;
                        }

                    } catch (Exception e) {
                        mensajesError.add("Fila " + rowNum + ": Error inesperado: " + e.getMessage());
                        errores++;
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

        } else if (filename.endsWith(".csv")) {
            //Procesar CSV (similar a Excel)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                int lineNum = 0;

                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    
                    if (lineNum == 1) continue; //Saltar encabezado
                    if (line.trim().isEmpty()) continue;

                    String[] campos = line.split("[,;\\t]");

                    if (campos.length < 5) {
                        mensajesError.add("Línea " + lineNum + ": Formato incorrecto");
                        errores++;
                        continue;
                    }

                    try {
                        String cui = campos[1].trim();
                        String anioInicio = campos[2].trim();
                        String apellidosNombres = campos[3].trim();
                        String correo = campos[4].trim();

                        if (cui.isEmpty() || apellidosNombres.isEmpty() || correo.isEmpty()) {
                            mensajesError.add("Línea " + lineNum + ": Campos vacíos");
                            errores++;
                            continue;
                        }

                        String password = cui;

                        if (insertarEstudiante(cui, apellidosNombres, correo, password)) {
                            procesados++;
                        } else {
                            mensajesError.add("Línea " + lineNum + ": Error al insertar estudiante " + cui);
                            errores++;
                        }

                    } catch (Exception e) {
                        mensajesError.add("Línea " + lineNum + ": Error: " + e.getMessage());
                        errores++;
                    }
                }
            }
        } else {
            result.put("success", false);
            result.put("mensaje", "Formato no soportado");
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
    }

    return result;
}
//Inserta un estudiante en la base de datos
private boolean insertarEstudiante(String cui, String apellidosNombres, String correo, String password) {
    try {
        //Verificar si ya existe el CUI
        String checkSql = "SELECT COUNT(*) FROM estudiantes WHERE cui = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, cui);
        
        if (count != null && count > 0) {
            return false; //Ya existe
        }

        //Insertar usuario primero
        String sqlUsuario = """
            INSERT INTO usuarios (correo_institucional, password_hash, salt, tipo_id, estado_cuenta, primer_acceso)
            VALUES (?, ?, ?, 1, 'ACTIVO', 1)
        """;
        
        jdbcTemplate.update(sqlUsuario, correo, password, "salt_" + cui);
        
        //Obtener id del usuario recién creado
        Integer idUsuario = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        
        //Generar número de matrícula del CUI
        int numeroMatricula = Integer.parseInt(cui.replaceAll("[^0-9]", ""));
        
        //Insertar estudiante
        String sqlEstudiante = """
            INSERT INTO estudiantes (cui, id_usuario, apellidos_nombres, numero_matricula, estado_estudiante)
            VALUES (?, ?, ?, ?, 'VIGENTE')
        """;
        
        jdbcTemplate.update(sqlEstudiante, cui, idUsuario, apellidosNombres, numeroMatricula);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Procesa archivo Excel/CSV de docentes
/** Formato: Nro, Codigo(id_usuario), Asignatura, Código del curso, Ciclo, Grupo, Apellidos y nombres, Correo, Departamento
 * (SOLO crea/actualiza:)
 * - Usuario y Docente
 * - Curso (SIN detectar laboratorio)
 * - Grupo de TEORIA 
 * La detección de laboratorio y asignación final se hace en HORARIOS */
public Map<String, Object> procesarArchivoDocentes(MultipartFile file) {
    Map<String, Object> result = new HashMap<>();
    int procesados = 0;
    int errores = 0;
    int cursosCreados = 0;
    int gruposCreados = 0;
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
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            Workbook workbook = null;
            
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else {
                    workbook = new HSSFWorkbook(is);
                }
                
                Sheet sheet = workbook.getSheetAt(0);
                int rowNum = 0;

                for (Row row : sheet) {
                    rowNum++;
                    
                    if (rowNum == 1) continue; //Saltar encabezado
                    if (esFilaVacia(row)) continue;

                    try {
                        //Columna 1: Codigo (id_usuario del docente)
                        String codigo = obtenerValorCelda(row.getCell(1));
                        //Columna 2: Asignatura (nombre del curso)
                        String asignatura = obtenerValorCelda(row.getCell(2));
                        //Columna 3: Código del curso
                        String codigoCurso = obtenerValorCelda(row.getCell(3));
                        //Columna 4: Ciclo
                        String ciclo = obtenerValorCelda(row.getCell(4));
                        //Columna 5: Grupo
                        String grupo = obtenerValorCelda(row.getCell(5));
                        //Columna 6: Apellidos y nombres
                        String apellidosNombres = obtenerValorCelda(row.getCell(6));
                        //Columna 7: Correo
                        String correo = obtenerValorCelda(row.getCell(7));
                        //Columna 8: Departamento
                        String departamento = obtenerValorCelda(row.getCell(8));

                        if (codigo == null || codigo.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Código vacío");
                            errores++;
                            continue;
                        }

                        if (apellidosNombres == null || apellidosNombres.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Apellidos y nombres vacío");
                            errores++;
                            continue;
                        }

                        if (correo == null || correo.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Correo vacío");
                            errores++;
                            continue;
                        }

                        //La contraseña será el correo
                        String password = correo.trim();
                        int idUsuario = Integer.parseInt(codigo.trim());

                        //1. Insertar o actualizar docente
                        boolean docenteInsertado = insertarDocente(idUsuario, apellidosNombres.trim(), 
                                          correo.trim(), departamento != null ? departamento.trim() : "General", 
                                          password);
                        
                        if (docenteInsertado) {
                            procesados++;
                        }

                        //2. Crear curso si se proporciona (SIN detectar laboratorio)
                        if (codigoCurso != null && !codigoCurso.trim().isEmpty() && 
                            asignatura != null && !asignatura.trim().isEmpty()) {
                            
                            if (crearCursoSiNoExiste(codigoCurso.trim(), asignatura.trim())) {
                                cursosCreados++;
                            }

                            //3. Crear SOLO grupo de TEORIA (el laboratorio se crea en horarios)
                            if (grupo != null && !grupo.trim().isEmpty()) {
                                if (crearGrupoTeorico(codigoCurso.trim(), grupo.trim())) {
                                    gruposCreados++;
                                }
                            }
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

        } else if (filename.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                int lineNum = 0;

                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    
                    if (lineNum == 1) continue;
                    if (line.trim().isEmpty()) continue;

                    String[] campos = line.split("[,;\\t]");

                    if (campos.length < 9) {
                        mensajesError.add("Línea " + lineNum + ": Formato incorrecto");
                        errores++;
                        continue;
                    }

                    try {
                        String codigo = campos[1].trim();
                        String asignatura = campos[2].trim();
                        String codigoCurso = campos[3].trim();
                        String ciclo = campos[4].trim();
                        String grupo = campos[5].trim();
                        String apellidosNombres = campos[6].trim();
                        String correo = campos[7].trim();
                        String departamento = campos[8].trim();

                        if (codigo.isEmpty() || apellidosNombres.isEmpty() || correo.isEmpty()) {
                            mensajesError.add("Línea " + lineNum + ": Campos vacíos");
                            errores++;
                            continue;
                        }

                        int idUsuario = Integer.parseInt(codigo);

                        if (insertarDocente(idUsuario, apellidosNombres, correo, 
                                          departamento.isEmpty() ? "General" : departamento, correo)) {
                            procesados++;
                        }

                        if (!codigoCurso.isEmpty() && !asignatura.isEmpty()) {
                            if (crearCursoSiNoExiste(codigoCurso, asignatura)) {
                                cursosCreados++;
                            }

                            if (!grupo.isEmpty()) {
                                if (crearGrupoTeorico(codigoCurso, grupo)) {
                                    gruposCreados++;
                                }
                            }
                        }

                    } catch (Exception e) {
                        mensajesError.add("Línea " + lineNum + ": Error: " + e.getMessage());
                        errores++;
                    }
                }
            }
        } else {
            result.put("success", false);
            result.put("mensaje", "Formato no soportado");
            return result;
        }
        
        result.put("success", true);
        result.put("procesados", procesados);
        result.put("errores", errores);
        result.put("cursosCreados", cursosCreados);
        result.put("gruposCreados", gruposCreados);
        result.put("mensajes", mensajesError);

    } catch (Exception e) {
        e.printStackTrace();
        result.put("success", false);
        result.put("mensaje", "Error procesando archivo: " + e.getMessage());
    }

    return result;
}

//Detecta si un curso tiene laboratorio revisando el archivo Excel
private boolean detectarSiTieneLaboratorio(String codigoCurso, Sheet sheet) {
    try {
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; //Saltar encabezado
            
            String codigo = obtenerValorCelda(row.getCell(3));
            String asignatura = obtenerValorCelda(row.getCell(2));
            
            if (codigo != null && codigo.trim().equals(codigoCurso)) {
                if (asignatura != null && 
                    (asignatura.toUpperCase().contains("LABORATORIO") || 
                     asignatura.toUpperCase().contains("LAB"))) {
                    return true;
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}

//Detecta si un curso tiene laboratorio revisando las líneas CSV
private boolean detectarSiTieneLaboratorioCSV(String codigoCurso, List<String> allLines) {
    try {
        for (String line : allLines) {
            String[] campos = line.split("[,;\\t]");
            if (campos.length >= 4) {
                String codigo = campos[3].trim();
                String asignatura = campos[2].trim();
                
                if (codigo.equals(codigoCurso)) {
                    if (asignatura.toUpperCase().contains("LABORATORIO") || 
                        asignatura.toUpperCase().contains("LAB")) {
                        return true;
                    }
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}

//Crea o actualiza un curso con detección de laboratorio
private boolean crearOActualizarCurso(String codigoCurso, String nombreCurso, boolean tieneLab) {
    try {
        //Verificar si existe el curso
        String checkSql = "SELECT COUNT(*) FROM cursos WHERE codigo_curso = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, codigoCurso);
        
        if (count != null && count > 0) {
            //Actualizar curso existente
            String updateSql = """
                UPDATE cursos 
                SET nombre_curso = ?, tiene_laboratorio = ?
                WHERE codigo_curso = ?
            """;
            jdbcTemplate.update(updateSql, nombreCurso, tieneLab ? 1 : 0, codigoCurso);
            return false; //Ya existía
        }
        
        //Insertar nuevo curso
        String sqlCurso = """
            INSERT INTO cursos (codigo_curso, nombre_curso, tiene_laboratorio, 
                              numero_grupos_teoria, numero_grupos_laboratorio, estado)
            VALUES (?, ?, ?, 2, ?, 'ACTIVO')
        """;
        
        jdbcTemplate.update(sqlCurso, codigoCurso, nombreCurso, tieneLab ? 1 : 0, tieneLab ? 2 : 0);
        return true; //Creado nuevo
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Asigna un docente a un curso creando el grupo correspondiente
private boolean asignarDocenteACurso(int idUsuario, String codigoCurso, String nombreCurso, 
                                     String letraGrupo, String tipoClase) {
    try {
        //Obtener id_docente
        String sqlIdDocente = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        List<Integer> idsDocente = jdbcTemplate.queryForList(sqlIdDocente, new Object[]{idUsuario}, Integer.class);
        if (idsDocente.isEmpty()) return false;
        int idDocente = idsDocente.get(0);

        //Obtener ciclo activo
        String sqlCiclo = "SELECT id_ciclo FROM ciclos_academicos WHERE estado = 'ACTIVO' LIMIT 1";
        List<Integer> ciclos = jdbcTemplate.queryForList(sqlCiclo, Integer.class);
        if (ciclos.isEmpty()) return false;
        int idCiclo = ciclos.get(0);

        //Buscar o crear grupo
        String checkGrupoSql = """
            SELECT grupo_id FROM grupos_curso 
            WHERE codigo_curso = ? AND id_ciclo = ? AND letra_grupo = ? AND tipo_clase = ?
        """;
        List<Integer> grupoIds = jdbcTemplate.queryForList(checkGrupoSql, 
            new Object[]{codigoCurso, idCiclo, letraGrupo, tipoClase}, Integer.class);
        
        int grupoId;
        if (grupoIds.isEmpty()) {
            //Crear grupo
            String sqlGrupo = """
                INSERT INTO grupos_curso (codigo_curso, id_ciclo, letra_grupo, tipo_clase, 
                                        capacidad_maxima, estado)
                VALUES (?, ?, ?, ?, ?, 'ACTIVO')
            """;
            int capacidad = tipoClase.equals("LABORATORIO") ? 20 : 40;
            jdbcTemplate.update(sqlGrupo, codigoCurso, idCiclo, letraGrupo, tipoClase, capacidad);
            grupoId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        } else {
            grupoId = grupoIds.get(0);
        }

        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Inserta un docente en la base de datos (contraseña=correo)
private boolean insertarDocente(int idUsuario, String apellidosNombres, String correo, 
                                String departamento, String password) {
    try {
        //Verificar si ya existe el usuario
        String checkUsuarioSql = "SELECT COUNT(*) FROM usuarios WHERE id_usuario = ?";
        Integer countUsuario = jdbcTemplate.queryForObject(checkUsuarioSql, Integer.class, idUsuario);
        
        if (countUsuario == null || countUsuario == 0) {
            //Insertar usuario
            String sqlUsuario = """
                INSERT INTO usuarios (id_usuario, correo_institucional, password_hash, salt, tipo_id, estado_cuenta, primer_acceso)
                VALUES (?, ?, ?, ?, 2, 'ACTIVO', 0)
            """;
            
            jdbcTemplate.update(sqlUsuario, idUsuario, correo, password, "salt_" + idUsuario);
        } else {
            //Actualizar correo si cambió
            String updateUsuarioSql = """
                UPDATE usuarios 
                SET correo_institucional = ?, password_hash = ?
                WHERE id_usuario = ?
            """;
            jdbcTemplate.update(updateUsuarioSql, correo, password, idUsuario);
        }

        //Verificar si ya existe el docente
        String checkSql = "SELECT COUNT(*) FROM docentes WHERE id_usuario = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, idUsuario);
        
        if (count != null && count > 0) {
            //Actualizar docente existente
            String updateSql = """
                UPDATE docentes 
                SET apellidos_nombres = ?, departamento = ?
                WHERE id_usuario = ?
            """;
            jdbcTemplate.update(updateSql, apellidosNombres, departamento, idUsuario);
            return false; //No es nuevo
        } else {
            //Insertar nuevo docente
            String sqlDocente = """
                INSERT INTO docentes (id_usuario, apellidos_nombres, departamento, es_responsable_teoria)
                VALUES (?, ?, ?, 0)
            """;
            
            jdbcTemplate.update(sqlDocente, idUsuario, apellidosNombres, departamento);
            return true; //Es nuevo
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Crea un curso si no existe
private boolean crearCursoSiNoExiste(String codigoCurso, String nombreCurso) {
    try {
        //Verificar si existe el curso
        String checkSql = "SELECT COUNT(*) FROM cursos WHERE codigo_curso = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, codigoCurso);
        
        if (count != null && count > 0) {
            //Actualizar solo el nombre si cambió
            String updateSql = """
                UPDATE cursos 
                SET nombre_curso = ?
                WHERE codigo_curso = ?
            """;
            jdbcTemplate.update(updateSql, nombreCurso, codigoCurso);
            return false; //Ya existía
        }
        
        //Insertar nuevo curso (tiene_laboratorio = 0 por defecto)
        String sqlCurso = """
            INSERT INTO cursos (codigo_curso, nombre_curso, tiene_laboratorio, 
                              numero_grupos_teoria, numero_grupos_laboratorio, estado)
            VALUES (?, ?, 0, 2, 0, 'ACTIVO')
        """;
        
        jdbcTemplate.update(sqlCurso, codigoCurso, nombreCurso);
        return true; //Creado nuevo
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Crea SOLO el grupo de TEORIA si no existe
private boolean crearGrupoTeorico(String codigoCurso, String letraGrupo) {
    try {
        //Obtener ciclo activo
        String sqlCiclo = "SELECT id_ciclo FROM ciclos_academicos WHERE estado = 'ACTIVO' LIMIT 1";
        List<Integer> ciclos = jdbcTemplate.queryForList(sqlCiclo, Integer.class);
        if (ciclos.isEmpty()) return false;
        int idCiclo = ciclos.get(0);

        //Verificar si ya existe el grupo de TEORIA
        String checkSql = """
            SELECT COUNT(*) FROM grupos_curso 
            WHERE codigo_curso = ? AND id_ciclo = ? AND letra_grupo = ? AND tipo_clase = 'TEORIA'
        """;
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, 
            codigoCurso, idCiclo, letraGrupo);
        
        if (count != null && count > 0) {
            return false; //Ya existe
        }

        //Crear grupo de TEORIA
        String sqlGrupo = """
            INSERT INTO grupos_curso (codigo_curso, id_ciclo, letra_grupo, tipo_clase, 
                                    capacidad_maxima, estado)
            VALUES (?, ?, ?, 'TEORIA', 40, 'ACTIVO')
        """;
        jdbcTemplate.update(sqlGrupo, codigoCurso, idCiclo, letraGrupo);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Asigna un docente a un curso SIN crear horarios placeholder
private boolean asignarDocenteACursoSinHorario(int idUsuario, String codigoCurso, String nombreCurso, String letraGrupo) {
    try {
        //Obtener id_docente
        String sqlIdDocente = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        List<Integer> idsDocente = jdbcTemplate.queryForList(sqlIdDocente, new Object[]{idUsuario}, Integer.class);
        if (idsDocente.isEmpty()) return false;
        int idDocente = idsDocente.get(0);

        //Obtener ciclo activo
        String sqlCiclo = "SELECT id_ciclo FROM ciclos_academicos WHERE estado = 'ACTIVO' LIMIT 1";
        List<Integer> ciclos = jdbcTemplate.queryForList(sqlCiclo, Integer.class);
        if (ciclos.isEmpty()) return false;
        int idCiclo = ciclos.get(0);

        //Buscar / crear grupo TEORIA
        String checkGrupoSql = """
            SELECT grupo_id FROM grupos_curso 
            WHERE codigo_curso = ? AND id_ciclo = ? AND letra_grupo = ? AND tipo_clase = 'TEORIA'
        """;
        List<Integer> grupoIds = jdbcTemplate.queryForList(checkGrupoSql, 
            new Object[]{codigoCurso, idCiclo, letraGrupo}, Integer.class);
        
        int grupoIdTeoria;
        if (grupoIds.isEmpty()) {
            String sqlGrupo = """
                INSERT INTO grupos_curso (codigo_curso, id_ciclo, letra_grupo, tipo_clase, capacidad_maxima, estado)
                VALUES (?, ?, ?, 'TEORIA', 40, 'ACTIVO')
            """;
            jdbcTemplate.update(sqlGrupo, codigoCurso, idCiclo, letraGrupo);
            grupoIdTeoria = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        } else {
            grupoIdTeoria = grupoIds.get(0);
        }

        //Si el curso tiene laboratorio, también crear grupo LABORATORIO
        String chkTieneLab = "SELECT tiene_laboratorio FROM cursos WHERE codigo_curso = ? LIMIT 1";
        Integer tieneLab = jdbcTemplate.queryForObject(chkTieneLab, Integer.class, codigoCurso);
        
        if (tieneLab != null && tieneLab == 1) {
            String sqlLabGrupo = """
                SELECT grupo_id FROM grupos_curso 
                WHERE codigo_curso = ? AND id_ciclo = ? AND letra_grupo = ? AND tipo_clase = 'LABORATORIO'
                LIMIT 1
            """;
            List<Integer> labIds = jdbcTemplate.queryForList(sqlLabGrupo, 
                new Object[]{codigoCurso, idCiclo, letraGrupo}, Integer.class);
            
            if (labIds.isEmpty()) {
                //Crear grupo laboratorio si no existe
                String sqlCrearLab = """
                    INSERT INTO grupos_curso (codigo_curso, id_ciclo, letra_grupo, tipo_clase, capacidad_maxima, estado)
                    VALUES (?, ?, ?, 'LABORATORIO', 20, 'ACTIVO')
                """;
                jdbcTemplate.update(sqlCrearLab, codigoCurso, idCiclo, letraGrupo);
            }
        }

        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}


//Procesa archivo Excel/CSV de alumnos por asignatura
//Formato: Nro, CUI, Apellidos y Nombres
public Map<String, Object> procesarArchivoAlumnosAsignatura(MultipartFile file) {
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
        String nombreCurso = null;
        String grupo = null;
        Integer grupoId = null;

        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            Workbook workbook = null;
            
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else {
                    workbook = new HSSFWorkbook(is);
                }
                
                Sheet sheet = workbook.getSheetAt(0);
                
                //Leer fila 5 (índice 4) para obtener el curso
                Row rowCurso = sheet.getRow(4);
                if (rowCurso != null) {
                    String asignatura = obtenerValorCelda(rowCurso.getCell(0));
                    if (asignatura != null && asignatura.contains(":")) {
                        nombreCurso = asignatura.split(":")[1].trim();
                    }
                }

                //Leer fila 6 (índice 5) para obtener el grupo
                Row rowGrupo = sheet.getRow(5);
                if (rowGrupo != null) {
                    String cicloCampo = obtenerValorCelda(rowGrupo.getCell(0));
                    if (cicloCampo != null && cicloCampo.contains("GRUPO")) {
                        String[] parts = cicloCampo.split("GRUPO");
                        if (parts.length > 1) {
                            String grupoStr = parts[1].trim();
                            grupo = grupoStr.split(" ")[0].substring(0, 1); //Primera letra
                        }
                    }
                }

                if (nombreCurso == null || grupo == null) {
                    mensajesError.add("No se pudo determinar el curso o grupo del archivo");
                    mensajesError.add("Curso encontrado: " + nombreCurso);
                    mensajesError.add("Grupo encontrado: " + grupo);
                    result.put("success", false);
                    result.put("mensaje", "Formato de archivo incorrecto");
                    result.put("mensajes", mensajesError);
                    return result;
                }

                //Buscar el grupo_id correspondiente
                grupoId = obtenerGrupoId(nombreCurso, grupo);
                
                if (grupoId == null) {
                    mensajesError.add("No se encontró el grupo para el curso: " + nombreCurso + " Grupo: " + grupo);
                    result.put("success", false);
                    result.put("mensaje", "Grupo no encontrado");
                    result.put("mensajes", mensajesError);
                    return result;
                }

                //Procesar estudiantes (empezar desde fila 10, índice 9)
                int rowNum = 0;
                for (Row row : sheet) {
                    rowNum++;
                    
                    if (rowNum <= 10) continue; //Saltar encabezados y metadata
                    if (esFilaVacia(row)) continue;

                    try {
                        //Columna 1: CUI
                        String cui = obtenerValorCelda(row.getCell(1));

                        if (cui == null || cui.trim().isEmpty()) {
                            continue; //Saltar filas sin CUI
                        }

                        cui = cui.trim();

                        if (insertarMatricula(cui, grupoId)) {
                            procesados++;
                        } else {
                            mensajesError.add("Fila " + rowNum + ": Error al matricular estudiante " + cui);
                            errores++;
                        }

                    } catch (Exception e) {
                        mensajesError.add("Fila " + rowNum + ": Error inesperado: " + e.getMessage());
                        errores++;
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

        } else if (filename.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                int lineNum = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    
                    //Leer curso de línea 5
                    if (lineNum == 5) {
                        if (line.contains(":")) {
                            nombreCurso = line.split(":")[1].trim();
                        }
                    }
                    
                    //Leer grupo de línea 6
                    if (lineNum == 6) {
                        if (line.contains("GRUPO ")) {
                            String[] parts = line.split("GRUPO ");
                            if (parts.length > 1) {
                                String grupoStr = parts[1].trim();
                                grupo = grupoStr.split(" ")[0].substring(0, 1);
                            }
                        }
                    }
                    
                    if (lineNum <= 10) continue;
                    if (line.trim().isEmpty()) continue;

                    if (grupoId == null && nombreCurso != null && grupo != null) {
                        grupoId = obtenerGrupoId(nombreCurso, grupo);
                        if (grupoId == null) {
                            mensajesError.add("No se encontró el grupo para el curso: " + nombreCurso + " Grupo: " + grupo);
                            result.put("success", false);
                            result.put("mensaje", "Grupo no encontrado");
                            result.put("mensajes", mensajesError);
                            return result;
                        }
                    }

                    String[] campos = line.split("[,;\\t]");
                    if (campos.length < 2) continue;

                    try {
                        String cui = campos[1].trim();
                        
                        if (cui.isEmpty()) continue;

                        if (insertarMatricula(cui, grupoId)) {
                            procesados++;
                        } else {
                            mensajesError.add("Línea " + lineNum + ": Error al matricular estudiante " + cui);
                            errores++;
                        }

                    } catch (Exception e) {
                        mensajesError.add("Línea " + lineNum + ": Error: " + e.getMessage());
                        errores++;
                    }
                }
            }
        } else {
            result.put("success", false);
            result.put("mensaje", "Formato no soportado");
            return result;
        }
        
        result.put("success", true);
        result.put("procesados", procesados);
        result.put("errores", errores);
        result.put("mensajes", mensajesError);
        result.put("curso", nombreCurso);
        result.put("grupo", grupo);

    } catch (Exception e) {
        e.printStackTrace();
        result.put("success", false);
        result.put("mensaje", "Error procesando archivo: " + e.getMessage());
    }

    return result;
}

//Obtiene el grupo_id basado en el nombre del curso y letra de grupo
private Integer obtenerGrupoId(String nombreCurso, String letraGrupo) {
    try {
        String sql = """
            SELECT g.grupo_id
            FROM grupos_curso g
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            WHERE c.nombre_curso = ?
              AND g.letra_grupo = ?
              AND g.tipo_clase = 'TEORIA'
              AND ca.estado = 'ACTIVO'
            LIMIT 1
        """;
        
        List<Integer> ids = jdbcTemplate.queryForList(sql, new Object[]{nombreCurso, letraGrupo}, Integer.class);
        return ids.isEmpty() ? null : ids.get(0);
        
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

//Inserta una matrícula
private boolean insertarMatricula(String cui, int grupoId) {
    try {
        //Verificar si ya existe la matrícula
        String checkSql = """
            SELECT COUNT(*) FROM matriculas 
            WHERE cui = ? AND grupo_id = ?
        """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, cui, grupoId);
        
        if (count != null && count > 0) {
            return false; //Ya existe
        }

        //Obtener número de matrícula del estudiante
        String getNumeroSql = "SELECT numero_matricula FROM estudiantes WHERE cui = ?";
        Integer numeroMatricula = jdbcTemplate.queryForObject(getNumeroSql, Integer.class, cui);
        
        if (numeroMatricula == null) {
            return false; //Estudiante no existe
        }

        //Insertar matrícula
        String sqlMatricula = """
            INSERT INTO matriculas (cui, grupo_id, numero_matricula, prioridad_matricula, estado_matricula)
            VALUES (?, ?, ?, 0, 'ACTIVO')
        """;
        
        jdbcTemplate.update(sqlMatricula, cui, grupoId, numeroMatricula);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Procesa archivo de horarios
 /* Formato: Nro, Ciclo, Curso, Día, Hora de Inicio, Hora de Fin, Tipo de Clase, Grupo, Número de Salón, ID_USUARIO
 * - Detecta si el curso tiene laboratorio (si encuentra filas con LABORATORIO)
 * - Crea grupos de LABORATORIO si no existen
 * - Asigna docentes a los horarios según ID_USUARIO
 * - Crea los horarios */
public Map<String, Object> procesarArchivoHorarios(MultipartFile file) {
    Map<String, Object> result = new HashMap<>();
    int procesados = 0;
    int errores = 0;
    int gruposLabCreados = 0;
    int cursosActualizados = 0;
    List<String> mensajesError = new ArrayList<>();
    
    //Map para trackear cursos con laboratorio
    Map<String, Boolean> cursosConLab = new HashMap<>();

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
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            Workbook workbook = null;
            
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else {
                    workbook = new HSSFWorkbook(is);
                }
                
                Sheet sheet = workbook.getSheetAt(0);
                
                //1: Detectar cursos con laboratorio y actualizar tabla cursos
                int rowNum = 0;
                for (Row row : sheet) {
                    rowNum++;
                    if (rowNum == 1) continue;
                    if (esFilaVacia(row)) continue;

                    String nombreCurso = obtenerValorCelda(row.getCell(2));
                    String tipoClase = obtenerValorCelda(row.getCell(6));
                    
                    if (nombreCurso != null && tipoClase != null) {
                        tipoClase = tipoClase.trim().toUpperCase();
                        if (tipoClase.contains("LAB")) {
                            cursosConLab.put(nombreCurso.trim(), true);
                        }
                    }
                }
                
                //Actualizar cursos con laboratorio detectados
                for (String nombreCurso : cursosConLab.keySet()) {
                    if (actualizarCursoConLaboratorio(nombreCurso)) {
                        cursosActualizados++;
                    }
                }
                
                //2: Crear grupos de laboratorio y horarios
                rowNum = 0;
                for (Row row : sheet) {
                    rowNum++;
                    
                    if (rowNum == 1) continue;
                    if (esFilaVacia(row)) continue;

                    try {
                        //Columna 1: Ciclo
                        String ciclo = obtenerValorCelda(row.getCell(1));
                        //Columna 2: Curso
                        String nombreCurso = obtenerValorCelda(row.getCell(2));
                        //Columna 3: Día
                        String dia = obtenerValorCelda(row.getCell(3));
                        //Columna 4: Hora de Inicio
                        String horaInicio = obtenerValorCelda(row.getCell(4));
                        //Columna 5: Hora de Fin
                        String horaFin = obtenerValorCelda(row.getCell(5));
                        //Columna 6: Tipo de Clase
                        String tipoClase = obtenerValorCelda(row.getCell(6));
                        //Columna 7: Grupo
                        String grupo = obtenerValorCelda(row.getCell(7));
                        //Columna 8: Número de Salón
                        String numeroSalon = obtenerValorCelda(row.getCell(8));
                        //Columna 9: ID_USUARIO (del docente)
                        String idUsuarioStr = obtenerValorCelda(row.getCell(9));

                        //Validaciones
                        if (nombreCurso == null || nombreCurso.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Curso vacío");
                            errores++;
                            continue;
                        }

                        if (dia == null || dia.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Día vacío");
                            errores++;
                            continue;
                        }

                        if (horaInicio == null || horaInicio.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Hora de inicio vacía");
                            errores++;
                            continue;
                        }

                        if (horaFin == null || horaFin.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Hora de fin vacía");
                            errores++;
                            continue;
                        }

                        if (tipoClase == null || tipoClase.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Tipo de clase vacío");
                            errores++;
                            continue;
                        }

                        if (grupo == null || grupo.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Grupo vacío");
                            errores++;
                            continue;
                        }

                        if (numeroSalon == null || numeroSalon.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Número de salón vacío");
                            errores++;
                            continue;
                        }

                        if (idUsuarioStr == null || idUsuarioStr.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": ID_USUARIO vacío");
                            errores++;
                            continue;
                        }

                        //Parsear ID_USUARIO
                        Integer idUsuario = null;
                        try {
                            idUsuario = Integer.parseInt(idUsuarioStr.trim());
                        } catch (NumberFormatException e) {
                            mensajesError.add("Fila " + rowNum + ": ID_USUARIO inválido");
                            errores++;
                            continue;
                        }

                        //Normalizar día
                        dia = normalizarDia(dia.trim().toUpperCase());
                        
                        //Normalizar tipo de clase
                        tipoClase = tipoClase.trim().toUpperCase();
                        if (tipoClase.contains("LAB")) {
                            tipoClase = "LABORATORIO";
                            
                            //Crear grupo de laboratorio si no existe
                            if (crearGrupoLaboratorio(nombreCurso.trim(), grupo.trim())) {
                                gruposLabCreados++;
                            }
                        } else {
                            tipoClase = "TEORIA";
                        }

                        //Formatear horas
                        horaInicio = formatearHora(horaInicio.trim());
                        horaFin = formatearHora(horaFin.trim());

                        //Insertar horario con docente asignado
                        if (insertarHorarioConDocente(nombreCurso.trim(), dia, horaInicio, horaFin, 
                                          tipoClase, grupo.trim(), numeroSalon.trim(), idUsuario)) {
                            procesados++;
                        } else {
                            mensajesError.add("Fila " + rowNum + ": Error al insertar horario");
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

        } else if (filename.endsWith(".csv")) {
            //Similar para CSV
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                List<String> allLines = new ArrayList<>();
                String line;
                
                //Leer todas las líneas
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
                
                //1: Detectar cursos con laboratorio
                for (int i = 1; i < allLines.size(); i++) {
                    String currentLine = allLines.get(i);
                    if (currentLine.trim().isEmpty()) continue;
                    
                    String[] campos = currentLine.split("[,;\\t]");
                    if (campos.length >= 7) {
                        String nombreCurso = campos[2].trim();
                        String tipoClase = campos[6].trim().toUpperCase();
                        
                        if (tipoClase.contains("LAB")) {
                            cursosConLab.put(nombreCurso, true);
                        }
                    }
                }
                
                //Actualizar cursos
                for (String nombreCurso : cursosConLab.keySet()) {
                    if (actualizarCursoConLaboratorio(nombreCurso)) {
                        cursosActualizados++;
                    }
                }
                
                //2: Procesar horarios
                int lineNum = 0;
                for (String currentLine : allLines) {
                    lineNum++;
                    
                    if (lineNum == 1) continue;
                    if (currentLine.trim().isEmpty()) continue;

                    String[] campos = currentLine.split("[,;\\t]");

                    if (campos.length < 10) {
                        mensajesError.add("Línea " + lineNum + ": Formato incorrecto");
                        errores++;
                        continue;
                    }

                    try {
                        String ciclo = campos[1].trim();
                        String nombreCurso = campos[2].trim();
                        String dia = normalizarDia(campos[3].trim().toUpperCase());
                        String horaInicio = formatearHora(campos[4].trim());
                        String horaFin = formatearHora(campos[5].trim());
                        String tipoClase = campos[6].trim().toUpperCase();
                        String grupo = campos[7].trim();
                        String numeroSalon = campos[8].trim();
                        
                        Integer idUsuario = null;
                        try {
                            idUsuario = Integer.parseInt(campos[9].trim());
                        } catch (NumberFormatException e) {
                            mensajesError.add("Línea " + lineNum + ": ID_USUARIO inválido");
                            errores++;
                            continue;
                        }

                        if (nombreCurso.isEmpty() || dia.isEmpty() || grupo.isEmpty() || 
                            numeroSalon.isEmpty() || idUsuario == null) {
                            mensajesError.add("Línea " + lineNum + ": Campos vacíos");
                            errores++;
                            continue;
                        }

                        if (tipoClase.contains("LAB")) {
                            tipoClase = "LABORATORIO";
                            if (crearGrupoLaboratorio(nombreCurso, grupo)) {
                                gruposLabCreados++;
                            }
                        } else {
                            tipoClase = "TEORIA";
                        }

                        if (insertarHorarioConDocente(nombreCurso, dia, horaInicio, horaFin, 
                                          tipoClase, grupo, numeroSalon, idUsuario)) {
                            procesados++;
                        } else {
                            mensajesError.add("Línea " + lineNum + ": Error al insertar horario");
                            errores++;
                        }

                    } catch (Exception e) {
                        mensajesError.add("Línea " + lineNum + ": Error: " + e.getMessage());
                        errores++;
                    }
                }
            }
        } else {
            result.put("success", false);
            result.put("mensaje", "Formato no soportado");
            return result;
        }
        
        result.put("success", true);
        result.put("procesados", procesados);
        result.put("errores", errores);
        result.put("gruposLabCreados", gruposLabCreados);
        result.put("cursosActualizados", cursosActualizados);
        result.put("mensajes", mensajesError);

    } catch (Exception e) {
        e.printStackTrace();
        result.put("success", false);
        result.put("mensaje", "Error procesando archivo: " + e.getMessage());
    }

    return result;
}

//Actualiza un curso para indicar que tiene laboratorio
private boolean actualizarCursoConLaboratorio(String nombreCurso) {
    try {
        String sql = """
            UPDATE cursos 
            SET tiene_laboratorio = 1, numero_grupos_laboratorio = 2
            WHERE nombre_curso = ?
        """;
        
        int rows = jdbcTemplate.update(sql, nombreCurso);
        return rows > 0;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Crea un grupo de LABORATORIO si no existe
private boolean crearGrupoLaboratorio(String nombreCurso, String letraGrupo) {
    try {
        //obtener código del curso
        String sqlCodigo = "SELECT codigo_curso FROM cursos WHERE nombre_curso = ? LIMIT 1";
        List<String> codigos = jdbcTemplate.queryForList(sqlCodigo, new Object[]{nombreCurso}, String.class);
        if (codigos.isEmpty()) return false;
        String codigoCurso = codigos.get(0);
        
        //Obtener ciclo activo
        String sqlCiclo = "SELECT id_ciclo FROM ciclos_academicos WHERE estado = 'ACTIVO' LIMIT 1";
        List<Integer> ciclos = jdbcTemplate.queryForList(sqlCiclo, Integer.class);
        if (ciclos.isEmpty()) return false;
        int idCiclo = ciclos.get(0);

        //Verificar si ya existe el grupo de LABORATORIO
        String checkSql = """
            SELECT COUNT(*) FROM grupos_curso 
            WHERE codigo_curso = ? AND id_ciclo = ? AND letra_grupo = ? AND tipo_clase = 'LABORATORIO'
        """;
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, 
            codigoCurso, idCiclo, letraGrupo);
        
        if (count != null && count > 0) {
            return false; //Ya existe
        }

        //Crear grupo de LABORATORIO
        String sqlGrupo = """
            INSERT INTO grupos_curso (codigo_curso, id_ciclo, letra_grupo, tipo_clase, 
                                    capacidad_maxima, estado)
            VALUES (?, ?, ?, 'LABORATORIO', 20, 'ACTIVO')
        """;
        jdbcTemplate.update(sqlGrupo, codigoCurso, idCiclo, letraGrupo);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Normaliza el día de la semana
private String normalizarDia(String dia) {
    if (dia.contains("LUN")) return "LUNES";
    if (dia.contains("MAR")) return "MARTES";
    if (dia.contains("MIE") || dia.contains("MIÉ")) return "MIERCOLES";
    if (dia.contains("JUE")) return "JUEVES";
    if (dia.contains("VIE")) return "VIERNES";
    return dia;
}

//Formatea la hora al formato HH:MM:SS
private String formatearHora(String hora) {
    //Si ya tiene el formato correcto
    if (hora.matches("\\d{2}:\\d{2}:\\d{2}")) {
        return hora;
    }
    
    //Si tiene formato HH:MM
    if (hora.matches("\\d{2}:\\d{2}")) {
        return hora + ":00";
    }
    
    //Si tiene formato H:MM
    if (hora.matches("\\d{1}:\\d{2}")) {
        return "0" + hora + ":00";
    }
    
    //Si solo tiene números (HHMM)
    if (hora.matches("\\d{4}")) {
        return hora.substring(0, 2) + ":" + hora.substring(2, 4) + ":00";
    }
    
    return hora + ":00";
}

//Inserta un horario en la base de datos
private boolean insertarHorario(String nombreCurso, String dia, String horaInicio, 
                                String horaFin, String tipoClase, String grupo, String numeroSalon) {
    try {
        //Buscar el grupo_id con el tipo de clase correcto
        String sqlGrupo = """
            SELECT g.grupo_id
            FROM grupos_curso g
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            WHERE c.nombre_curso = ?
              AND g.letra_grupo = ?
              AND g.tipo_clase = ?
              AND ca.estado = 'ACTIVO'
            LIMIT 1
        """;
        
        List<Integer> grupoIds = jdbcTemplate.queryForList(sqlGrupo, 
            new Object[]{nombreCurso, grupo, tipoClase}, Integer.class);
        
        if (grupoIds.isEmpty()) {
            return false; //Grupo no encontrado
        }
        
        Integer grupoId = grupoIds.get(0);

        //Verificar si ya existe el horario
        String checkSql = """
            SELECT COUNT(*) FROM horarios 
            WHERE grupo_id = ? AND dia_semana = ? AND hora_inicio = ?
        """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, grupoId, dia, horaInicio);
        
        if (count != null && count > 0) {
            return false; //Ya existe
        }

        //Buscar docente del curso con ese grupo y tipo de clase
        Integer idDocente = obtenerDocenteDelGrupo(nombreCurso, grupo, tipoClase);

        //Insertar horario
        String sqlHorario = """
            INSERT INTO horarios (grupo_id, numero_salon, dia_semana, hora_inicio, hora_fin, id_docente, estado)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
        """;
        
        jdbcTemplate.update(sqlHorario, grupoId, numeroSalon, dia, horaInicio, horaFin, idDocente);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Inserta un horario con docente asignado según ID_USUARIO
private boolean insertarHorarioConDocente(String nombreCurso, String dia, String horaInicio, 
                                String horaFin, String tipoClase, String grupo, 
                                String numeroSalon, Integer idUsuario) {
    try {
        //Buscar el grupo_id con el tipo de clase correcto
        String sqlGrupo = """
            SELECT g.grupo_id
            FROM grupos_curso g
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            INNER JOIN ciclos_academicos ca ON g.id_ciclo = ca.id_ciclo
            WHERE c.nombre_curso = ?
              AND g.letra_grupo = ?
              AND g.tipo_clase = ?
              AND ca.estado = 'ACTIVO'
            LIMIT 1
        """;
        
        List<Integer> grupoIds = jdbcTemplate.queryForList(sqlGrupo, 
            new Object[]{nombreCurso, grupo, tipoClase}, Integer.class);
        
        if (grupoIds.isEmpty()) {
            return false; //Grupo no encontrado
        }
        
        Integer grupoId = grupoIds.get(0);

        //Verificar si ya existe el horario
        String checkSql = """
            SELECT COUNT(*) FROM horarios 
            WHERE grupo_id = ? AND dia_semana = ? AND hora_inicio = ?
        """;
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, grupoId, dia, horaInicio);
        
        if (count != null && count > 0) {
            return false; //Ya existe
        }

        //Obtener id_docente desde id_usuario
        String sqlDocente = "SELECT id_docente FROM docentes WHERE id_usuario = ?";
        List<Integer> docenteIds = jdbcTemplate.queryForList(sqlDocente, 
            new Object[]{idUsuario}, Integer.class);
        
        if (docenteIds.isEmpty()) {
            return false; //Docente no encontrado
        }
        
        Integer idDocente = docenteIds.get(0);

        //Insertar horario
        String sqlHorario = """
            INSERT INTO horarios (grupo_id, numero_salon, dia_semana, hora_inicio, hora_fin, id_docente, estado)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
        """;
        
        jdbcTemplate.update(sqlHorario, grupoId, numeroSalon, dia, horaInicio, horaFin, idDocente);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Obtiene el id_docente asignado a un curso (si existe)
private Integer obtenerDocenteDelGrupo(String nombreCurso, String letraGrupo, String tipoClase) {
    try {
        //Si es laboratorio, buscar en grupo de laboratorio, sino en teoría
        String sql = """
            SELECT DISTINCT d.id_docente
            FROM docentes d
            INNER JOIN horarios h ON d.id_docente = h.id_docente
            INNER JOIN grupos_curso g ON h.grupo_id = g.grupo_id
            INNER JOIN cursos c ON g.codigo_curso = c.codigo_curso
            WHERE c.nombre_curso = ?
              AND g.letra_grupo = ?
              AND g.tipo_clase = 'TEORIA'
            LIMIT 1
        """;
        
        List<Integer> ids = jdbcTemplate.queryForList(sql, 
            new Object[]{nombreCurso, letraGrupo}, Integer.class);
        
        return ids.isEmpty() ? null : ids.get(0);
        
    } catch (Exception e) {
        return null;
    }
}

//Procesa archivo Excel/CSV de pesos de evaluación
//Formato: Nro, Curso, EP1, EP2, EP3, EC1, EC2, EC3, Total
public Map<String, Object> procesarArchivoPesosEvaluacion(MultipartFile file) {
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
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            Workbook workbook = null;
            
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else {
                    workbook = new HSSFWorkbook(is);
                }
                
                Sheet sheet = workbook.getSheetAt(0);
                int rowNum = 0;

                for (Row row : sheet) {
                    rowNum++;
                    
                    if (rowNum == 1) continue; //Saltar encabezado
                    if (esFilaVacia(row)) continue;

                    try {
                        //Columna 1: Curso
                        String nombreCurso = obtenerValorCelda(row.getCell(1));
                        //Columnas 2-7: EP1, EP2, EP3, EC1, EC2, EC3
                        String ep1Str = obtenerValorCelda(row.getCell(2));
                        String ep2Str = obtenerValorCelda(row.getCell(3));
                        String ep3Str = obtenerValorCelda(row.getCell(4));
                        String ec1Str = obtenerValorCelda(row.getCell(5));
                        String ec2Str = obtenerValorCelda(row.getCell(6));
                        String ec3Str = obtenerValorCelda(row.getCell(7));

                        if (nombreCurso == null || nombreCurso.trim().isEmpty()) {
                            mensajesError.add("Fila " + rowNum + ": Curso vacío");
                            errores++;
                            continue;
                        }

                        try {
                            double ep1 = Double.parseDouble(ep1Str.replace(',', '.'));
                            double ep2 = Double.parseDouble(ep2Str.replace(',', '.'));
                            double ep3 = Double.parseDouble(ep3Str.replace(',', '.'));
                            double ec1 = Double.parseDouble(ec1Str.replace(',', '.'));
                            double ec2 = Double.parseDouble(ec2Str.replace(',', '.'));
                            double ec3 = Double.parseDouble(ec3Str.replace(',', '.'));

                            //Validar que sume 100
                            double total = ep1 + ep2 + ep3 + ec1 + ec2 + ec3;
                            if (Math.abs(total - 100.0) > 0.01) {
                                mensajesError.add("Fila " + rowNum + ": Los porcentajes no suman 100 (suma: " + total + ")");
                                errores++;
                                continue;
                            }

                            if (insertarPesosEvaluacion(nombreCurso.trim(), ep1, ep2, ep3, ec1, ec2, ec3)) {
                                procesados++;
                            } else {
                                mensajesError.add("Fila " + rowNum + ": Error al insertar pesos para " + nombreCurso);
                                errores++;
                            }

                        } catch (NumberFormatException e) {
                            mensajesError.add("Fila " + rowNum + ": Valores numéricos inválidos");
                            errores++;
                        }

                    } catch (Exception e) {
                        mensajesError.add("Fila " + rowNum + ": Error inesperado: " + e.getMessage());
                        errores++;
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

        } else if (filename.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                int lineNum = 0;

                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    
                    if (lineNum == 1) continue;
                    if (line.trim().isEmpty()) continue;

                    String[] campos = line.split("[,;\\t]");

                    if (campos.length < 8) {
                        mensajesError.add("Línea " + lineNum + ": Formato incorrecto");
                        errores++;
                        continue;
                    }

                    try {
                        String nombreCurso = campos[1].trim();
                        
                        if (nombreCurso.isEmpty()) {
                            mensajesError.add("Línea " + lineNum + ": Curso vacío");
                            errores++;
                            continue;
                        }

                        double ep1 = Double.parseDouble(campos[2].trim().replace(',', '.'));
                        double ep2 = Double.parseDouble(campos[3].trim().replace(',', '.'));
                        double ep3 = Double.parseDouble(campos[4].trim().replace(',', '.'));
                        double ec1 = Double.parseDouble(campos[5].trim().replace(',', '.'));
                        double ec2 = Double.parseDouble(campos[6].trim().replace(',', '.'));
                        double ec3 = Double.parseDouble(campos[7].trim().replace(',', '.'));

                        double total = ep1 + ep2 + ep3 + ec1 + ec2 + ec3;
                        if (Math.abs(total - 100.0) > 0.01) {
                            mensajesError.add("Línea " + lineNum + ": Los porcentajes no suman 100");
                            errores++;
                            continue;
                        }

                        if (insertarPesosEvaluacion(nombreCurso, ep1, ep2, ep3, ec1, ec2, ec3)) {
                            procesados++;
                        } else {
                            mensajesError.add("Línea " + lineNum + ": Error al insertar pesos para " + nombreCurso);
                            errores++;
                        }

                    } catch (NumberFormatException e) {
                        mensajesError.add("Línea " + lineNum + ": Valores numéricos inválidos");
                        errores++;
                    } catch (Exception e) {
                        mensajesError.add("Línea " + lineNum + ": Error: " + e.getMessage());
                        errores++;
                    }
                }
            }
        } else {
            result.put("success", false);
            result.put("mensaje", "Formato no soportado");
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
    }

    return result;
}

//Inserta los porcentajes de evaluación para un curso
private boolean insertarPesosEvaluacion(String nombreCurso, double ep1, double ep2, double ep3, 
                                        double ec1, double ec2, double ec3) {
    try {
        //Buscar código del curso
        String sqlCodigo = "SELECT codigo_curso FROM cursos WHERE nombre_curso = ? LIMIT 1";
        List<String> codigos = jdbcTemplate.queryForList(sqlCodigo, new Object[]{nombreCurso}, String.class);
        
        if (codigos.isEmpty()) {
            return false; //Curso no encontrado
        }
        
        String codigoCurso = codigos.get(0);

        //Obtener ciclo activo
        String sqlCiclo = "SELECT id_ciclo FROM ciclos_academicos WHERE estado = 'ACTIVO' LIMIT 1";
        List<Integer> ciclos = jdbcTemplate.queryForList(sqlCiclo, Integer.class);
        
        if (ciclos.isEmpty()) {
            return false; //No hay ciclo activo
        }
        
        Integer idCiclo = ciclos.get(0);

        //IDs de tipos de evaluación
        //1=EP1, 2=EP2, 3=EP3, 4=EC1, 5=EC2, 6=EC3
        Map<Integer, Double> pesos = new HashMap<>();
        pesos.put(1, ep1);
        pesos.put(2, ep2);
        pesos.put(3, ep3);
        pesos.put(4, ec1);
        pesos.put(5, ec2);
        pesos.put(6, ec3);

        for (Map.Entry<Integer, Double> entry : pesos.entrySet()) {
            Integer tipoEvalId = entry.getKey();
            Double porcentaje = entry.getValue();

            //Verificar si ya existe
            String checkSql = """
                SELECT COUNT(*) FROM porcentajes_evaluacion 
                WHERE codigo_curso = ? AND id_ciclo = ? AND tipo_eval_id = ?
            """;
            
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, 
                codigoCurso, idCiclo, tipoEvalId);

            if (count != null && count > 0) {
                //Actualizar
                String updateSql = """
                    UPDATE porcentajes_evaluacion 
                    SET porcentaje = ? 
                    WHERE codigo_curso = ? AND id_ciclo = ? AND tipo_eval_id = ?
                """;
                jdbcTemplate.update(updateSql, porcentaje, codigoCurso, idCiclo, tipoEvalId);
            } else {
                //Insertar
                String insertSql = """
                    INSERT INTO porcentajes_evaluacion (codigo_curso, id_ciclo, tipo_eval_id, porcentaje)
                    VALUES (?, ?, ?, ?)
                """;
                jdbcTemplate.update(insertSql, codigoCurso, idCiclo, tipoEvalId, porcentaje);
            }
        }
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

//Métodos auxiliares
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

private String obtenerValorCelda(Cell cell) {
    if (cell == null) return null;
    DataFormatter fmt = new DataFormatter();
    String val = fmt.formatCellValue(cell);
    return val == null ? null : val.trim();
}

// 3. VER USUARIOS 
//Obtiene lista completa de estudiantes con sus cursos y laboratorios
public List<Map<String, Object>> obtenerEstudiantesCompleto() {
    String sql = """
        SELECT 
            e.cui,
            e.id_usuario,
            e.apellidos_nombres,
            u.correo_institucional,
            e.numero_matricula,
            e.estado_estudiante,
            COUNT(DISTINCT m.id_matricula) as cursos_matriculados
        FROM estudiantes e
        INNER JOIN usuarios u ON e.id_usuario = u.id_usuario
        LEFT JOIN matriculas m ON e.cui = m.cui AND m.estado_matricula = 'ACTIVO'
        WHERE u.estado_cuenta = 'ACTIVO'
        GROUP BY e.cui, e.id_usuario, e.apellidos_nombres, u.correo_institucional, 
                 e.numero_matricula, e.estado_estudiante
        ORDER BY e.apellidos_nombres
    """;
    
    List<Map<String, Object>> estudiantes = jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> est = new HashMap<>();
        est.put("cui", rs.getString("cui"));
        est.put("idUsuario", rs.getInt("id_usuario"));
        est.put("apellidosNombres", rs.getString("apellidos_nombres"));
        est.put("correoInstitucional", rs.getString("correo_institucional"));
        est.put("numeroMatricula", rs.getInt("numero_matricula"));
        est.put("estadoEstudiante", rs.getString("estado_estudiante"));
        est.put("cursosMatriculados", rs.getInt("cursos_matriculados"));
        return est;
    });

    //Cargar detalles de cursos para cada estudiante
    for (Map<String, Object> e : estudiantes) {
        String cui = (String) e.get("cui");
        e.put("cursosDetalle", obtenerCursosEstudianteSimple(cui));
        e.put("laboratoriosMatriculados", obtenerLaboratoriosMatriculadosSimple(cui));
        e.put("laboratoriosDisponibles", obtenerLaboratoriosDisponiblesSimple(cui));
    }

    return estudiantes;
}

//Obtiene cursos de un estudiante 
private List<Map<String, Object>> obtenerCursosEstudianteSimple(String cui) {
    String sql = """
        SELECT DISTINCT gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase, gc.grupo_id
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO'
        ORDER BY gc.tipo_clase, gc.codigo_curso, gc.letra_grupo
    """;

    return jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        Map<String, Object> c = new HashMap<>();
        c.put("codigoCurso", rs.getString("codigo_curso"));
        c.put("nombreCurso", rs.getString("nombre_curso"));
        c.put("letraGrupo", rs.getString("letra_grupo"));
        c.put("tipoClase", rs.getString("tipo_clase"));
        c.put("grupoId", rs.getInt("grupo_id"));
        return c;
    });
}

//Obtiene laboratorios matriculados de un estudiante
private List<Map<String, Object>> obtenerLaboratoriosMatriculadosSimple(String cui) {
    String sql = """
        SELECT DISTINCT gc.codigo_curso, c.nombre_curso, gc.letra_grupo, gc.tipo_clase, gc.grupo_id
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO' AND gc.tipo_clase = 'LABORATORIO'
        ORDER BY gc.codigo_curso, gc.letra_grupo
    """;

    return jdbcTemplate.query(sql, new Object[]{cui}, (rs, rowNum) -> {
        Map<String, Object> c = new HashMap<>();
        c.put("codigoCurso", rs.getString("codigo_curso"));
        c.put("nombreCurso", rs.getString("nombre_curso"));
        c.put("letraGrupo", rs.getString("letra_grupo"));
        c.put("tipoClase", rs.getString("tipo_clase"));
        c.put("grupoId", rs.getInt("grupo_id"));
        return c;
    });
}

//Obtiene laboratorios disponibles para un estudiante (ELIMINADO ¿CREO?)
private List<Map<String, Object>> obtenerLaboratoriosDisponiblesSimple(String cui) {
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
        Map<String, Object> c = new HashMap<>();
        c.put("codigoCurso", rs.getString("codigo_curso"));
        c.put("nombreCurso", rs.getString("nombre_curso"));
        c.put("letraGrupo", rs.getString("letra_grupo"));
        c.put("tipoClase", rs.getString("tipo_clase"));
        c.put("grupoId", rs.getInt("grupo_id"));
        return c;
    });
}

//Obtiene lista completa de profesores con sus cursos
public List<Map<String, Object>> obtenerProfesoresCompleto() {
    String sql = """
        SELECT d.id_docente, d.id_usuario, d.apellidos_nombres, d.departamento, 
            u.correo_institucional, d.es_responsable_teoria,
            COUNT(DISTINCT h.id_horario) as cursos_asignados,
            GROUP_CONCAT(DISTINCT gc.tipo_clase) as tipos_clase,
            GROUP_CONCAT(DISTINCT CONCAT(gc.codigo_curso,'||',c.nombre_curso,'||',gc.letra_grupo,'||',gc.tipo_clase,'||',gc.grupo_id) SEPARATOR ';') as cursos_detalle
        FROM docentes d
        INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
        LEFT JOIN horarios h ON d.id_docente = h.id_docente
        LEFT JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        LEFT JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        WHERE u.estado_cuenta = 'ACTIVO'
        GROUP BY d.id_docente, d.id_usuario, d.apellidos_nombres, d.departamento, 
                 u.correo_institucional, d.es_responsable_teoria
        ORDER BY d.apellidos_nombres
    """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> p = new HashMap<>();
        p.put("idDocente", rs.getInt("id_docente"));
        p.put("idUsuario", rs.getInt("id_usuario"));
        p.put("apellidosNombres", rs.getString("apellidos_nombres"));
        p.put("departamento", rs.getString("departamento"));
        p.put("correoInstitucional", rs.getString("correo_institucional"));
        p.put("esResponsableTeoria", rs.getBoolean("es_responsable_teoria"));
        p.put("cursosAsignados", rs.getInt("cursos_asignados"));

        String tipos = rs.getString("tipos_clase");
        String cursosDetalleRaw = rs.getString("cursos_detalle");
        
        //Determinar tipo de clase
        String tipoClase;
        if (tipos == null) {
            tipoClase = "SIN ASIGNAR";
        } else if (tipos.contains("TEORIA") && tipos.contains("LABORATORIO")) {
            tipoClase = "AMBOS";
        } else {
            tipoClase = tipos;
        }
        p.put("tipoClase", tipoClase);

        //Procesar cursos detalle
        List<Map<String, Object>> cursosDetalle = new ArrayList<>();
        if (cursosDetalleRaw != null && !cursosDetalleRaw.isEmpty()) {
            String[] parts = cursosDetalleRaw.split(";");
            for (String part : parts) {
                String[] fields = part.split("\\|\\|");
                if (fields.length >= 4) {
                    Map<String, Object> g = new HashMap<>();
                    g.put("codigoCurso", fields[0]);
                    g.put("nombreCurso", fields[1]);
                    g.put("letraGrupo", fields[2]);
                    g.put("tipoClase", fields[3]);
                    if (fields.length >= 5) {
                        try {
                            g.put("grupoId", Integer.parseInt(fields[4]));
                        } catch (NumberFormatException nfe) {
                            g.put("grupoId", 0);
                        }
                    }
                    cursosDetalle.add(g);
                }
            }
        }
        p.put("cursosDetalle", cursosDetalle);

        return p;
    });
}

// 4. HABILITAR PERÍODOS DE NOTAS
    //Guarda un período de ingreso de notas
    public int guardarPeriodoIngresoNotas(
            String codigoCurso,
            Integer tipoEvalId,
            LocalDateTime inicio,
            LocalDateTime fin,
            int creadoPor) {

        String sql = """
            INSERT INTO periodos_ingreso_notas 
            (codigo_curso, tipo_eval_id, fecha_inicio, fecha_fin, creado_por, estado)
            VALUES (?, ?, ?, ?, ?, 'PROGRAMADO')
        """;

        jdbcTemplate.update(sql, codigoCurso, tipoEvalId, inicio, fin, creadoPor);
        actualizarEstadosPeriodosNotas();

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    //Obtiene todos los períodos de ingreso de notas
    public List<PeriodoIngresoNotas> obtenerPeriodosIngresoNotas() {
        actualizarEstadosPeriodosNotas();
        
        String sql = """
            SELECT 
                p.id_periodo,
                p.codigo_curso,
                c.nombre_curso,
                p.tipo_eval_id,
                te.nombre AS nombre_evaluacion,
                p.fecha_inicio,
                p.fecha_fin,
                p.estado,
                p.creado_por
            FROM periodos_ingreso_notas p
            LEFT JOIN cursos c ON p.codigo_curso = c.codigo_curso
            LEFT JOIN tipos_evaluacion te ON p.tipo_eval_id = te.tipo_eval_id
            ORDER BY p.fecha_inicio DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
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
            p.creadoPor = rs.getInt("creado_por");
            return p;
        });
    }

    //Devuelve el id_usuario (si existe) para un correo institucional dado
    public Optional<Integer> obtenerIdUsuarioPorCorreo(String correo) {
        String sql = "SELECT id_usuario FROM usuarios WHERE correo_institucional = ?";
        List<Integer> ids = jdbcTemplate.queryForList(sql, new Object[]{correo}, Integer.class);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    //Elimina un período de ingreso de notas
    public boolean eliminarPeriodoIngresoNotas(int idPeriodo) {
        int rows = jdbcTemplate.update(
            "DELETE FROM periodos_ingreso_notas WHERE id_periodo = ?",
            idPeriodo
        );
        return rows > 0;
    }

    //Verifica si hay períodos activos para ingreso de notas
    public boolean hayPeriodoActivoNotas() {
        actualizarEstadosPeriodosNotas();
        
        String sql = """
            SELECT COUNT(*) 
            FROM periodos_ingreso_notas
            WHERE estado = 'ACTIVO'
        """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }

    //Actualiza estados de períodos de notas
    public void actualizarEstadosPeriodosNotas() {
        try {
            jdbcTemplate.execute("CALL actualizar_estados_periodos_notas()");
        } catch (Exception e) {
            //Si el procedimiento no existe, hacerlo manualmente
            LocalDateTime ahora = LocalDateTime.now();

            String sqlActivar = """
                UPDATE periodos_ingreso_notas
                SET estado = 'ACTIVO'
                WHERE estado = 'PROGRAMADO'
                  AND fecha_inicio <= ?
                  AND fecha_fin > ?
            """;
            jdbcTemplate.update(sqlActivar, ahora, ahora);

            String sqlFinalizar = """
                UPDATE periodos_ingreso_notas
                SET estado = 'FINALIZADO'
                WHERE estado IN ('ACTIVO', 'PROGRAMADO')
                  AND fecha_fin <= ?
            """;
            jdbcTemplate.update(sqlFinalizar, ahora);
        }
    }

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

    //Obtiene todos los tipos de evaluación (para selector)
    public List<Map<String, Object>> obtenerTiposEvaluacion() {
        String sql = """
            SELECT tipo_eval_id, codigo, nombre, tipo
            FROM tipos_evaluacion
            ORDER BY tipo_eval_id
        """;

        return jdbcTemplate.queryForList(sql);
    }

// 5. MATRÍCULAS LABS 
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

    public boolean hayPeriodoActivoLaboratorios() {
        actualizarEstadosPeriodos();
        
        String sql = """
            SELECT COUNT(*) 
            FROM periodos_matricula_laboratorio
            WHERE estado = 'ACTIVO'
        """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }

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
        actualizarEstadosPeriodos();

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public List<PeriodoMatriculaLab> obtenerPeriodosMatricula() {
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

    public boolean eliminarPeriodoMatricula(int idPeriodo) {
        int rows = jdbcTemplate.update(
            "DELETE FROM periodos_matricula_laboratorio WHERE id_periodo = ?",
            idPeriodo
        );
        return rows > 0;
    }

    public void actualizarEstadosPeriodos() {
        LocalDateTime ahora = LocalDateTime.now();

        String sqlActivar = """
            UPDATE periodos_matricula_laboratorio
            SET estado = 'ACTIVO'
            WHERE estado = 'PROGRAMADO'
              AND fecha_inicio <= ?
              AND fecha_fin > ?
        """;
        jdbcTemplate.update(sqlActivar, ahora, ahora);

        String sqlFinalizar = """
            UPDATE periodos_matricula_laboratorio
            SET estado = 'FINALIZADO'
            WHERE estado IN ('ACTIVO', 'PROGRAMADO')
              AND fecha_fin <= ?
        """;
        jdbcTemplate.update(sqlFinalizar, ahora);
    }

// 6. EN OTRO ARCHIVO: SecVerCursos...

// 7. VER HORARIOS
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

//Obtiene lista simplificada de todos los docentes (selector)
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

//Obtiene lista simplificada de todos los estudiantes (selector)
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

//Obtiene lista de todos los salones (selector)
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

//Obtiene horarios de un docente por ID
public List<Map<String, Object>> obtenerHorariosPorDocente(int idDocente) {
    String sql = """
        SELECT 
            h.id_horario,
            h.grupo_id,
            gc.codigo_curso,
            c.nombre_curso,
            gc.letra_grupo,
            gc.tipo_clase,
            h.numero_salon,
            h.dia_semana,
            DATE_FORMAT(h.hora_inicio, '%H:%i') as horaInicio,
            DATE_FORMAT(h.hora_fin, '%H:%i') as horaFin,
            d.apellidos_nombres as nombreDocente,
            h.estado,
            NULL as motivo,
            NULL as descripcion
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        INNER JOIN docentes d ON h.id_docente = d.id_docente
        WHERE h.id_docente = ? AND h.estado = 'ACTIVO'
        
        UNION ALL
        
        SELECT 
            r.id_reserva as id_horario,
            0 as grupo_id,
            'RESERVA' as codigo_curso,
            CONCAT('Reserva: ', COALESCE(r.motivo, 'Sin motivo')) as nombre_curso,
            '' as letra_grupo,
            'RESERVA' as tipo_clase,
            r.numero_salon,
            r.dia_semana,
            DATE_FORMAT(r.hora_inicio, '%H:%i') as horaInicio,
            DATE_FORMAT(r.hora_fin, '%H:%i') as horaFin,
            d.apellidos_nombres as nombreDocente,
            r.estado_reserva as estado,
            r.motivo,
            r.descripcion
        FROM reservas_salon r
        INNER JOIN docentes d ON r.id_docente = d.id_docente
        WHERE r.id_docente = ? AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        
        ORDER BY dia_semana, horaInicio
    """;
    
    return jdbcTemplate.queryForList(sql, idDocente, idDocente);
}

//Obtiene horarios de un estudiante por CUI
public List<Map<String, Object>> obtenerHorariosPorEstudiante(String cui) {
    String sql = """
        SELECT 
            h.id_horario,
            h.grupo_id,
            gc.codigo_curso,
            c.nombre_curso,
            gc.letra_grupo,
            gc.tipo_clase,
            h.numero_salon,
            h.dia_semana,
            DATE_FORMAT(h.hora_inicio, '%H:%i') as horaInicio,
            DATE_FORMAT(h.hora_fin, '%H:%i') as horaFin,
            h.hora_inicio as hora_inicio_sort,
            IFNULL(d.apellidos_nombres, 'Sin docente') as nombreDocente,
            h.estado
        FROM matriculas m
        INNER JOIN grupos_curso gc ON m.grupo_id = gc.grupo_id
        INNER JOIN horarios h ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        LEFT JOIN docentes d ON h.id_docente = d.id_docente
        WHERE m.cui = ? AND m.estado_matricula = 'ACTIVO'
        GROUP BY h.id_horario, h.grupo_id, gc.codigo_curso, c.nombre_curso, 
                 gc.letra_grupo, gc.tipo_clase, h.numero_salon, h.dia_semana,
                 h.hora_inicio, h.hora_fin, d.apellidos_nombres, h.estado
        ORDER BY FIELD(h.dia_semana,'LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'), hora_inicio_sort
    """;
    
    return jdbcTemplate.queryForList(sql, cui);
}

//Obtiene horarios de un salón específico
public List<Map<String, Object>> obtenerHorariosPorSalon(String numeroSalon) {
    String sql = """
        SELECT 
            h.id_horario,
            h.grupo_id,
            gc.codigo_curso,
            c.nombre_curso,
            gc.letra_grupo,
            gc.tipo_clase,
            h.numero_salon,
            h.dia_semana,
            DATE_FORMAT(h.hora_inicio, '%H:%i') as horaInicio,
            DATE_FORMAT(h.hora_fin, '%H:%i') as horaFin,
            IFNULL(d.apellidos_nombres, 'Sin docente') as nombreDocente,
            h.estado,
            NULL as motivo,
            NULL as descripcion
        FROM horarios h
        INNER JOIN grupos_curso gc ON h.grupo_id = gc.grupo_id
        INNER JOIN cursos c ON gc.codigo_curso = c.codigo_curso
        LEFT JOIN docentes d ON h.id_docente = d.id_docente
        WHERE h.numero_salon = ? AND h.estado = 'ACTIVO'
        
        UNION ALL
        
        SELECT 
            0 as id_horario,
            0 as grupo_id,
            'RESERVA' as codigo_curso,
            CONCAT('Reserva: ', r.motivo) as nombre_curso,
            '' as letra_grupo,
            'RESERVA' as tipo_clase,
            r.numero_salon,
            r.dia_semana,
            DATE_FORMAT(r.hora_inicio, '%H:%i') as horaInicio,
            DATE_FORMAT(r.hora_fin, '%H:%i') as horaFin,
            d.apellidos_nombres as nombreDocente,
            r.estado_reserva as estado,
            r.motivo,
            r.descripcion
        FROM reservas_salon r
        INNER JOIN docentes d ON r.id_docente = d.id_docente
        WHERE r.numero_salon = ? AND r.estado_reserva IN ('PENDIENTE', 'CONFIRMADA')
        
        ORDER BY dia_semana, horaInicio
    """;
    
    return jdbcTemplate.queryForList(sql, numeroSalon, numeroSalon);
}

// 8. VER PDFS
//Obtiene todos los sílabos del sistema con información completa
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
            d.apellidos_nombres as nombre_docente,
            CONCAT(ca.anio, '-', ca.semestre) as ciclo
        FROM silabos s
        INNER JOIN cursos c ON s.codigo_curso = c.codigo_curso
        INNER JOIN docentes d ON s.id_docente = d.id_docente
        INNER JOIN ciclos_academicos ca ON s.id_ciclo = ca.id_ciclo
        ORDER BY s.fecha_subida DESC
    """;
    
    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        Map<String, Object> silabo = new HashMap<>();
        silabo.put("idSilabo", rs.getInt("id_silabo"));
        silabo.put("codigoCurso", rs.getString("codigo_curso"));
        silabo.put("nombreCurso", rs.getString("nombre_curso"));
        silabo.put("grupoTeoria", rs.getString("grupo_teoria"));
        
        //Normalizar ruta a formato Unix antes de enviar al frontend
        String rutaArchivo = rs.getString("ruta_archivo");
        if (rutaArchivo != null) {
            rutaArchivo = rutaArchivo.replace("\\", "/");
        }
        silabo.put("rutaArchivo", rutaArchivo);
        
        silabo.put("fechaSubida", rs.getTimestamp("fecha_subida"));
        silabo.put("estado", rs.getString("estado"));
        silabo.put("nombreDocente", rs.getString("nombre_docente"));
        silabo.put("ciclo", rs.getString("ciclo"));
        return silabo;
    });
}

//Obtiene todos los exámenes (mayores y menores notas) del sistema
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
            te.codigo as codigo_evaluacion,
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
        examen.put("codigoEvaluacion", rs.getString("codigo_evaluacion"));
        examen.put("tipoNota", rs.getString("tipo_nota"));
        
        //Normalizar ruta a formato Unix antes de enviar al frontend
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

// 9. REPORTES
//Obtiene reporte completo de estudiantes matriculados
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
    
    //Agregar notas a cada estudiante
    for (Map<String, Object> est : estudiantes) {
        int idMatricula = (int) est.get("idMatricula");
        Map<String, Double> notas = obtenerNotasEstudiante(idMatricula);
        est.put("notas", notas);
        
        //Calcular promedio
        double suma = 0;
        int count = 0;
        for (Double nota : notas.values()) {
            if (nota != null) {
                suma += nota;
                count++;
            }
        }
        est.put("promedio", count > 0 ? suma / count : 0.0);
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

}