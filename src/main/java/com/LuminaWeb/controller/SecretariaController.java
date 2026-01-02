package com.LuminaWeb.controller;

import com.LuminaWeb.dao.SecretariaDAO;
import com.LuminaWeb.dao.SecretariaDAO.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;

import org.springframework.web.multipart.MultipartFile;

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

// 1. DASHBOARD
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

// 2. SUBIDA DE DATOS POR EXCEL
//Procesa archivo Excel/CSV de estudiantes
@PostMapping("/upload/estudiantes")
public ResponseEntity<?> uploadEstudiantes(
        @RequestParam("file") MultipartFile file,
        HttpSession session) {
    
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        if (file.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Archivo vacío");
            return ResponseEntity.ok(resp);
        }

        //Validar extensión
        String filename = file.getOriginalFilename();
        if (filename == null || 
            (!filename.toLowerCase().endsWith(".xlsx") && 
             !filename.toLowerCase().endsWith(".xls") && 
             !filename.toLowerCase().endsWith(".csv"))) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Formato de archivo no válido. Use .xlsx, .xls o .csv");
            return ResponseEntity.ok(resp);
        }

        Map<String, Object> result = dao.procesarArchivoEstudiantes(file);
        return ResponseEntity.ok(result);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

//Procesa archivo Excel/CSV de docentes
@PostMapping("/upload/docentes")
public ResponseEntity<?> uploadDocentes(
        @RequestParam("file") MultipartFile file,
        HttpSession session) {
    
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        if (file.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Archivo vacío");
            return ResponseEntity.ok(resp);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || 
            (!filename.toLowerCase().endsWith(".xlsx") && 
             !filename.toLowerCase().endsWith(".xls") && 
             !filename.toLowerCase().endsWith(".csv"))) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Formato de archivo no válido. Use .xlsx, .xls o .csv");
            return ResponseEntity.ok(resp);
        }

        Map<String, Object> result = dao.procesarArchivoDocentes(file);
        return ResponseEntity.ok(result);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

//Procesa archivo Excel/CSV de alumnos por asignatura
@PostMapping("/upload/alumnos-asignatura")
public ResponseEntity<?> uploadAlumnosAsignatura(
        @RequestParam("file") MultipartFile file,
        HttpSession session) {
    
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        if (file.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Archivo vacío");
            return ResponseEntity.ok(resp);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || 
            (!filename.toLowerCase().endsWith(".xlsx") && 
             !filename.toLowerCase().endsWith(".xls") && 
             !filename.toLowerCase().endsWith(".csv"))) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Formato de archivo no válido. Use .xlsx, .xls o .csv");
            return ResponseEntity.ok(resp);
        }

        Map<String, Object> result = dao.procesarArchivoAlumnosAsignatura(file);
        return ResponseEntity.ok(result);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

//Procesa archivo Excel/CSV de horarios
@PostMapping("/upload/horarios")
public ResponseEntity<?> uploadHorarios(
        @RequestParam("file") MultipartFile file,
        HttpSession session) {
    
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        if (file.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Archivo vacío");
            return ResponseEntity.ok(resp);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || 
            (!filename.toLowerCase().endsWith(".xlsx") && 
             !filename.toLowerCase().endsWith(".xls") && 
             !filename.toLowerCase().endsWith(".csv"))) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Formato de archivo no válido. Use .xlsx, .xls o .csv");
            return ResponseEntity.ok(resp);
        }

        Map<String, Object> result = dao.procesarArchivoHorarios(file);
        return ResponseEntity.ok(result);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

//Procesa archivo Excel/CSV de pesos de evaluación
@PostMapping("/upload/pesos-evaluacion")
public ResponseEntity<?> uploadPesosEvaluacion(
        @RequestParam("file") MultipartFile file,
        HttpSession session) {
    
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        if (file.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Archivo vacío");
            return ResponseEntity.ok(resp);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || 
            (!filename.toLowerCase().endsWith(".xlsx") && 
             !filename.toLowerCase().endsWith(".xls") && 
             !filename.toLowerCase().endsWith(".csv"))) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Formato de archivo no válido. Use .xlsx, .xls o .csv");
            return ResponseEntity.ok(resp);
        }

        Map<String, Object> result = dao.procesarArchivoPesosEvaluacion(file);
        return ResponseEntity.ok(result);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

