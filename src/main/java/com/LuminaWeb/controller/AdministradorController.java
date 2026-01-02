package com.LuminaWeb.controller;

import com.LuminaWeb.dao.AdministradorDAO;
import com.LuminaWeb.dao.AdministradorDAO.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/api")
public class AdministradorController {

    private final AdministradorDAO dao;

    @Autowired
    public AdministradorController(AdministradorDAO dao) {
        this.dao = dao;
    }

// 1. DASHBOARD
    @GetMapping("/estadisticas")
    public ResponseEntity<?> getEstadisticas(HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            Estadisticas stats = dao.obtenerEstadisticas();
            return ResponseEntity.ok(stats);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @GetMapping("/actividad-reciente")
    public ResponseEntity<?> getActividadReciente(
            @RequestParam(defaultValue = "5") int limite,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            List<ActividadReciente> actividades = dao.obtenerActividadReciente(limite);
            return ResponseEntity.ok(actividades);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

// 2. GESTIÓN DE USUARIOS 
    @GetMapping("/usuarios")
    public ResponseEntity<?> getTodosUsuarios(HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }
            System.out.println("[ADMIN] solicitando lista de usuarios para sesión: " + (session != null ? session.getAttribute("usuario") : "<no-session>"));
            List<Usuario> usuarios = dao.obtenerTodosUsuarios();
            System.out.println("[ADMIN] usuarios encontrados: " + (usuarios == null ? 0 : usuarios.size()));
            return ResponseEntity.ok(usuarios);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String correo = (String) payload.get("correo");
            String password = (String) payload.get("password");
            String salt = (String) payload.get("salt");
            int tipoId = ((Number) payload.get("tipoId")).intValue();
            String nombreCompleto = (String) payload.get("nombreCompleto");

            if (correo == null || password == null || nombreCompleto == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResp("Datos incompletos"));
            }

            boolean success = dao.crearUsuario(correo, password, salt, tipoId, nombreCompleto);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Usuario creado correctamente" : "Error al crear usuario");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    @PutMapping("/usuarios/{idUsuario}/estado")
    public ResponseEntity<?> cambiarEstadoUsuario(
            @PathVariable int idUsuario,
            @RequestBody Map<String, String> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String nuevoEstado = payload.get("estado");
            if (nuevoEstado == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResp("Estado no especificado"));
            }

            boolean success = dao.cambiarEstadoUsuario(idUsuario, nuevoEstado);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Estado actualizado" : "Error al actualizar estado");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    @DeleteMapping("/usuarios/{idUsuario}")
    public ResponseEntity<?> eliminarUsuario(
            @PathVariable int idUsuario,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            boolean success = dao.eliminarUsuario(idUsuario);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Usuario eliminado" : "Error al eliminar usuario");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

// 3. GESTIÓN DE PROFESORES
    @GetMapping("/profesores")
    public ResponseEntity<?> getTodosProfesores(HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            List<Profesor> profesores = dao.obtenerTodosProfesores();
            return ResponseEntity.ok(profesores);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @PutMapping("/profesores/{idDocente}")
    public ResponseEntity<?> actualizarProfesor(
            @PathVariable int idDocente,
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String apellidosNombres = (String) payload.get("apellidosNombres");
            String departamento = (String) payload.get("departamento");
            boolean esResponsableTeoria = (Boolean) payload.getOrDefault("esResponsableTeoria", false);

            //Optional: tipoClase (TEORIA, LABORATORIO, AMBOS) 
            String tipoClase = (String) payload.getOrDefault("tipoClase", null);
            if (tipoClase != null) {
                String tc = tipoClase.toUpperCase();
                esResponsableTeoria = "TEORIA".equals(tc) || "AMBOS".equals(tc);
            }

            boolean success = dao.actualizarProfesor(idDocente, apellidosNombres, departamento, esResponsableTeoria);

            int gruposAsignados = 0;
            if (payload.containsKey("grupoIdToAssign") && payload.get("grupoIdToAssign") != null) {
                int grupoId = ((Number) payload.get("grupoIdToAssign")).intValue();
                gruposAsignados = dao.asignarGrupoAProfesor(grupoId, idDocente);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            String baseMsg = success ? "Profesor actualizado" : "Error al actualizar";
            if (gruposAsignados > 0) baseMsg += " - " + gruposAsignados + " horarios actualizados para el grupo asignado";
            else if (payload.containsKey("grupoIdToAssign") && payload.get("grupoIdToAssign") != null) baseMsg += " - No se encontraron horarios existentes para el grupo seleccionado (ver Asignar Horarios).";
            resp.put("mensaje", baseMsg);
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

@PutMapping("/profesores/{idDocente}/completo")
public ResponseEntity<?> actualizarProfesorCompleto(
        @PathVariable int idDocente,
        @RequestBody Map<String, Object> payload,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        String apellidosNombres = (String) payload.get("apellidosNombres");
        String correo = (String) payload.get("correoInstitucional");
        String departamento = (String) payload.get("departamento");
        String password = (String) payload.get("password");

        boolean success = dao.actualizarProfesor(idDocente, apellidosNombres, correo, departamento, password);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success ? "Profesor actualizado correctamente" : "Error al actualizar");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

@PostMapping("/profesores/{idDocente}/asignar-curso")
public ResponseEntity<?> asignarCursoProfesor(
        @PathVariable int idDocente,
        @RequestBody Map<String, Object> payload,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        int grupoId = ((Number) payload.get("grupoId")).intValue();
        boolean success = dao.asignarCursoAProfesor(idDocente, grupoId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success ? "Curso asignado correctamente" : "Error al asignar curso");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

@DeleteMapping("/profesores/{idDocente}/desasignar-curso/{grupoId}")
public ResponseEntity<?> desasignarCursoProfesor(
        @PathVariable int idDocente,
        @PathVariable int grupoId,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        boolean success = dao.desasignarCursoDeProfesor(idDocente, grupoId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success ? "Curso desasignado correctamente" : "Error al desasignar curso");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

// 4. GESTIÓN DE ESTUDIANTES
    @GetMapping("/estudiantes")
    public ResponseEntity<?> getTodosEstudiantes(HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            List<Estudiante> estudiantes = dao.obtenerTodosEstudiantes();
            return ResponseEntity.ok(estudiantes);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @PutMapping("/estudiantes/{cui}")
    public ResponseEntity<?> actualizarEstudiante(
            @PathVariable String cui,
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String apellidosNombres = (String) payload.get("apellidosNombres");
            int numeroMatricula = ((Number) payload.get("numeroMatricula")).intValue();
            String estado = (String) payload.get("estado");

            boolean success = dao.actualizarEstudiante(cui, apellidosNombres, numeroMatricula, estado);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Estudiante actualizado" : "Error al actualizar");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

@GetMapping("/grupos-disponibles")
public ResponseEntity<?> getGruposDisponibles(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<GrupoCurso> grupos = dao.obtenerGruposDisponibles();
        return ResponseEntity.ok(grupos);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@PutMapping("/estudiantes/{cui}/completo")
public ResponseEntity<?> actualizarEstudianteCompleto(
        @PathVariable String cui,
        @RequestBody Map<String, Object> payload,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        String apellidosNombres = (String) payload.get("apellidosNombres");
        String correo = (String) payload.get("correoInstitucional");
        int numeroMatricula = ((Number) payload.get("numeroMatricula")).intValue();
        String estado = (String) payload.get("estadoEstudiante");
        String password = (String) payload.get("password");

        boolean success = dao.actualizarEstudiante(cui, apellidosNombres, correo, numeroMatricula, estado, password);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success ? "Estudiante actualizado correctamente" : "Error al actualizar");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

@PostMapping("/estudiantes/{cui}/asignar-curso")
public ResponseEntity<?> asignarCursoEstudiante(
        @PathVariable String cui,
        @RequestBody Map<String, Object> payload,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        int grupoId = ((Number) payload.get("grupoId")).intValue();
        boolean success = dao.asignarCursoAEstudiante(cui, grupoId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success ? "Curso asignado correctamente" : "Error al asignar curso");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

@DeleteMapping("/estudiantes/{cui}/desasignar-curso/{grupoId}")
public ResponseEntity<?> desasignarCursoEstudiante(
        @PathVariable String cui,
        @PathVariable int grupoId,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        boolean success = dao.desasignarCursoDeEstudiante(cui, grupoId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success ? "Curso desasignado correctamente" : "Error al desasignar curso");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

// 5. GESTIÓN DE EDITAR CURSOS
    @GetMapping("/cursos")
    public ResponseEntity<?> getTodosCursos(HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            List<Curso> cursos = dao.obtenerTodosCursos();
            return ResponseEntity.ok(cursos);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @PostMapping("/cursos")
    public ResponseEntity<?> crearCurso(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String codigoCurso = (String) payload.get("codigoCurso");
            String nombreCurso = (String) payload.get("nombreCurso");
            boolean tieneLaboratorio = (Boolean) payload.getOrDefault("tieneLaboratorio", false);
            int numeroGruposTeoria = ((Number) payload.getOrDefault("numeroGruposTeoria", 1)).intValue();
            int numeroGruposLaboratorio = ((Number) payload.getOrDefault("numeroGruposLaboratorio", 0)).intValue();

            boolean success = dao.crearCurso(codigoCurso, nombreCurso, tieneLaboratorio, 
                                            numeroGruposTeoria, numeroGruposLaboratorio);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Curso creado" : "Error al crear curso");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    @PostMapping("/grupos")
    public ResponseEntity<?> crearGrupo(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String codigoCurso = (String) payload.get("codigoCurso");
            int idCiclo = ((Number) payload.get("idCiclo")).intValue();
            String letraGrupo = (String) payload.get("letraGrupo");
            String tipoClase = (String) payload.get("tipoClase");
            int capacidadMaxima = ((Number) payload.getOrDefault("capacidadMaxima", 40)).intValue();

            boolean success = dao.crearGrupoCurso(codigoCurso, idCiclo, letraGrupo, tipoClase, capacidadMaxima);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Grupo creado" : "Error al crear grupo");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

// 6. INFORMACION DE CURSOS en CursoController2...

// 7. GESTIÓN DE HORARIOS
    @GetMapping("/horarios")
    public ResponseEntity<?> getTodosHorarios(HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            List<Horario> horarios = dao.obtenerTodosHorarios();
            return ResponseEntity.ok(horarios);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

    @GetMapping("/horarios/docente/{idDocente}")
    public ResponseEntity<?> getHorariosDocente(
            @PathVariable int idDocente,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            List<Horario> horarios = dao.obtenerHorariosPorDocente(idDocente);
            return ResponseEntity.ok(horarios);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResp("Error: " + ex.getMessage()));
        }
    }

@PostMapping("/horarios")
public ResponseEntity<?> crearHorario(
        @RequestBody Map<String, Object> payload,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        int grupoId = ((Number) payload.get("grupoId")).intValue();
        String numeroSalon = (String) payload.get("numeroSalon");
        String diaSemana = (String) payload.get("diaSemana");
        String horaInicio = (String) payload.get("horaInicio");
        String horaFin = (String) payload.get("horaFin");
        
        //idDocente ahora es opcional
        Integer idDocente = payload.get("idDocente") != null 
            ? ((Number) payload.get("idDocente")).intValue() 
            : null;

        boolean success = dao.crearHorario(grupoId, numeroSalon, diaSemana, horaInicio, horaFin, idDocente);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("mensaje", success 
            ? "Horario creado correctamente" 
            : "Error al crear horario (puede haber conflicto)");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("mensaje", "Error: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

    @PutMapping("/horarios/{idHorario}")
    public ResponseEntity<?> actualizarHorario(
            @PathVariable int idHorario,
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            String numeroSalon = (String) payload.get("numeroSalon");
            String diaSemana = (String) payload.get("diaSemana");
            String horaInicio = (String) payload.get("horaInicio");
            String horaFin = (String) payload.get("horaFin");
            int idDocente = ((Number) payload.get("idDocente")).intValue();

            boolean success = dao.actualizarHorario(idHorario, numeroSalon, diaSemana, horaInicio, horaFin, idDocente);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Horario actualizado" : "Error al actualizar");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    @DeleteMapping("/horarios/{idHorario}")
    public ResponseEntity<?> eliminarHorario(
            @PathVariable int idHorario,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            boolean success = dao.eliminarHorario(idHorario);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Horario eliminado" : "Error al eliminar");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    @DeleteMapping("/reservas/{idReserva}")
    public ResponseEntity<?> eliminarReserva(
            @PathVariable int idReserva,
            HttpSession session) {
        try {
            if (!esAdmin(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResp("No autorizado"));
            }

            boolean success = dao.eliminarReserva(idReserva);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", success);
            resp.put("mensaje", success ? "Reserva cancelada" : "Error al cancelar");
            
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("mensaje", "Error: " + ex.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

//Obtiene contadores para el dashboard de horarios
@GetMapping("/horarios/contadores")
public ResponseEntity<?> getContadoresHorarios(HttpSession session) {
    try {
        if (!esAdmin(session)) {
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

//Obtiene lista simplificada de docentes
@GetMapping("/listas/docentes")
public ResponseEntity<?> getListaDocentes(HttpSession session) {
    try {
        if (!esAdmin(session)) {
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

//Obtiene lista simplificada de estudiantes
@GetMapping("/listas/estudiantes")
public ResponseEntity<?> getListaEstudiantes(HttpSession session) {
    try {
        if (!esAdmin(session)) {
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

//Obtiene lista de salones
@GetMapping("/listas/salones")
public ResponseEntity<?> getListaSalones(HttpSession session) {
    try {
        if (!esAdmin(session)) {
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

//Obtiene horarios de un estudiante específico
@GetMapping("/horarios/estudiante/{cui}")
public ResponseEntity<?> getHorariosEstudiante(
        @PathVariable String cui,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Horario> horarios = dao.obtenerHorariosPorEstudiante(cui);
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
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Horario> horarios = dao.obtenerHorariosPorSalon(numeroSalon);
        return ResponseEntity.ok(horarios);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Obtiene detalle de un horario para edición
@GetMapping("/horarios/{idHorario}/detalle")
public ResponseEntity<?> getDetalleHorario(
        @PathVariable int idHorario,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        Horario horario = dao.obtenerHorarioPorId(idHorario);
        
        if (horario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Horario no encontrado"));
        }

        return ResponseEntity.ok(horario);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//Verifica si hay conflictos antes de crear/actualizar
@PostMapping("/horarios/verificar-conflicto")
public ResponseEntity<?> verificarConflicto(
        @RequestBody Map<String, Object> payload,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        String numeroSalon = (String) payload.get("numeroSalon");
        String diaSemana = (String) payload.get("diaSemana");
        String horaInicio = (String) payload.get("horaInicio");
        String horaFin = (String) payload.get("horaFin");
        Integer idHorarioExcluir = payload.get("idHorarioExcluir") != null 
            ? ((Number) payload.get("idHorarioExcluir")).intValue() 
            : null;

        boolean hayConflicto = dao.verificarConflictoHorario(
            numeroSalon, diaSemana, horaInicio, horaFin, idHorarioExcluir
        );

        Map<String, Object> resp = new HashMap<>();
        resp.put("hayConflicto", hayConflicto);
        resp.put("mensaje", hayConflicto 
            ? "Hay conflicto con otro horario o reserva existente" 
            : "No hay conflictos");
        
        return ResponseEntity.ok(resp);

    } catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> resp = new HashMap<>();
        resp.put("hayConflicto", true);
        resp.put("mensaje", "Error al verificar: " + ex.getMessage());
        return ResponseEntity.ok(resp);
    }
}

// 8. REPORTES
@GetMapping("/reportes/estudiantes-matriculados")
public ResponseEntity<?> getReporteEstudiantesMatriculados(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> reporte = dao.obtenerReporteEstudiantesMatriculados();
        return ResponseEntity.ok(reporte);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al generar reporte: " + ex.getMessage()));
    }
}

@GetMapping("/reportes/curso/{codigoCurso}")
public ResponseEntity<?> getReporteCurso(
        @PathVariable String codigoCurso,
        HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        Map<String, Object> reporte = dao.obtenerReporteCurso(codigoCurso);
        return ResponseEntity.ok(reporte);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al generar reporte: " + ex.getMessage()));
    }
}

@GetMapping("/reportes/docentes")
public ResponseEntity<?> getReporteDocentes(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> reporte = dao.obtenerReporteDocentes();
        return ResponseEntity.ok(reporte);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error al generar reporte: " + ex.getMessage()));
    }
}

//Método auxiliar para que visualice la ruta correcta :v
private String normalizarRuta(String ruta) {
    if (ruta == null || ruta.trim().isEmpty()) {
        return "";
    }
    return ruta.replace("/", "\\");
}

// 9. VER PDFs
@GetMapping("/cursos-activos")
public ResponseEntity<?> obtenerCursosActivos(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> cursos = dao.obtenerCursosActivos();
        return ResponseEntity.ok(cursos);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@GetMapping("/tipos-evaluacion")
public ResponseEntity<?> obtenerTiposEvaluacion(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> tipos = dao.obtenerTiposEvaluacion();
        return ResponseEntity.ok(tipos);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@GetMapping("/silabos/todos")
public ResponseEntity<?> obtenerTodosSilabos(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> silabos = dao.obtenerTodosSilabos();
        return ResponseEntity.ok(silabos);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@GetMapping("/examenes/todos")
public ResponseEntity<?> obtenerTodosExamenes(HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        List<Map<String, Object>> examenes = dao.obtenerTodosExamenes();
        return ResponseEntity.ok(examenes);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

//ver?ruta=...
@GetMapping("/silabos/ver")
public ResponseEntity<?> verSilabo(@RequestParam("ruta") String rutaArchivo, HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        String rutaLimpia = normalizarRuta(rutaArchivo);
        File file = new File(rutaLimpia);
        
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Archivo no encontrado"));
        }
        
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

//ver?ruta=...
@GetMapping("/examenes/ver")
public ResponseEntity<?> verExamenPdf(@RequestParam("ruta") String rutaArchivo, HttpSession session) {
    try {
        if (!esAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autorizado"));
        }

        String rutaLimpia = normalizarRuta(rutaArchivo);
        File file = new File(rutaLimpia);
        
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Archivo no encontrado"));
        }
        
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

// 0. MÉTODOS AUXILIARES (DE VARIAS SECCIONES)
    private boolean esAdmin(HttpSession session) {
        if (session == null || session.getAttribute("usuario") == null) {
            return false;
        }
        
        //Verificar que el usuario sea ADMINISTRADOR (tipo_id = 4)
        Object tipoObj = session.getAttribute("tipo_usuario");
        if (tipoObj == null) return false;
        
        String tipo = tipoObj.toString();
        return "ADMINISTRADOR".equals(tipo) || "4".equals(tipo);
    }

    private static class ErrorResp {
        public String error;
        public ErrorResp(String e) { this.error = e; }
    }
}