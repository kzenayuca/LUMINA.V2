package com.LuminaWeb.controller;

import com.LuminaWeb.dao.ProfesorDAO;
import com.LuminaWeb.dao.ProfesorDAO.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/**
 * Controlador REST unificado para operaciones del docente.
 *
 * Rutas:
 *  GET  /api/profesores/me                    -> datos del profesor autenticado
 *  GET  /api/profesores/me/horarios           -> horarios del profesor
 *  GET  /api/profesores/me/cursos             -> cursos que dicta
 *  GET  /api/profesores/me/estadisticas       -> estadísticas generales
 *  GET  /api/profesores/cursos/{grupoId}/estudiantes -> estudiantes con notas
 *  POST /api/profesores/notas/guardar         -> guardar nota individual
 *  POST /api/profesores/notas/upload          -> subir notas por Excel
 *  GET  /api/profesores/periodos-activos      -> períodos de ingreso habilitados
 *  GET  /api/profesores/evaluaciones-habilitadas -> tipos de evaluación habilitados
 *  GET /api/profesores                -> listar todos los profesores
 *  GET /api/profesores/me             -> profesor autenticado (según session "usuario")
 *  GET /api/profesores/me/horarios    -> horarios del profesor autenticado
 *  POST /api/profesores/reservas      -> crear reserva
 *  GET /api/profesores/me/reservas    -> obtener reservas del profesor
 *  DELETE /api/profesores/reservas    -> cancelar reserva
 */
@RestController
@RequestMapping("/doc/api/profesores")
public class ProfesorController {

    private final ProfesorDAO dao;

    @Autowired
    public ProfesorController(ProfesorDAO dao) {
        this.dao = dao;
    }

// 0. INFORMACIÓN DEL PROFESOR
    //listar todos los profesores
    @GetMapping({"", "/"})
    public ResponseEntity<?> listAll() {
        try {
            List<Profesor> lista = dao.obtenerTodos();
            return ResponseEntity.ok(lista);
        } catch (DataAccessException dae) {
            dae.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    //profesor autenticado
    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            
            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            return ResponseEntity.ok(p);
            
        } catch (DataAccessException dae) {
            dae.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

// 1. DASHBOARD DEL PROFESOR
    //cursos que dicta
    @GetMapping("/me/cursos")
    public ResponseEntity<?> getMeCursos(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            
            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            List<CursoDocente> cursos = dao.obtenerCursosDocente(p.idUsuario);
            return ResponseEntity.ok(cursos);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    //estadísticas del profesor 
    @GetMapping("/me/estadisticas")
    public ResponseEntity<?> getMeEstadisticas(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            
            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            EstadisticasProfesor stats = dao.obtenerEstadisticasProfesor(p.idUsuario);
            return ResponseEntity.ok(stats);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

// 2. ASISTENCIA en otro archivo...

// 3. GESTIÓN DE NOTAS
    //guardar nota individual 
    @PostMapping("/notas/guardar")
    public ResponseEntity<?> guardarNota(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }

            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            //Obtener id_docente usando helper en DAO
            var optDocente = dao.obtenerIdDocentePorIdUsuario(p.idUsuario);
            if (optDocente.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResp("No se encontró el registro de docente"));
            }

            int idDocente = optDocente.get();

            //Extraer parámetros
            int idMatricula = ((Number) payload.get("idMatricula")).intValue();
            int tipoEvalId = ((Number) payload.get("tipoEvalId")).intValue();
            double calificacion = ((Number) payload.get("calificacion")).doubleValue();

            //Validar calificación
            if (calificacion < 0 || calificacion > 20) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "La calificación debe estar entre 0 y 20");
                return ResponseEntity.ok(resp);
            }

            boolean success = dao.guardarNota(idMatricula, tipoEvalId, calificacion, idDocente);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Nota guardada correctamente" : "Error al guardar nota");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    //subir notas por Excel/CSV 
    @PostMapping("/notas/upload")
    public ResponseEntity<?> uploadNotas(
            @RequestParam("file") MultipartFile file,
            @RequestParam("grupoId") int grupoId,
            @RequestParam("tipoEvalId") int tipoEvalId,
            HttpSession session) {
        
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }

            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            //Obtener id_docente usando helper en DAO
            var optDocente = dao.obtenerIdDocentePorIdUsuario(p.idUsuario);
            if (optDocente.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResp("No se encontró el registro de docente"));
            }

            int idDocente = optDocente.get();

            //Validar archivo
            if (file.isEmpty()) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "Archivo vacío");
                return ResponseEntity.ok(resp);
            }

            //Procesar archivo
            Map<String, Object> result = dao.procesarArchivoNotas(
                file, grupoId, tipoEvalId, idDocente
            );

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

