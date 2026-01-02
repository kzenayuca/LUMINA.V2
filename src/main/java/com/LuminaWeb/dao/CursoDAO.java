// CursoDAO.java
package com.LuminaWeb.dao;
import com.LuminaWeb.dto.CursoGrupoDTO;
import com.LuminaWeb.dto.SilaboDTO;
import com.LuminaWeb.dto.UnidadDTO;
import com.LuminaWeb.dto.TemaDTO;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CursoDAO {

    private final JdbcTemplate jdbc;
    public class GrupoInfo {
        private Integer idCiclo;
        private String grupoTeoria;
        private Integer idDocente;

        public GrupoInfo(Integer idCiclo, String grupoTeoria, Integer idDocente) {
            this.idCiclo = idCiclo;
            this.grupoTeoria = grupoTeoria;
            this.idDocente = idDocente;
        }

        public Integer getIdCiclo() { return idCiclo; }
        public String getGrupoTeoria() { return grupoTeoria; }
        public Integer getIdDocente() { return idDocente; }
    }
    

    // DAO method - obtiene id_ciclo y grupo_teoria usando horarios
    public GrupoInfo obtenerInfoGrupo(String codigoCurso, Integer idDocente) {
        String sql = """
                SELECT g.id_ciclo, g.letra_grupo AS grupo_teoria, h.id_docente
                FROM grupos_curso g
                JOIN horarios h ON g.grupo_id = h.grupo_id
                JOIN docentes d ON d.id_docente = h.id_docente
                WHERE g.codigo_curso = ? 
                AND d.id_usuario = ? 
                AND h.estado = 'ACTIVO'
                LIMIT 1;
        """;

        try {
            return jdbc.queryForObject(sql, (rs, rowNum) ->
                new GrupoInfo(
                    rs.getObject("id_ciclo") == null ? null : rs.getInt("id_ciclo"),
                    rs.getString("grupo_teoria"),
                    rs.getInt("id_docente")
                ),
                codigoCurso, idDocente
            );
        } catch (EmptyResultDataAccessException ex) {
            // No encontró grupo correspondiente: devolvemos null o lanzamos excepción controlada
            return null;
        } catch (Exception ex) {
            // Re-lanzar para que el caller lo maneje y se loguee (o envolver en unchecked)
            throw new RuntimeException("Error obteniendo info de grupo: " + ex.getMessage(), ex);
        }
    }



    public CursoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CursoGrupoDTO> obtenerCursosPorDocente(String correo) {
        return jdbc.query(con -> {
            CallableStatement cs = con.prepareCall("{CALL sp_cursos_y_grupos_por_docente(?)}");
            cs.setString(1, correo);
            return cs;
        }, (rs, rowNum) -> {
            CursoGrupoDTO c = new CursoGrupoDTO();
            c.setCodigoCurso(rs.getString("codigo_curso"));
            c.setNombreCurso(rs.getString("nombre_curso"));
            c.setGrupoId(rs.getInt("grupo_id"));
            c.setLetraGrupo(rs.getString("letra_grupo"));
            c.setTipoClase(rs.getString("tipo_clase"));
            return c;
        });
    }

    public List<SilaboDTO> obtenerSilabosPorCurso(String codigoCurso) {
        return jdbc.query(con -> {
            CallableStatement cs = con.prepareCall("{CALL sp_obtener_silabos_por_curso(?)}");
            cs.setString(1, codigoCurso);
            return cs;
        }, (rs, rowNum) -> {
            // Nota: tu SP devuelve o filas o una advertencia; aquí se mapea si hay filas reales
            SilaboDTO s = new SilaboDTO();
            s.setIdSilabo(rs.getInt("id_silabo"));
            s.setCodigoCurso(rs.getString("codigo_curso"));
            s.setIdCiclo(rs.getInt("id_ciclo"));
            s.setGrupoTeoria(rs.getString("grupo_teoria"));
            s.setRutaArchivo(rs.getString("ruta_archivo"));
            s.setIdDocente(rs.getInt("id_docente"));
            s.setFechaSubida(rs.getTimestamp("fecha_subida"));
            s.setEstado(rs.getString("estado"));
            return s;
        });
    }

    // Guardar registros de contenido (unidades/temas). Aquí debes decidir el esquema de tablas; ejemplo simple:
    public int guardarContenido(String codigoCurso, Integer idDocente, String contenidoJson) {
        // Supón que tienes una tabla contenidos (id, codigo_curso, id_docente, json_contenido, fecha)
        String sql = "INSERT INTO contenidos (codigo_curso, id_docente, json_contenido, fecha_creacion) VALUES (?, ?, ?, NOW())";
        return jdbc.update(sql, codigoCurso, idDocente, contenidoJson);
    }

    public int registrarSilabo(String codigoCurso, Integer idDocente, String rutaArchivo) {
        // 1) obtener info del grupo
        System.out.println("Buscando grupo para curso=" + codigoCurso + " docente=" + idDocente);

        GrupoInfo info = obtenerInfoGrupo(codigoCurso, idDocente);
        System.out.println(info == null ? "No se encontró grupo activo" : "Encontrado grupo: ciclo=" + info.getIdCiclo() + " grupo=" + info.getGrupoTeoria() + " idDocente=" + info.getIdDocente());
        if (info == null) {
            // Decide comportamiento: lanzar excepción (recomendado) o insertar con NULLs
            throw new IllegalStateException("No se encontró grupo activo para el curso " + codigoCurso + " y docente " + idDocente);
            // o si prefieres insertar con NULL:
            // info = new GrupoInfo(null, null);
        }

        Integer idCiclo = info.getIdCiclo();
        String grupoTeoria = info.getGrupoTeoria();
        Integer idDoc = info.getIdDocente();

        // 2) comprobar existencia (usamos codigo + id_ciclo + grupo_teoria + id_docente)
        String checkSql = """
            SELECT COUNT(*) FROM silabos
            WHERE codigo_curso = ? AND (id_ciclo = ? OR (? IS NULL AND id_ciclo IS NULL))
            AND (grupo_teoria = ? OR (? IS NULL AND grupo_teoria IS NULL))
            AND id_docente = ?
        """;

        Integer count = jdbc.queryForObject(checkSql, Integer.class,
                codigoCurso, idCiclo, idCiclo, grupoTeoria, grupoTeoria, idDoc);

        if (count != null && count > 0) {
            System.out.println("Sílabo ya existe, actualizando registro");
            String updateSql = """
                UPDATE silabos
                SET ruta_archivo = ?, fecha_subida = NOW(), estado = 'APROBADO'
                WHERE codigo_curso = ? AND (id_ciclo = ? OR (? IS NULL AND id_ciclo IS NULL))
                AND (grupo_teoria = ? OR (? IS NULL AND grupo_teoria IS NULL))
                AND id_docente = ?
            """;
            return jdbc.update(updateSql,
                rutaArchivo,
                codigoCurso, idCiclo, idCiclo, grupoTeoria, grupoTeoria, idDoc
            );
        } else {
            System.out.println("Insertando nuevo registro de sílabo");
            String insertSql = """
                INSERT INTO silabos (codigo_curso, id_ciclo, grupo_teoria, ruta_archivo, id_docente, fecha_subida, estado)
                VALUES (?, ?, ?, ?, ?, NOW(), 'APROBADO')
            """;
            return jdbc.update(insertSql, codigoCurso, idCiclo, grupoTeoria, rutaArchivo, idDoc);
        }
    }


    //PARA EL CONTENIDO

        // 1) Obtener silabo por curso y idUsuario (busca id_docente via docentes.id_usuario)
    public SilaboDTO obtenerSilaboPorCursoYUsuario(String codigoCurso, Integer idUsuario) {
        String sql = """
            SELECT s.id_silabo, s.codigo_curso, s.id_ciclo, s.grupo_teoria, s.ruta_archivo, s.id_docente, s.estado
            FROM silabos s
            JOIN docentes d ON s.id_docente = d.id_docente
            WHERE s.codigo_curso = ? AND d.id_usuario = ?
            LIMIT 1
        """;
        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {
                SilaboDTO s = new SilaboDTO();
                s.setIdSilabo(rs.getInt("id_silabo"));
                s.setCodigoCurso(rs.getString("codigo_curso"));
                s.setIdCiclo(rs.getInt("id_ciclo"));
                s.setGrupoTeoria(rs.getString("grupo_teoria"));
                s.setRutaArchivo(rs.getString("ruta_archivo"));
                s.setIdDocente(rs.getInt("id_docente"));
                s.setEstado(rs.getString("estado"));
                return s;
            }, codigoCurso, idUsuario);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    // 2) Obtener unidades + temas por id_silabo
    public List<UnidadDTO> obtenerUnidadesConTemasPorSilabo(Integer idSilabo) {
        if (idSilabo == null) return Collections.emptyList();

        String sqlUnidades = "SELECT unidad_id, numero_unidad, nombre_unidad FROM unidades WHERE id_silabo = ? ORDER BY numero_unidad";
        List<UnidadDTO> unidades = jdbc.query(sqlUnidades, (rs, rowNum) -> {
            UnidadDTO u = new UnidadDTO();
            u.setUnidadId(rs.getInt("unidad_id"));
            u.setNumeroUnidad(rs.getInt("numero_unidad"));
            u.setNombreUnidad(rs.getString("nombre_unidad"));
            u.setTemas(new ArrayList<>());
            return u;
        }, idSilabo);

        String sqlTemas = "SELECT id_tema, unidad_id, numero_tema, nombre_tema, duracion_estimada, estado FROM temas WHERE unidad_id = ? ORDER BY numero_tema";

        for (UnidadDTO u : unidades) {
            List<TemaDTO> temas = jdbc.query(sqlTemas, (rs, rowNum) -> {
                TemaDTO t = new TemaDTO();
                t.setIdTema(rs.getInt("id_tema"));
                t.setNumeroTema(rs.getInt("numero_tema"));
                t.setNombreTema(rs.getString("nombre_tema"));
                t.setDuracionEstimada(rs.getObject("duracion_estimada") == null ? null : rs.getInt("duracion_estimada"));
                t.setEstado(rs.getString("estado"));
                return t;
            }, u.getUnidadId());
            u.setTemas(temas);
        }

        return unidades;
    }
    @Transactional
    public Integer guardarSilaboYContenido(String codigoCurso,
                                        Integer idUsuario,
                                        Integer idCiclo,
                                        String grupoTeoria,
                                        Integer idDocente,
                                        String rutaArchivo,
                                        List<UnidadDTO> unidades) {

        // Variable mutable para resolver id_docente real
        Integer idDocenteReal = idDocente;

        // 1) Obtener id_docente real si no se pasó (si idDocente == null)
        if (idDocenteReal == null) {
            String q = "SELECT id_docente FROM docentes WHERE id_usuario = ? LIMIT 1";
            try {
                idDocenteReal = jdbc.queryForObject(q, Integer.class, idUsuario);
            } catch (EmptyResultDataAccessException e) {
                throw new IllegalStateException("No existe docente para id_usuario=" + idUsuario);
            }
        }

        // Normalizar grupoTeoria
        if (grupoTeoria != null) grupoTeoria = grupoTeoria.trim();

        // Copias final para las lambdas
        final Integer idDocenteFinal = idDocenteReal;
        final String grupoTeoriaFinal = grupoTeoria;

        // 2) Buscar silabo existente por codigo,id_ciclo,grupo_teoria,id_docente
        String findSilabo = "SELECT id_silabo FROM silabos WHERE codigo_curso = ? AND id_ciclo = ? AND grupo_teoria = ? AND id_docente = ? LIMIT 1";
        Integer idSilabo = null;
        try {
            idSilabo = jdbc.queryForObject(findSilabo, Integer.class, codigoCurso, idCiclo, grupoTeoriaFinal, idDocenteFinal);
        } catch (EmptyResultDataAccessException ex) {
            idSilabo = null;
        }

        if (idSilabo == null) {
            // Insert silabo (usamos idDocenteFinal dentro de la lambda)
            String insert = "INSERT INTO silabos (codigo_curso, id_ciclo, grupo_teoria, ruta_archivo, id_docente, fecha_subida, estado) VALUES (?, ?, ?, ?, ?, NOW(), 'APROBADO')";
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, codigoCurso);
                ps.setInt(2, idCiclo);
                ps.setString(3, grupoTeoriaFinal);
                ps.setString(4, rutaArchivo);
                ps.setInt(5, idDocenteFinal);
                return ps;
            }, kh);
            Number key = kh.getKey();
            if (key != null) idSilabo = key.intValue();
            else throw new RuntimeException("No se pudo obtener id_silabo tras insert.");
        } else {
            // Update ruta y fecha
            String upd = "UPDATE silabos SET ruta_archivo = ?, fecha_subida = NOW(), estado = 'APROBADO' WHERE id_silabo = ?";
            jdbc.update(upd, rutaArchivo, idSilabo);
        }

        // Ahora idSilabo está resuelto; creamos la copia final para lambdas posteriores
        final Integer idSilaboFinal = idSilabo;

        // 3) Borrar unidades y temas existentes (simplifica lógica)
        String delTemas = "DELETE t FROM temas t JOIN unidades u ON t.unidad_id = u.unidad_id WHERE u.id_silabo = ?";
        jdbc.update(delTemas, idSilaboFinal); // elimina temas asociados
        String delUnidades = "DELETE FROM unidades WHERE id_silabo = ?";
        jdbc.update(delUnidades, idSilaboFinal); // elimina unidades

        // 4) Insertar nuevas unidades y temas
        String insertUnidad = "INSERT INTO unidades (id_silabo, numero_unidad, nombre_unidad, descripcion) VALUES (?, ?, ?, ?)";
        String insertTema = "INSERT INTO temas (unidad_id, numero_tema, nombre_tema, duracion_estimada, estado) VALUES (?, ?, ?, ?, ?)";

        for (UnidadDTO u : unidades) {
            KeyHolder khU = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(insertUnidad, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, idSilaboFinal); // uso de copia final
                ps.setInt(2, u.getNumeroUnidad() != null ? u.getNumeroUnidad() : 0);
                ps.setString(3, u.getNombreUnidad());
                ps.setString(4, null);
                return ps;
            }, khU);
            Number k = khU.getKey();
            if (k == null) throw new RuntimeException("No se pudo insertar unidad");
            Integer unidadId = k.intValue();

            if (u.getTemas() != null) {
                int temaNum = 1;
                for (TemaDTO t : u.getTemas()) {
                    jdbc.update(insertTema,
                        unidadId,
                        t.getNumeroTema() != null ? t.getNumeroTema() : temaNum,
                        t.getNombreTema(),
                        t.getDuracionEstimada(),
                        t.getEstado() != null ? t.getEstado() : "PENDIENTE");
                    temaNum++;
                }
            }
        }

        // Retornamos idSilabo para que el caller pueda usarlo si quiere
        return idSilaboFinal;
    }



}
