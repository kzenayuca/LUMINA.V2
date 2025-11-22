package com.LuminaWeb.controller;

import com.LuminaWeb.dao.EstudianteDAO;
import com.LuminaWeb.dao.EstudianteDAO.Estudiante;
import com.LuminaWeb.dao.EstudianteDAO.CursoNotasDTO;
import com.LuminaWeb.dao.EstudianteDAO.Horario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

/**
 * Controlador REST para operaciones relacionadas con estudiantes.
 *
 * Rutas:
 *  GET /api/estudiantes                -> listar todos los estudiantes
 *  GET /api/estudiantes/me             -> estudiante autenticado (según session "usuario")
 *  GET /api/estudiantes/notas          -> notas por cui (o si no se pasa, intenta usar la sesión)
 *  GET /api/estudiantes/me/horarios    -> horarios del estudiante autenticado
 *
 * Nota: EstudianteDAO está declarado como @Repository y usa JdbcTemplate.
 */
@RestController
@RequestMapping("/est/api/estudiantes")
public class EstudianteController {

    private final EstudianteDAO dao;

    @Autowired
    public EstudianteController(EstudianteDAO dao) {
        this.dao = dao;
    }

    // GET /api/estudiantes  -> listar todos los estudiantes
    @GetMapping({"", "/"})
    public ResponseEntity<?> listAll() {
        try {
            List<Estudiante> lista = dao.obtenerTodos();
            return ResponseEntity.ok(lista);
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    // GET /api/estudiantes/me -> devolver estudiante autenticado (por session)
    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }

            String correo = (String) session.getAttribute("usuario");
            Estudiante e = dao.obtenerPorCorreo(correo);
            if (e == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Estudiante no encontrado"));
            }

            return ResponseEntity.ok(e);
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/estudiantes/notas?cui=...&semestre=...
     * Si no se pasa 'cui', intenta obtenerlo desde la sesión (correo -> buscar Estudiante -> extraer campo 'cui').
     */
    @GetMapping("/notas")
    public ResponseEntity<?> getNotas(@RequestParam(value = "cui", required = false) String cui,
                                      @RequestParam(value = "semestre", required = false) String semestre,
                                      HttpSession session) {
        try {
            // Si no viene cui, intentar obtener desde sesión
            if (cui == null || cui.trim().isEmpty()) {
                if (session != null && session.getAttribute("usuario") != null) {
                    String correo = (String) session.getAttribute("usuario");
                    Estudiante e = dao.obtenerPorCorreo(correo);
                    if (e != null) {
                        // Intentar obtener 'cui' directamente (campo público). Si tu DTO cambia, usa getter.
                        if (e.cui != null && !e.cui.trim().isEmpty()) {
                            cui = e.cui;
                        } else {
                            // intento con reflexión como fallback (si aún lo necesitas)
                            try {
                                java.lang.reflect.Field f = e.getClass().getDeclaredField("cui");
                                f.setAccessible(true);
                                Object val = f.get(e);
                                if (val != null) cui = val.toString();
                            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                                // no hay cui disponible
                            }
                        }
                    }
                }
            }

            if (cui == null || cui.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResp("Falta parámetro 'cui' (o no autenticado)"));
            }

            List<CursoNotasDTO> cursos = dao.getNotasPorCUI(cui, semestre);
            return ResponseEntity.ok(cursos);

        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    // GET /api/estudiantes/me/horarios -> horarios del estudiante autenticado
    @GetMapping("/me/horarios")
    public ResponseEntity<?> getMeHorarios(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            String correo = (String) session.getAttribute("usuario");
            Estudiante e = dao.obtenerPorCorreo(correo);
            if (e == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Estudiante no encontrado"));
            }

            // usar e.idUsuario (campo público en tu DTO). Si lo cambias a private, usa getter.
            int idUsuario = e.idUsuario;
            List<Horario> horarios = dao.obtenerHorariosPorIdUsuario(idUsuario);
            return ResponseEntity.ok(horarios);

        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    // Clase para mensajes de error simples (se serializa a JSON automáticamente)
    private static class ErrorResp {
        public String error;
        public ErrorResp(String e) { this.error = e; }
    }
}