//PERÍODOS Y EVALUACIONES HABILITADAS
    //períodos de ingreso habilitados 
    @GetMapping("/periodos-activos")
    public ResponseEntity<?> getPeriodosActivos(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }

            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            List<PeriodoIngresoNotas> periodos = dao.obtenerPeriodosActivosDocente(p.idUsuario);
            return ResponseEntity.ok(periodos);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @GetMapping("/evaluaciones-habilitadas")
    public ResponseEntity<?> getEvaluacionesHabilitadas(
            @RequestParam String codigoCurso,
            HttpSession session) {
        
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }

            List<Map<String, Object>> evaluaciones = 
                dao.obtenerTiposEvaluacionHabilitados(codigoCurso);
            
            return ResponseEntity.ok(evaluaciones);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

//subir examen PDF (SOLO PARCIALES) 
@PostMapping("/examenes/upload")
public ResponseEntity<?> uploadExamen(
        @RequestParam("file") MultipartFile file,
        @RequestParam("grupoId") int grupoId,
        @RequestParam("tipoEvalId") int tipoEvalId,  
        @RequestParam("tipo") String tipo,
        HttpSession session) {
    
    try {
        System.out.println("=== SUBIENDO EXAMEN PDF ===");
        System.out.println("Grupo ID: " + grupoId);
        System.out.println("Tipo Eval ID: " + tipoEvalId);
        System.out.println("Tipo: " + tipo);
        System.out.println("Archivo: " + file.getOriginalFilename());
        
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        String correo = (String) session.getAttribute("usuario");
        Profesor p = dao.obtenerPorCorreo(correo);
        
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Profesor no encontrado"));
        }

        //Obtener id_docente
        var optDocente = dao.obtenerIdDocentePorIdUsuario(p.idUsuario);
        if (optDocente.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResp("No se encontró el registro de docente"));
        }
        int idDocente = optDocente.get();
        System.out.println("ID Docente: " + idDocente);

        //Validar que sea evaluación PARCIAL (1, 2 o 3)
        if (tipoEvalId < 1 || tipoEvalId > 3) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Solo se pueden subir exámenes para evaluaciones parciales (EP1, EP2, EP3)");
            return ResponseEntity.ok(resp);
        }

        //Validar tipo
        if (!tipo.equals("alta") && !tipo.equals("baja")) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Tipo de examen inválido");
            return ResponseEntity.ok(resp);
        }

        //Validar archivo
        if (file.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Archivo vacío");
            return ResponseEntity.ok(resp);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Solo se permiten archivos PDF");
            return ResponseEntity.ok(resp);
        }

        //Verificar si ya existe
        if (dao.existeExamenSubido(grupoId, tipoEvalId, tipo)) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Ya existe un examen " + (tipo.equals("alta") ? "de nota alta" : "de nota baja") + " para esta evaluación");
            return ResponseEntity.ok(resp);
        }

        //Ruta base correcta
        String rutaBase = "C:/temp/lumina";
        System.out.println("Ruta base: " + rutaBase);

        boolean exito = dao.guardarExamenPDF(file, grupoId, tipoEvalId, tipo, rutaBase, idDocente);

        System.out.println("Resultado guardado: " + exito);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", exito);
        resp.put("mensaje", exito ? "Examen subido correctamente" : "Error al subir examen");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        System.out.println("ERROR EXCEPTION: " + ex.getMessage());
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

//cursos que dicta (SOLO TEORÍA para notas) 
@GetMapping("/me/cursos-teoria")
public ResponseEntity<?> getMeCursosTeoria(HttpSession session) {
    try {
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }
        
        String correo = (String) session.getAttribute("usuario");
        Profesor p = dao.obtenerPorCorreo(correo);
        
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Profesor no encontrado"));
        }

        //Obtener todos los cursos
        List<CursoDocente> todosCursos = dao.obtenerCursosDocente(p.idUsuario);
        
        //Filtrar solo TEORÍA 
        List<CursoDocente> cursosTeoria = new ArrayList<>();
        for (CursoDocente curso : todosCursos) {
            if ("TEORIA".equalsIgnoreCase(curso.tipoClase)) {
                cursosTeoria.add(curso);
            }
        }
        
        return ResponseEntity.ok(cursosTeoria);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

