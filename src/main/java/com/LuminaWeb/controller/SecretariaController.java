package com.LuminaWeb.controller;

import com.LuminaWeb.dao.SecretariaDAO;
import com.LuminaWeb.dao.SecretariaDAO.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/sec/api/secretaria")
public class SecretariaController {

    private final SecretariaDAO dao;

    @Autowired
    public SecretariaController(SecretariaDAO dao) {
        this.dao = dao;
    }

    private static class ErrorResp {
        public String error;
        public ErrorResp(String e) { this.error = e; }
    }

    // ----------------------------------------------------------------------------------------
    // CURSOS CON LABORATORIO
    // ----------------------------------------------------------------------------------------
    @GetMapping("/cursos-laboratorio")
    public ResponseEntity<?> listarCursosConLaboratorio(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));

            return ResponseEntity.ok(dao.obtenerCursosConLaboratorio());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // GRUPOS DE LABORATORIO
    // ----------------------------------------------------------------------------------------
    @GetMapping("/grupos-laboratorio")
    public ResponseEntity<?> obtenerGruposLaboratorio(
            @RequestParam(required = false) String codigoCurso,
            HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            return ResponseEntity.ok(dao.obtenerGruposLaboratorio(codigoCurso));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // ESTADÍSTICAS
    // ----------------------------------------------------------------------------------------
    @GetMapping("/estadisticas")
    public ResponseEntity<?> obtenerEstadisticas(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            return ResponseEntity.ok(dao.obtenerEstadisticasMatricula());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // ESTADO MATRÍCULA (actualiza antes de consultar)
    // ----------------------------------------------------------------------------------------
    @GetMapping("/estado-matricula")
    public ResponseEntity<?> obtenerEstadoMatricula(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            // Actualiza estados antes de consultar
            dao.actualizarEstadosPeriodos();
            
            boolean activo = dao.hayPeriodoActivoLaboratorios();

            Map<String, Object> resp = new HashMap<>();
            resp.put("matriculaActiva", activo);
            resp.put("estado", activo ? "ABIERTO" : "CERRADO");

            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // HABILITAR MATRÍCULA (solo cambia estado)
    // ----------------------------------------------------------------------------------------
    @PostMapping("/habilitar-matricula")
    public ResponseEntity<?> habilitarMatricula(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            String codigoCurso = (String) payload.get("codigoCurso");
            boolean habilitar = (Boolean) payload.getOrDefault("habilitar", true);

            boolean result = dao.habilitarMatriculaLaboratorio(codigoCurso, habilitar);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("mensaje", result ?
                    "Matrícula actualizada correctamente" :
                    "No se realizaron cambios");

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // GUARDAR PERÍODO DE MATRÍCULA
    // ----------------------------------------------------------------------------------------
    @PostMapping("/guardar-periodo")
    public ResponseEntity<?> guardarPeriodo(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        System.out.println("\n==============================");
        System.out.println(">>> PETICIÓN RECIBIDA: /guardar-periodo");
        System.out.println("Payload recibido: " + payload);
        System.out.println("==============================\n");

        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            // Extraer valores del payload
            String codigoCurso = payload.get("codigoCurso") != null 
                ? payload.get("codigoCurso").toString() 
                : null;
                
            String fechaInicio = (String) payload.get("fechaInicio");
            String fechaFin = (String) payload.get("fechaFin");
            
            // Manejar cupos como Object primero
            Object cuposObj = payload.get("cupos");
            Integer cupos = null;
            if (cuposObj instanceof Integer) {
                cupos = (Integer) cuposObj;
            } else if (cuposObj instanceof String) {
                cupos = Integer.parseInt((String) cuposObj);
            } else if (cuposObj instanceof Number) {
                cupos = ((Number) cuposObj).intValue();
            }

            // Validaciones
            if (fechaInicio == null || fechaInicio.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "mensaje", "Fecha de inicio requerida"));
            }
            
            if (fechaFin == null || fechaFin.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "mensaje", "Fecha de fin requerida"));
            }
            
            if (cupos == null || cupos < 1) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "mensaje", "Cupos inválidos"));
            }

            // Parsear fechas
            LocalDateTime inicio = LocalDateTime.parse(fechaInicio);
            LocalDateTime fin = LocalDateTime.parse(fechaFin);

            // Validar lógica de fechas
            if (fin.isBefore(inicio)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, 
                        "mensaje", "La fecha de fin debe ser posterior a la de inicio"));
            }

            // Guardar en BD
            int idPeriodo = dao.guardarPeriodoMatricula(
                    codigoCurso, inicio, fin, cupos
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("idPeriodo", idPeriodo);
            resp.put("mensaje", "Período guardado correctamente");

            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "mensaje", "Error: " + ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // LISTAR PERÍODOS (actualiza estados antes)
    // ----------------------------------------------------------------------------------------
    @GetMapping("/periodos-matricula")
    public ResponseEntity<?> listarPeriodos(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            // Actualiza estados antes de listar
            dao.actualizarEstadosPeriodos();
            
            return ResponseEntity.ok(dao.obtenerPeriodosMatricula());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // ----------------------------------------------------------------------------------------
    // ELIMINAR PERÍODO
    // ----------------------------------------------------------------------------------------
    @DeleteMapping("/periodos/{id}")
    public ResponseEntity<?> eliminarPeriodo(
            @PathVariable int id,
            HttpSession session) {

        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            boolean result = dao.eliminarPeriodoMatricula(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("mensaje", result ? "Período eliminado" : "No existe el período");

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }
}