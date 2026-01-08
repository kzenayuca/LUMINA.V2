package com.LuminaWeb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class CursoService2 {

    @Autowired
    private JdbcTemplate jdbc;

    // =============================
    // LISTAR CURSOS CON DETALLES
    // =============================
    public List<Map<String, Object>> listarCursosConDetalles() {
        String sql = "SELECT c.codigo_curso, c.nombre_curso, c.tiene_laboratorio, " +
                     "g.grupo_id, g.tipo_clase, g.letra_grupo, g.capacidad_maxima, " +
                     "h.dia_semana, h.hora_inicio, h.hora_fin, h.numero_salon, " +
                     "d.apellidos_nombres " +
                     "FROM cursos c " +
                     "LEFT JOIN grupos_curso g ON c.codigo_curso = g.codigo_curso " +
                     "LEFT JOIN horarios h ON g.grupo_id = h.grupo_id " +
                     "LEFT JOIN docentes d ON h.id_docente = d.id_docente " +
                     "ORDER BY c.codigo_curso, g.letra_grupo, g.tipo_clase, g.grupo_id";

        List<Map<String, Object>> filas = jdbc.queryForList(sql);

        Map<String, Map<String, Object>> cursosMap = new HashMap<>();

        for (Map<String, Object> fila : filas) {
            String codigo = fila.get("codigo_curso").toString();
            String letraGrupo = fila.get("letra_grupo") != null ? 
                                fila.get("letra_grupo").toString() : "A";
            
            String claveUnica = codigo + "_" + letraGrupo;

            Map<String, Object> curso = cursosMap.get(claveUnica);
            if (curso == null) {
                curso = new HashMap<>();
                curso.put("codigo_curso", codigo);
                curso.put("nombre_curso", fila.get("nombre_curso"));
                curso.put("tiene_laboratorio", fila.get("tiene_laboratorio"));
                curso.put("letra_grupo", letraGrupo);
                curso.put("capacidad_maxima", fila.get("capacidad_maxima"));
                curso.put("horarios", new ArrayList<String>());
                curso.put("docentes_teoria", new ArrayList<String>());
                curso.put("docentes_laboratorio", new ArrayList<String>());
                cursosMap.put(claveUnica, curso);
            }

            String tipo = fila.get("tipo_clase") != null ? 
                          fila.get("tipo_clase").toString() : "";
            String horarioStr = "";
            
            if (fila.get("dia_semana") != null) {
                horarioStr = fila.get("dia_semana") + " " +
                             fila.get("hora_inicio") + "-" +
                             fila.get("hora_fin") + " (" +
                             fila.get("numero_salon") + ")";
            }

            if (!horarioStr.isEmpty()) {
                List<String> horarios = (List<String>) curso.get("horarios");
                if (!horarios.contains(horarioStr)) {
                    horarios.add(horarioStr);
                }
            }

            if (fila.get("apellidos_nombres") != null) {
                String docenteNombre = fila.get("apellidos_nombres").toString();

                if ("TEORIA".equals(tipo)) {
                    List<String> listaTeoria = (List<String>) curso.get("docentes_teoria");
                    if (!listaTeoria.contains(docenteNombre)) {
                        listaTeoria.add(docenteNombre);
                    }
                } else if ("LABORATORIO".equals(tipo)) {
                    List<String> listaLab = (List<String>) curso.get("docentes_laboratorio");
                    if (!listaLab.contains(docenteNombre)) {
                        listaLab.add(docenteNombre);
                    }
                }
            }
        }

        return new ArrayList<>(cursosMap.values());
    }

    // =============================
    // OBTENER GRUPO SEGÚN TIPO Y GRUPO (NUEVO)
    // =============================
    public Integer obtenerGrupoId(String codigo, String tipoClase, String letraGrupo) {
        try {
            return jdbc.queryForObject(
                "SELECT grupo_id FROM grupos_curso WHERE codigo_curso=? AND tipo_clase=? AND letra_grupo=? LIMIT 1",
                Integer.class,
                codigo, tipoClase, letraGrupo
            );
        } catch (Exception e) {
            return null;
        }
    }

    // =============================
    // VERIFICAR SI EXISTE CURSO CON MISMO CÓDIGO Y GRUPO
    // =============================
    public boolean existeCursoConMismoGrupo(String codigo, String letraGrupo) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM grupos_curso gc " +
                "WHERE gc.codigo_curso = ? AND gc.letra_grupo = ?",
                Integer.class,
                codigo, letraGrupo
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // =============================
    // VERIFICAR SI EXISTE CURSO
    // =============================
    public boolean existeCurso(String codigo) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cursos WHERE codigo_curso = ?",
                Integer.class,
                codigo
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // =============================
    // VERIFICAR SI EXISTE CURSO CON MISMO NOMBRE
    // =============================
    public boolean existeCursoConNombre(String nombre) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cursos WHERE nombre_curso = ?",
                Integer.class,
                nombre
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // =============================
    // GUARDAR CURSO
    // =============================
    @Transactional
    public void guardarCurso(Map<String, Object> datos) {
        String codigo = datos.get("codigo").toString();
        String nombre = datos.get("nombre").toString();
        String letraGrupo = datos.get("letraGrupo").toString();
        int lab = Integer.parseInt(datos.get("lab").toString());
        int capacidadMaxima = Integer.parseInt(datos.getOrDefault("capacidadMaxima", "10").toString());
        int idCiclo = 1;

        if (existeCursoConMismoGrupo(codigo, letraGrupo)) {
            throw new RuntimeException("Ya existe un curso con el código " + codigo + 
                                     " y grupo " + letraGrupo + ". Use un grupo diferente.");
        }

        try {
            Integer cursoExiste = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cursos WHERE codigo_curso = ? AND nombre_curso = ?",
                Integer.class,
                codigo, nombre
            );
            
            if (cursoExiste == null || cursoExiste == 0) {
                jdbc.update(
                    "INSERT INTO cursos (codigo_curso, nombre_curso, tiene_laboratorio) VALUES (?,?,?)",
                    codigo, nombre, lab
                );
            } else {
                jdbc.update(
                    "UPDATE cursos SET tiene_laboratorio = ? WHERE codigo_curso = ? AND nombre_curso = ?",
                    lab, codigo, nombre
                );
            }
        } catch (Exception e) {
            jdbc.update(
                "UPDATE cursos SET tiene_laboratorio = ? WHERE codigo_curso = ? AND nombre_curso = ?",
                lab, codigo, nombre
            );
        }

        crearGrupo(codigo, "TEORIA", idCiclo, letraGrupo, capacidadMaxima);

        if (lab == 1) {
            crearGrupo(codigo, "LABORATORIO", idCiclo, letraGrupo, capacidadMaxima);
        }

        List<Map<String,Object>> horarios = (List<Map<String,Object>>) datos.get("horarios");

        if (horarios != null) {
            for (Map<String,Object> h : horarios) {
                String tipoClase = h.get("tipoClase").toString();
                String dia = h.get("dia").toString();
                String ini = h.get("ini").toString();
                String fin = h.get("fin").toString();
                String salon = h.get("salon").toString();

                Integer grupoId = obtenerGrupoId(codigo, tipoClase, letraGrupo);

                Integer idDocente = null;

                if ("TEORIA".equals(tipoClase)) {
                    List<Integer> docentesTeoria = (List<Integer>) datos.get("docentesTeoria");
                    if (docentesTeoria != null && !docentesTeoria.isEmpty()) {
                        idDocente = docentesTeoria.get(0);
                    }
                }

                if ("LABORATORIO".equals(tipoClase)) {
                    List<Integer> docentesLab = (List<Integer>) datos.get("docentesLab");
                    if (docentesLab != null && !docentesLab.isEmpty()) {
                        idDocente = docentesLab.get(0);
                    }
                }

                if (grupoId != null && idDocente != null) {
                    crearHorario(grupoId, dia, ini, fin, salon, idDocente);
                }
            }
        }
    }

    // =============================
    // OBTENER CURSO POR CÓDIGO
    // =============================
    public Map<String,Object> obtenerCurso(String codigo) {
        List<Map<String,Object>> cursos = listarCursosConDetalles();
        for (Map<String,Object> c : cursos) {
            if (c.get("codigo_curso").equals(codigo)) {
                return c;
            }
        }
        return null;
    }

    // =============================
    // OBTENER CURSO POR CÓDIGO Y GRUPO
    // =============================
    public Map<String,Object> obtenerCursoPorCodigoYGrupo(String codigo, String grupo) {
        List<Map<String,Object>> cursos = listarCursosConDetalles();
        for (Map<String,Object> c : cursos) {
            String codigoCurso = c.get("codigo_curso").toString();
            String grupoCurso = c.get("letra_grupo") != null ? 
                               c.get("letra_grupo").toString() : "A";
            
            if (codigoCurso.equals(codigo) && grupoCurso.equals(grupo)) {
                return c;
            }
        }
        return null;
    }

    // =============================
    // ACTUALIZAR CURSO
    // =============================
    @Transactional
    public void actualizarCurso(String codigo, Map<String,Object> datos) {
        String nombre = datos.get("nombre").toString();
        String letraGrupo = datos.get("letraGrupo").toString();
        int lab = Integer.parseInt(datos.get("lab").toString());
        int capacidadMaxima = Integer.parseInt(datos.getOrDefault("capacidadMaxima", "10").toString());

        jdbc.update(
                "UPDATE cursos SET nombre_curso=?, tiene_laboratorio=? WHERE codigo_curso=?",
                nombre, lab, codigo
        );

        // Actualizar capacidad máxima en grupos
        jdbc.update(
            "UPDATE grupos_curso SET capacidad_maxima=? WHERE codigo_curso=? AND letra_grupo=?",
            capacidadMaxima, codigo, letraGrupo
        );

        List<Integer> grupoIds = jdbc.queryForList(
            "SELECT grupo_id FROM grupos_curso WHERE codigo_curso=? AND letra_grupo=?",
            Integer.class,
            codigo, letraGrupo
        );

        for (Integer id : grupoIds) {
            jdbc.update("DELETE FROM horarios WHERE grupo_id=?", id);
        }

        List<Map<String,Object>> horarios = (List<Map<String,Object>>) datos.get("horarios");

        if (horarios != null) {
            for (Map<String,Object> h : horarios) {
                String tipoClase = h.get("tipoClase").toString();
                String dia = h.get("dia").toString();
                String ini = h.get("ini").toString();
                String fin = h.get("fin").toString();
                String salon = h.get("salon").toString();

                Integer grupoId = obtenerGrupoId(codigo, tipoClase, letraGrupo);

                Integer idDocente = null;

                if ("TEORIA".equals(tipoClase)) {
                    List<Integer> docentesTeoria = (List<Integer>) datos.get("docentesTeoria");
                    if (docentesTeoria != null && !docentesTeoria.isEmpty())
                        idDocente = docentesTeoria.get(0);
                }

                if ("LABORATORIO".equals(tipoClase)) {
                    List<Integer> docentesLab = (List<Integer>) datos.get("docentesLab");
                    if (docentesLab != null && !docentesLab.isEmpty())
                        idDocente = docentesLab.get(0);
                }

                if (grupoId != null && idDocente != null) {
                    crearHorario(grupoId, dia, ini, fin, salon, idDocente);
                }
            }
        }
    }

    // =============================
    // ELIMINAR CURSO
    // =============================
    @Transactional
    public void eliminarCurso(String codigo) {
        List<Integer> grupoIds = jdbc.queryForList(
                "SELECT grupo_id FROM grupos_curso WHERE codigo_curso=?",
                Integer.class,
                codigo
        );

        for (Integer grupoId : grupoIds) {
            jdbc.update("DELETE FROM horarios WHERE grupo_id=?", grupoId);
        }

        jdbc.update("DELETE FROM grupos_curso WHERE codigo_curso=?", codigo);
        jdbc.update("DELETE FROM cursos WHERE codigo_curso=?", codigo);
    }

    // =============================
    // CREAR GRUPO
    // =============================
    public int crearGrupo(String codigo, String tipo, int idCiclo, String letraGrupo, int capacidadMaxima) {
        jdbc.update(
                "INSERT INTO grupos_curso (codigo_curso, tipo_clase, id_ciclo, letra_grupo, capacidad_maxima) VALUES (?,?,?,?,?)",
                codigo, tipo, idCiclo, letraGrupo, capacidadMaxima
        );
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    // =============================
    // CREAR HORARIO
    // =============================
    public int crearHorario(int grupoId, String dia, String ini, String fin, String salon, int idDocente) {
        jdbc.update(
            "INSERT INTO horarios (grupo_id, dia_semana, hora_inicio, hora_fin, numero_salon, id_docente, estado) VALUES (?,?,?,?,?,?,?)",
            grupoId, dia, ini, fin, salon, idDocente, "ACTIVO"
        );
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }
}