// 4. ESTADÍSTICAS (falta mejorar)
@GetMapping("/cursos/{grupoId}/avance-temario")
public ResponseEntity<?> getAvanceTemario(
        @PathVariable int grupoId,
        HttpSession session) {
    
    try {
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        //Obtener datos del grupo usando el DAO
        Map<String, Object> datosGrupo = dao.obtenerDatosGrupo(grupoId);
        
        if (datosGrupo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Grupo no encontrado"));
        }

        String codigoCurso = (String) datosGrupo.get("codigo_curso");
        int idCiclo = ((Number) datosGrupo.get("id_ciclo")).intValue();

        Map<String, Object> avance = dao.obtenerAvanceTemario(codigoCurso, idCiclo);
        return ResponseEntity.ok(avance);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@GetMapping("/cursos/{grupoId}/estadisticas-asistencia")
public ResponseEntity<?> getEstadisticasAsistencia(
        @PathVariable int grupoId,
        HttpSession session) {
    
    try {
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        Map<String, Object> stats = dao.obtenerEstadisticasAsistencia(grupoId);
        return ResponseEntity.ok(stats);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@GetMapping("/cursos/{grupoId}/indicadores")
public ResponseEntity<?> getIndicadoresRapidos(
        @PathVariable int grupoId,
        HttpSession session) {
    
    try {
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        Map<String, Object> indicadores = dao.obtenerIndicadoresRapidos(grupoId);
        return ResponseEntity.ok(indicadores);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

// 5. HORARIOS
    //horarios del profesor 
    @GetMapping("/me/horarios")
    public ResponseEntity<?> getMeHorarios(HttpSession session) {
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            
            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            int idUsuario = p.idUsuario;
            List<Horario> horarios = dao.obtenerHorariosPorIdUsuario(p.idUsuario);
            return ResponseEntity.ok(horarios);

        } catch (DataAccessException dae) {
            dae.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }
    
// 6. RESERVAS
    //crear nueva reserva
@PostMapping("/reservas")
public ResponseEntity<?> crearReserva(@RequestBody Map<String, Object> reservaData, HttpSession session) {
    try {
        System.out.println("=== CREANDO RESERVA ===");
        System.out.println("Datos recibidos: " + reservaData);
        
        if (session == null || session.getAttribute("usuario") == null) {
            System.out.println("ERROR: No autenticado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }
        
        String correo = (String) session.getAttribute("usuario");
        System.out.println("Correo de sesión: " + correo);
        
        Profesor p = dao.obtenerPorCorreo(correo);
        if (p == null) {
            System.out.println("ERROR: Profesor no encontrado para correo: " + correo);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Profesor no encontrado"));
        }

        System.out.println("Profesor encontrado: ID=" + p.idUsuario + ", Nombre=" + p.apellidosNombres);

        //Extraer datos con verificación de nulos
        String numeroSalon = (String) reservaData.get("ambiente");
        String diaSemana = (String) reservaData.get("dia");
        String fechaReserva = (String) reservaData.get("fecha");
        String horaInicio = (String) reservaData.get("horaInicio");
        String horaFin = (String) reservaData.get("horaFin");
        String motivo = (String) reservaData.get("motivo");
        String descripcion = (String) reservaData.get("descripcion");

        System.out.println("Datos extraídos:");
        System.out.println("  Ambiente: " + numeroSalon);
        System.out.println("  Día: " + diaSemana);
        System.out.println("  Hora inicio: " + horaInicio);
        System.out.println("  Hora fin: " + horaFin);
        System.out.println("  Motivo: " + motivo);
        System.out.println("  Descripción: " + descripcion);

        //Validar datos requeridos
        if (numeroSalon == null || diaSemana == null || horaInicio == null || horaFin == null || motivo == null) {
            System.out.println("ERROR: Datos incompletos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResp("Datos incompletos"));
        }

        System.out.println("Insertando reserva para profesor ID: " + p.idUsuario);

        //Verificar límite semanal antes de insertar para dar feedback inmediato
        Optional<Integer> optDocentePre = dao.obtenerIdDocentePorIdUsuario(p.idUsuario);
        if (optDocentePre.isPresent()) {
            int idDocentePre = optDocentePre.get();
            int semanaCountPre = dao.contarReservasSemanaPorDocente(idDocentePre, fechaReserva);
            if (semanaCountPre >= 2) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("mensaje", "Límite semanal alcanzado: no puede crear más de 2 reservas en la misma semana");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
            }
        }

        boolean exito = dao.insertarReserva(p.idUsuario, numeroSalon, diaSemana, fechaReserva, horaInicio, horaFin, motivo, descripcion);

        if (exito) {
            System.out.println("RESERVA CREADA EXITOSAMENTE");
            return ResponseEntity.ok(new MessageResp("Reserva creada exitosamente"));
        } else {
            System.out.println("ERROR: No se pudo crear la reserva en BD - posible conflicto con horario o reserva existente");
            //intentar identificar si se debe al límite semanal
            Optional<Integer> optIdDocente = dao.obtenerIdDocentePorIdUsuario(p.idUsuario);
            if (optIdDocente.isPresent()) {
                int idDocente = optIdDocente.get();
                int semanaCount = dao.contarReservasSemanaPorDocente(idDocente, fechaReserva);
                if (semanaCount >= 2) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", false);
                    resp.put("mensaje", "Límite semanal alcanzado: no puede crear más de 2 reservas en la misma semana");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
                }
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "No se pudo crear la reserva: existe conflicto con horario fijo o con otra reserva para la misma fecha/hora");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }

    } catch (Exception ex) {
        System.out.println("ERROR EXCEPCIÓN: " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error interno: " + ex.getMessage()));
    }
}

    //obtener reservas del profesor
    @GetMapping("/me/reservas")
    public ResponseEntity<?> obtenerMisReservas(HttpSession session) {
        try {
            System.out.println("=== OBTENIENDO RESERVAS ===");
            
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            
            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            System.out.println("Buscando reservas para profesor ID: " + p.idUsuario);
            
            List<Map<String, Object>> reservas = dao.obtenerReservasPorIdDocente(p.idUsuario);

            System.out.println("Reservas encontradas: " + reservas.size());
            return ResponseEntity.ok(reservas);

        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    //cancelar reserva  
    @DeleteMapping("/reservas")
    public ResponseEntity<?> cancelarReserva(@RequestBody Map<String, Object> cancelData, HttpSession session) {
        try {
            System.out.println("=== CANCELANDO RESERVA ===");
            System.out.println("Datos: " + cancelData);
            
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }
            
            String correo = (String) session.getAttribute("usuario");
            Profesor p = dao.obtenerPorCorreo(correo);
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Profesor no encontrado"));
            }

            String numeroSalon = (String) cancelData.get("ambiente");
            String diaSemana = (String) cancelData.get("dia");
            String horaInicio = (String) cancelData.get("horaInicio");
            String fechaReserva = (String) cancelData.get("fecha");

            boolean exito = dao.cancelarReserva(p.idUsuario, numeroSalon, diaSemana, horaInicio, fechaReserva);

            if (exito) {
                return ResponseEntity.ok(new MessageResp("Reserva cancelada exitosamente"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResp("Reserva no encontrada o ya cancelada"));
            }

        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

    //Clase para mensajes de error simples 
    private static class ErrorResp {
        public String error;
        public ErrorResp(String p) { this.error = p; }
    }

    private static class MessageResp {
        public String mensaje;
        public MessageResp(String p) { this.mensaje = p; }
    }

//obtener todos los ambientes disponibles
@GetMapping("/ambientes")
public ResponseEntity<?> obtenerAmbientes() {
    try {
        System.out.println("ENDPOINT AMBIENTES LLAMADO ");
        
        List<Map<String, Object>> ambientes = dao.obtenerAmbientes();
        
        System.out.println("Ambientes enviados al frontend: " + ambientes.size());
        if (ambientes.isEmpty()) {
            System.out.println(" LISTA DE AMBIENTES VACÍA");
        } else {
            System.out.println("📋 Ambientes: " + ambientes);
        }
        
        return ResponseEntity.ok(ambientes);
        
    } catch (Exception ex) {
        System.out.println("ERROR en endpoint ambientes: " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al cargar ambientes: " + ex.getMessage()));
    }
}

//obtener horarios de todos los profesores
@GetMapping("/horarios-generales")
public ResponseEntity<?> obtenerHorariosGenerales() {
    try {
        System.out.println("=== ENDPOINT /horarios-generales LLAMADO ===");
        
        List<Map<String, Object>> horarios = dao.obtenerHorariosGenerales();
        
        System.out.println("✓ Horarios a enviar: " + horarios.size());
        
        if (horarios.isEmpty()) {
            System.out.println("⚠️ NO HAY HORARIOS PARA ENVIAR");
        }
        
        return ResponseEntity.ok(horarios);
        
    } catch (Exception ex) {
        System.out.println("❌ ERROR en endpoint horarios-generales: " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al cargar horarios generales: " + ex.getMessage()));
    }
}

// 7. SÍLABO Y CONTENIDO en otro archivo...

// 8. REPORTES
    //estudiantes con notas 
    @GetMapping("/cursos/{grupoId}/estudiantes")
    public ResponseEntity<?> getEstudiantesConNotas(
            @PathVariable int grupoId,
            HttpSession session) {
        
        try {
            if (session == null || session.getAttribute("usuario") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autenticado"));
            }

            List<NotaEstudiante> estudiantes = dao.obtenerEstudiantesConNotas(grupoId);
            return ResponseEntity.ok(estudiantes);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }
//asistencias con fechas 
@GetMapping("/cursos/{grupoId}/asistencias")
public ResponseEntity<?> getAsistenciasPorGrupo(
        @PathVariable int grupoId,
        HttpSession session) {
    
    try {
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }

        List<Map<String, Object>> asistencias = dao.obtenerAsistenciasPorGrupo(grupoId);
        return ResponseEntity.ok(asistencias);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}
}