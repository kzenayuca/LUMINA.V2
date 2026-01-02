package com.LuminaWeb.controller;

import com.LuminaWeb.dao.EstudianteDAO;
import com.LuminaWeb.dao.MatriculaLaboratorioDAO;
import com.LuminaWeb.dao.MatriculaLaboratorioDAO.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/est/api/matricula-laboratorio")
public class MatriculaLaboratorioController {

    @Autowired
    private MatriculaLaboratorioDAO dao;

    @Autowired
    private EstudianteDAO estudianteDAO;

    private static class ErrorResp {
        public String error;
        public ErrorResp(String e) { this.error = e; }
    }

    /**
     * Verifica si hay período de matrícula activo
     */
    @GetMapping("/periodo-activo")
    public ResponseEntity<?> verificarPeriodoActivo(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
            }

            boolean activo = dao.hayPeriodoActivo();
            Map<String, Object> periodo = activo ? dao.obtenerPeriodoActivo() : null;

            Map<String, Object> response = new HashMap<>();
            response.put("activo", activo);
            response.put("periodo", periodo);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp(ex.getMessage()));
        }
    }

    /**
     * Obtiene los laboratorios disponibles para el estudiante autenticado
     */
    @GetMapping("/disponibles")
    public ResponseEntity<?> obtenerLaboratoriosDisponibles(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
            }

            // Verificar período activo
            if (!dao.hayPeriodoActivo()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResp("No hay período de matrícula activo"));
            }

            // Obtener estudiante
            String correo = (String) session.getAttribute("usuario");
            EstudianteDAO.Estudiante estudiante = estudianteDAO.obtenerPorCorreo(correo);

            if (estudiante == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Estudiante no encontrado"));
            }

            // Obtener laboratorios disponibles
            List<LaboratorioDisponible> laboratorios = 
                dao.obtenerLaboratoriosDisponibles(estudiante.cui);

            return ResponseEntity.ok(laboratorios);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp(ex.getMessage()));
        }
    }

    /**
     * Matricula al estudiante en un grupo de laboratorio
     */
    @PostMapping("/matricular")
    public ResponseEntity<?> matricularEnLaboratorio(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        try {
            if (session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
            }

            // Verificar período activo
            if (!dao.hayPeriodoActivo()) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "No hay período de matrícula activo");
                return ResponseEntity.ok(resp);
            }

            // Obtener estudiante
            String correo = (String) session.getAttribute("usuario");
            EstudianteDAO.Estudiante estudiante = estudianteDAO.obtenerPorCorreo(correo);

            if (estudiante == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "Estudiante no encontrado");
                return ResponseEntity.ok(resp);
            }

            // Obtener grupoId del payload
            Object grupoIdObj = payload.get("grupoId");
            if (grupoIdObj == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "Falta el parámetro grupoId");
                return ResponseEntity.ok(resp);
            }

            int grupoId;
            if (grupoIdObj instanceof Integer) {
                grupoId = (Integer) grupoIdObj;
            } else if (grupoIdObj instanceof String) {
                grupoId = Integer.parseInt((String) grupoIdObj);
            } else if (grupoIdObj instanceof Number) {
                grupoId = ((Number) grupoIdObj).intValue();
            } else {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "grupoId inválido");
                return ResponseEntity.ok(resp);
            }

            // Realizar matrícula
            Map<String, Object> result = dao.matricularEnLaboratorio(
                estudiante.cui, grupoId);

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    /**
     * Obtiene las matrículas de laboratorio del estudiante
     */
    @GetMapping("/mis-matriculas")
    public ResponseEntity<?> obtenerMisMatriculas(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
            }

            // Obtener estudiante
            String correo = (String) session.getAttribute("usuario");
            EstudianteDAO.Estudiante estudiante = estudianteDAO.obtenerPorCorreo(correo);

            if (estudiante == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Estudiante no encontrado"));
            }

            // Obtener matrículas
            List<MatriculaLab> matriculas = 
                dao.obtenerMatriculasLaboratorio(estudiante.cui);

            return ResponseEntity.ok(matriculas);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp(ex.getMessage()));
        }
    }

    /**
     * Cancela la matrícula de un laboratorio
     */
    @DeleteMapping("/{idMatricula}")
    public ResponseEntity<?> cancelarMatricula(
            @PathVariable int idMatricula,
            HttpSession session) {

        try {
            if (session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
            }

            // Obtener estudiante
            String correo = (String) session.getAttribute("usuario");
            EstudianteDAO.Estudiante estudiante = estudianteDAO.obtenerPorCorreo(correo);

            if (estudiante == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "Estudiante no encontrado");
                return ResponseEntity.ok(resp);
            }

            // Cancelar matrícula
            boolean result = dao.cancelarMatriculaLaboratorio(
                idMatricula, estudiante.cui);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("mensaje", result ? 
                "Matrícula cancelada exitosamente" : 
                "No se pudo cancelar la matrícula");

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }
}