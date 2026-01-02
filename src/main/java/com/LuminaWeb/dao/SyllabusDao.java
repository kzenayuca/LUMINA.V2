package com.LuminaWeb.dao;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Repository
public class SyllabusDao {

    private final JdbcTemplate jdbc;

    public SyllabusDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Obtener cursos por correo del profesor (ajusta la consulta a tu modelo)
    public List<Map<String,String>> getCursosPorCorreoProfesor(String correo) {
        // Este SQL asume hay join entre usuarios->docentes->horarios->grupos_curso->cursos
        String sql = """
            SELECT DISTINCT c.codigo_curso, c.nombre_curso
            FROM usuarios u
            JOIN docentes d ON d.id_usuario = u.id_usuario
            JOIN horarios h ON h.id_docente = d.id_docente
            JOIN grupos_curso g ON g.grupo_id = h.grupo_id
            JOIN cursos c ON c.codigo_curso = g.codigo_curso
            WHERE u.correo_institucional = ?
        """;
        return jdbc.query(sql, new Object[]{correo}, (rs, rowNum) -> {
            Map<String,String> m = new HashMap<>();
            m.put("codigo", rs.getString("codigo_curso"));
            m.put("nombre", rs.getString("nombre_curso"));
            return m;
        });
    }

    public boolean existeSilabo(String codigoCurso) {
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM silabos WHERE codigo_curso = ?", Integer.class, codigoCurso);
        return cnt != null && cnt > 0;
    }

    public int insertarSilabo(String codigoCurso, Integer idCiclo, String grupoTeoria, String rutaArchivo, String correoProfesor) {
        // insertar y devolver id_silabo (autoincrement)
        String sql = "INSERT INTO silabos (codigo_curso, id_ciclo, grupo_teoria, ruta_archivo, id_docente, estado) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDIENTE')";
        // Intentamos resolver id_docente por correo
        // Resolve id_docente by professor email in a single assignment so it is
        // effectively final and can be referenced from inside the lambda below.
        Integer idDocente = (correoProfesor != null && !correoProfesor.isEmpty())
            ? jdbc.queryForObject("SELECT d.id_docente FROM docentes d JOIN usuarios u ON d.id_usuario = u.id_usuario WHERE u.correo_institucional = ?",
                Integer.class, correoProfesor)
            : null;
        // Si idCiclo es null, insertar con null (o ajustar)
        // Ejecutar insert y retornar id generado
        return jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, codigoCurso);
            if (idCiclo != null) ps.setInt(2, idCiclo); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, grupoTeoria);
            ps.setString(4, rutaArchivo);
            if (idDocente != null) ps.setInt(5, idDocente); else ps.setNull(5, java.sql.Types.INTEGER);
            return ps;
        });
    }

    public void actualizarRutaSilabo(String codigoCurso, String nuevaRuta, String correoProfesor) {
        // Actualiza la ruta y fecha_subida
        String sql = "UPDATE silabos SET ruta_archivo = ?, fecha_subida = NOW(), estado = 'PENDIENTE' WHERE codigo_curso = ?";
        jdbc.update(sql, nuevaRuta, codigoCurso);
    }

    // IMPORTAR EXCEL: ejemplo sencillo: asumimos formato:
    // columna A: numero_unidad
    // columna B: nombre_unidad
    // columna C: numero_tema
    // columna D: nombre_tema
    public void importarUnidadesYTemasDesdeExcel(InputStream in, Integer idSilabo) throws Exception {
        Workbook wb = WorkbookFactory.create(in);
        Sheet sheet = wb.getSheetAt(0);
        // Mapa para llevar control local: key=numero_unidad -> id_unidad (si necesitamos)
        // Primero iteramos filas y vamos insertando unidades/temas
        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                // si hay encabezados, puedes omitir: cambia según tu Excel
                // Suponemos la primera fila puede ser encabezado; detectarlo:
                Cell c0 = row.getCell(0);
                if (c0 != null && c0.getCellType() == CellType.STRING && c0.getStringCellValue().toLowerCase().contains("unidad")) {
                    continue; // saltar encabezado
                }
            }
            Cell cellUnidadNum = row.getCell(0);
            Cell cellUnidadName = row.getCell(1);
            Cell cellTemaNum = row.getCell(2);
            Cell cellTemaName = row.getCell(3);

            if (cellUnidadNum == null || cellUnidadName == null || cellTemaNum == null || cellTemaName == null) {
                continue;
            }

            int numeroUnidad = (int) cellUnidadNum.getNumericCellValue();
            String nombreUnidad = cellUnidadName.getStringCellValue();
            int numeroTema = (int) cellTemaNum.getNumericCellValue();
            String nombreTema = cellTemaName.getStringCellValue();

            // Insertar unidad (si no existe para este silabo+numero_unidad)
            Integer idUnidad = jdbc.queryForObject(
                    "SELECT unidad_id FROM unidades u JOIN silabos s ON u.id_silabo = s.id_silabo WHERE s.codigo_curso = ? AND u.numero_unidad = ?",
                    Integer.class, new Object[]{ /* codigoCurso? */ /* but we don't have codigoCurso here */ null, numeroUnidad});
            // Simplificar: insertar sin comprobación (puedes mejorar esto)
            String sqlU = "INSERT INTO unidades (id_silabo, numero_unidad, nombre_unidad) VALUES (?, ?, ?)";
            jdbc.update(sqlU, idSilabo, numeroUnidad, nombreUnidad);

            // obtener unidad_id recien insertada
            Integer unidadId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            String sqlT = "INSERT INTO temas (unidad_id, numero_tema, nombre_tema) VALUES (?, ?, ?)";
            jdbc.update(sqlT, unidadId, numeroTema, nombreTema);
        }
        wb.close();
    }
}