// 3. VER USUARIOS 
//Lista de estudiantes con toda su información
@GetMapping("/estudiantes-completo")
public ResponseEntity<?> obtenerEstudiantesCompleto(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null)
            return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

        return ResponseEntity.ok(dao.obtenerEstudiantesCompleto());

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
    }
}

//Lista de profesores con toda su información
@GetMapping("/profesores-completo")
public ResponseEntity<?> obtenerProfesoresCompleto(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null)
            return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

        return ResponseEntity.ok(dao.obtenerProfesoresCompleto());

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
    }
}

// 4. HABILITAR PERÍODOS DE NOTAS
    //lista de cursos para selector
    @GetMapping("/cursos-activos")
    public ResponseEntity<?> obtenerCursosActivos(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            return ResponseEntity.ok(dao.obtenerCursosActivos());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    //lista de tipos de evaluación 
    @GetMapping("/tipos-evaluacion")
    public ResponseEntity<?> obtenerTiposEvaluacion(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            return ResponseEntity.ok(dao.obtenerTiposEvaluacion());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    //guardar período de ingreso de notas 
    @PostMapping("/guardar-periodo-notas")
    public ResponseEntity<?> guardarPeriodoNotas(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            //Obtener id_usuario de la sesión
            String correo = (String) session.getAttribute("usuario");
            //obtener id del usuario por correo desde el DAO
            var optId = dao.obtenerIdUsuarioPorCorreo(correo);
            if (optId.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "mensaje", "Usuario no encontrado"));
            }

            int creadoPor = optId.get();

            //Extraer parámetros
            String codigoCurso = payload.get("codigoCurso") != null 
                ? payload.get("codigoCurso").toString() 
                : null;

            Object tipoEvalIdObj = payload.get("tipoEvalId");
            Integer tipoEvalId = null;
            if (tipoEvalIdObj != null) {
                if (tipoEvalIdObj instanceof Integer) {
                    tipoEvalId = (Integer) tipoEvalIdObj;
                } else if (tipoEvalIdObj instanceof String) {
                    tipoEvalId = Integer.parseInt((String) tipoEvalIdObj);
                } else if (tipoEvalIdObj instanceof Number) {
                    tipoEvalId = ((Number) tipoEvalIdObj).intValue();
                }
            }
                
            String fechaInicio = (String) payload.get("fechaInicio");
            String fechaFin = (String) payload.get("fechaFin");

            //Validaciones
            if (fechaInicio == null || fechaInicio.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "mensaje", "Fecha de inicio requerida"));
            }
            
            if (fechaFin == null || fechaFin.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "mensaje", "Fecha de fin requerida"));
            }

            LocalDateTime inicio = LocalDateTime.parse(fechaInicio);
            LocalDateTime fin = LocalDateTime.parse(fechaFin);

            if (fin.isBefore(inicio)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, 
                        "mensaje", "La fecha de fin debe ser posterior a la de inicio"));
            }

            //Guardar en BD
            int idPeriodo = dao.guardarPeriodoIngresoNotas(
                    codigoCurso, tipoEvalId, inicio, fin, creadoPor
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("idPeriodo", idPeriodo);
            resp.put("mensaje", "Período de ingreso de notas guardado correctamente");

            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "mensaje", "Error: " + ex.getMessage()));
        }
    }

    //listar períodos 
    @GetMapping("/periodos-ingreso-notas")
    public ResponseEntity<?> listarPeriodosIngresoNotas(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            dao.actualizarEstadosPeriodosNotas();
            
            return ResponseEntity.ok(dao.obtenerPeriodosIngresoNotas());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    //eliminar período 
    @DeleteMapping("/periodos-ingreso-notas/{id}")
    public ResponseEntity<?> eliminarPeriodoIngresoNotas(
            @PathVariable int id,
            HttpSession session) {

        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            boolean result = dao.eliminarPeriodoIngresoNotas(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("mensaje", result ? "Período eliminado" : "No existe el período");

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    //verificar si hay períodos activos 
    @GetMapping("/estado-ingreso-notas")
    public ResponseEntity<?> obtenerEstadoIngresoNotas(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            dao.actualizarEstadosPeriodosNotas();
            
            boolean activo = dao.hayPeriodoActivoNotas();

            Map<String, Object> resp = new HashMap<>();
            resp.put("notasActivas", activo);
            resp.put("estado", activo ? "HABILITADO" : "DESHABILITADO");

            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

    // 5. MATRÍCULAS LABS 
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

    @GetMapping("/estado-matricula")
    public ResponseEntity<?> obtenerEstadoMatricula(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

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

    @PostMapping("/guardar-periodo")
    public ResponseEntity<?> guardarPeriodo(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            String codigoCurso = payload.get("codigoCurso") != null 
                ? payload.get("codigoCurso").toString() 
                : null;
                
            String fechaInicio = (String) payload.get("fechaInicio");
            String fechaFin = (String) payload.get("fechaFin");
            
            Object cuposObj = payload.get("cupos");
            Integer cupos = null;
            if (cuposObj instanceof Integer) {
                cupos = (Integer) cuposObj;
            } else if (cuposObj instanceof String) {
                cupos = Integer.parseInt((String) cuposObj);
            } else if (cuposObj instanceof Number) {
                cupos = ((Number) cuposObj).intValue();
            }

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

            LocalDateTime inicio = LocalDateTime.parse(fechaInicio);
            LocalDateTime fin = LocalDateTime.parse(fechaFin);

            if (fin.isBefore(inicio)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, 
                        "mensaje", "La fecha de fin debe ser posterior a la de inicio"));
            }

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

    @GetMapping("/periodos-matricula")
    public ResponseEntity<?> listarPeriodos(HttpSession session) {
        try {
            if (session.getAttribute("usuario") == null)
                return ResponseEntity.status(401).body(new ErrorResp("No autenticado"));

            dao.actualizarEstadosPeriodos();
            
            return ResponseEntity.ok(dao.obtenerPeriodosMatricula());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResp(ex.getMessage()));
        }
    }

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

// 6. EN OTRO ARCHIVO: SecVerCursos...

// 7. VER HORARIOS
//Obtiene contadores para el dashboard de horarios
@GetMapping("/horarios/contadores")
public ResponseEntity<?> getContadoresHorarios(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        Map<String, Object> contadores = new HashMap<>();
        contadores.put("totalCursos", dao.contarCursosActivos());
        contadores.put("totalAulas", dao.contarAulasDisponibles());
        
        return ResponseEntity.ok(contadores);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene lista simplificada de docentes (selector)
@GetMapping("/listas/docentes")
public ResponseEntity<?> getListaDocentes(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> docentes = dao.obtenerListaDocentes();
        return ResponseEntity.ok(docentes);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene lista simplificada de estudiantes (selector)
@GetMapping("/listas/estudiantes")
public ResponseEntity<?> getListaEstudiantes(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> estudiantes = dao.obtenerListaEstudiantes();
        return ResponseEntity.ok(estudiantes);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene lista de salones (selector)
@GetMapping("/listas/salones")
public ResponseEntity<?> getListaSalones(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> salones = dao.obtenerListaSalones();
        return ResponseEntity.ok(salones);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene horarios de un docente específico
@GetMapping("/horarios/docente/{idDocente}")
public ResponseEntity<?> getHorariosDocente(
        @PathVariable int idDocente,
        HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> horarios = dao.obtenerHorariosPorDocente(idDocente);
        return ResponseEntity.ok(horarios);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene horarios de un estudiante específico
@GetMapping("/horarios/estudiante/{cui}")
public ResponseEntity<?> getHorariosEstudiante(
        @PathVariable String cui,
        HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> horarios = dao.obtenerHorariosPorEstudiante(cui);
        return ResponseEntity.ok(horarios);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene horarios de un salón específico
@GetMapping("/horarios/salon/{numeroSalon}")
public ResponseEntity<?> getHorariosSalon(
        @PathVariable String numeroSalon,
        HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> horarios = dao.obtenerHorariosPorSalon(numeroSalon);
        return ResponseEntity.ok(horarios);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

// 8. REPORTES
//Obtiene todos los sílabos del sistema
@GetMapping("/silabos/todos")
public ResponseEntity<?> obtenerTodosSilabos(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        List<Map<String, Object>> silabos = dao.obtenerTodosSilabos();
        return ResponseEntity.ok(silabos);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene todos los exámenes (mayores y menores notas) del sistema
@GetMapping("/examenes/todos")
public ResponseEntity<?> obtenerTodosExamenes(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        List<Map<String, Object>> examenes = dao.obtenerTodosExamenes();
        return ResponseEntity.ok(examenes);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Sirve un sílabo PDF para visualización
@GetMapping("/silabos/ver")
public ResponseEntity<?> verSilabo(@RequestParam("ruta") String rutaArchivo, HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        //Limpiar y normalizar la ruta
        String rutaLimpia = normalizarRuta(rutaArchivo);        
        System.out.println("🔍 [Sílabo] Ruta recibida: " + rutaArchivo);
        System.out.println("🔍 [Sílabo] Ruta limpia: " + rutaLimpia);
        
        File file = new File(rutaLimpia);
        
        if (!file.exists()) {
            System.err.println("❌ Archivo no encontrado: " + file.getAbsolutePath());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Archivo no encontrado"));
        }        
        System.out.println("✅ Archivo encontrado: " + file.getAbsolutePath());
        
        Resource resource = new FileSystemResource(file);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", file.getName());
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al leer el archivo: " + ex.getMessage()));
    }
}

//Sirve un PDF de examen (mayores/menores notas) para visualización 
@GetMapping("/examenes/ver")
public ResponseEntity<?> verExamenPdf(@RequestParam("ruta") String rutaArchivo, HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        //Limpiar y normalizar la ruta
        String rutaLimpia = normalizarRuta(rutaArchivo);        
        System.out.println("🔍 [Examen] Ruta recibida: " + rutaArchivo);
        System.out.println("🔍 [Examen] Ruta limpia: " + rutaLimpia);
        
        File file = new File(rutaLimpia);
        
        if (!file.exists()) {
            System.err.println("❌ Archivo no encontrado: " + file.getAbsolutePath());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Archivo no encontrado"));
        }        
        System.out.println("✅ Archivo encontrado: " + file.getAbsolutePath());
        
        Resource resource = new FileSystemResource(file);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", file.getName());
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al leer el archivo: " + ex.getMessage()));
    }
}

//Endpoint de prueba para verificar si una ruta es válida
@GetMapping("/test-ruta")
public ResponseEntity<?> testRuta(@RequestParam("ruta") String rutaArchivo) {
    Map<String, Object> resultado = new HashMap<>();
    
    try {
        String rutaLimpia = normalizarRuta(rutaArchivo);
        File file = new File(rutaLimpia);
        
        resultado.put("rutaOriginal", rutaArchivo);
        resultado.put("rutaLimpia", rutaLimpia);
        resultado.put("rutaAbsoluta", file.getAbsolutePath());
        resultado.put("existe", file.exists());
        resultado.put("esArchivo", file.isFile());
        resultado.put("tamaño", file.exists() ? file.length() : 0);
        resultado.put("esLegible", file.canRead());
        
        return ResponseEntity.ok(resultado);
        
    } catch (Exception ex) {
        resultado.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultado);
    }
}

//Método auxiliar para normalizar rutas de archivos
//Convierte rutas Unix (/) a rutas Windows (\) para lectura de archivos
private String normalizarRuta(String ruta) {
    if (ruta == null || ruta.trim().isEmpty()) {
        return "";
    }
    
    System.out.println("🔍 Ruta recibida del frontend: " + ruta);
    
    //Convertir barras Unix a Windows
    String rutaLimpia = ruta.replace("/", "\\");
    
    System.out.println("🔍 Ruta convertida para Windows: " + rutaLimpia);
    
    return rutaLimpia;
}

// 9. REPORTES
//Genera reporte de estudiantes matriculados con sus cursos
@GetMapping("/reportes/estudiantes-matriculados")
public ResponseEntity<?> getReporteEstudiantesMatriculados(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        List<Map<String, Object>> reporte = dao.obtenerReporteEstudiantesMatriculados();
        return ResponseEntity.ok(reporte);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al generar reporte: " + ex.getMessage()));
    }
}

//Genera reporte completo de un curso específico
@GetMapping("/reportes/curso/{codigoCurso}")
public ResponseEntity<?> getReporteCurso(
        @PathVariable String codigoCurso,
        HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        Map<String, Object> reporte = dao.obtenerReporteCurso(codigoCurso);
        return ResponseEntity.ok(reporte);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al generar reporte: " + ex.getMessage()));
    }
}

//Genera reporte general de todos los docentes
@GetMapping("/reportes/docentes")
public ResponseEntity<?> getReporteDocentes(HttpSession session) {
    try {
        if (session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        List<Map<String, Object>> reporte = dao.obtenerReporteDocentes();
        return ResponseEntity.ok(reporte);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al generar reporte: " + ex.getMessage()));
    }
}

}