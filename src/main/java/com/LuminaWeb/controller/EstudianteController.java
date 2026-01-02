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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    // estudiante_dashboard
    /** GET /api/estudiantes/me/resumen-cursos
     * Devuelve el resumen de cursos matriculados del estudiante autenticado */
    @GetMapping("/me/resumen-cursos")
    public ResponseEntity<?> getResumenCursos(HttpSession session) {
        try {
            // <-- CORRECCIÓN: faltaban los operadores lógicos ("||")
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

            // <-- CORRECCIÓN: faltaba "||" entre las condiciones
            if (e.cui == null || e.cui.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResp("CUI no disponible para el estudiante"));
            }

            Map<String, Integer> resumen = dao.obtenerResumenCursos(e.cui);
            return ResponseEntity.ok(resumen);

        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error DB: " + dae.getMostSpecificCause().getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error interno: " + ex.getMessage()));
        }
    }

@GetMapping("/me/silabos")
public ResponseEntity<?> getMeSilabos(HttpSession session) {
    try {
        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResp("No autenticado"));
        }
        
        String correo = (String) session.getAttribute("usuario");
        Estudiante e = dao.obtenerPorCorreo(correo);
        
        if (e == null || e.cui == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResp("Estudiante no encontrado"));
        }

        List<EstudianteDAO.SilaboEstudiante> silabos = dao.obtenerSilabosPorCUI(e.cui);
        
        // ⭐ Procesar las rutas de manera SEGURA
        for (EstudianteDAO.SilaboEstudiante silabo : silabos) {
            if (silabo.rutaArchivo != null && !silabo.rutaArchivo.isEmpty()) {
                String rutaOriginal = silabo.rutaArchivo;
                System.out.println("Ruta original en controller: [" + rutaOriginal + "]");
                
                // Usar Path de Java para extraer el nombre 
                try {
                    Path path = Paths.get(rutaOriginal);
                    String nombreArchivo = path.getFileName().toString();
                    System.out.println("Nombre extraído con Path: [" + nombreArchivo + "]");
                    silabo.rutaArchivo = nombreArchivo;
                } catch (Exception ex) {
                    System.out.println("Error con Paths.get, usando método manual");
                    // Si falla Paths.get, usar método manual
                    String nombreArchivo = rutaOriginal;
                    
                    // Buscar el último separador (\ o /)
                    int lastBackslash = nombreArchivo.lastIndexOf('\\');
                    int lastSlash = nombreArchivo.lastIndexOf('/');
                    int lastSeparator = Math.max(lastBackslash, lastSlash);
                    
                    if (lastSeparator >= 0 && lastSeparator < nombreArchivo.length() - 1) {
                        nombreArchivo = nombreArchivo.substring(lastSeparator + 1);
                    }
                    
                    System.out.println("Nombre extraído (manual): [" + nombreArchivo + "]");
                    silabo.rutaArchivo = nombreArchivo;
                }
            }
        }
        
        return ResponseEntity.ok(silabos);

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResp("Error: " + ex.getMessage()));
    }
}

@GetMapping("/silabos/ver")
public ResponseEntity<?> verSilabo(@RequestParam String ruta) {
    try {
        System.out.println("=== DEBUG COMPLETO ===");
        System.out.println("Nombre archivo recibido: [" + ruta + "]");
        
        // Limpiar cualquier carácter extraño que pueda venir
        String nombreArchivo = ruta
            .replaceAll("[\\x00-\\x1F\\x7F]", "")  // Eliminar caracteres de control
            .replaceAll("^[A-Za-z]:", "")           // Eliminar C:, D:, etc. por si acaso
            .replaceAll(".*[/\\\\]", "")            // Extraer después del último separador
            .trim();
        
        System.out.println("Nombre limpio: [" + nombreArchivo + "]");
        
        // Validar que el nombre no esté vacío
        if (nombreArchivo.isEmpty() || nombreArchivo.length() < 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Nombre de archivo inválido");
        }
        
        // ⭐ AQUÍ defines la ruta base donde están los PDFs
        Path directorioBase = Paths.get("C:", "temp", "lumina", "silabos");
        
        // Combinar la ruta base con el nombre del archivo
        Path archivoPath = directorioBase.resolve(nombreArchivo);
        
        System.out.println("Buscando archivo en: " + archivoPath.toAbsolutePath());

        // Verificar que el archivo existe
        if (!Files.exists(archivoPath)) {
            System.out.println("❌ Archivo NO encontrado");
            
            // Lista los archivos del directorio para debug
            if (Files.exists(directorioBase)) {
                System.out.println("Archivos en el directorio:");
                Files.list(directorioBase).forEach(p -> 
                    System.out.println("  - " + p.getFileName())
                );
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Archivo no encontrado: " + nombreArchivo);
        }

        System.out.println("✓ Archivo encontrado, leyendo...");
        
        // Leer el archivo
        byte[] data = Files.readAllBytes(archivoPath);
        
        System.out.println("✓ PDF leído correctamente: " + data.length + " bytes");
        
        // Retornar el PDF
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"" + nombreArchivo + "\"")
                .body(data);

    } catch (Exception e) {
        System.out.println("❌ ERROR: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error al leer archivo: " + e.getMessage());
    }
}
// Clase para mensajes de error simples (se serializa a JSON automáticamente)
    private static class ErrorResp {
        public String error;
        public ErrorResp(String e) { this.error = e; }
    }
}