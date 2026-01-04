package com.LuminaWeb.controller;

import com.LuminaWeb.dto.ContenidoDTO;
// CursoController.java
import com.LuminaWeb.dto.CursoGrupoDTO;
import com.LuminaWeb.dto.SilaboDTO;
import com.LuminaWeb.dto.TemaDTO;
import com.LuminaWeb.dto.UnidadDTO;
import com.LuminaWeb.service.CursoService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.nio.file.*;

@RestController
@RequestMapping("/api")
public class CursoController {
    
    @Value("${lumina.silabos.dir:${java.io.tmpdir}/lumina/silabos}")
    private String silabosBaseDir;

    private final CursoService service;
    public CursoController(CursoService service) { this.service = service; }

    // 1) Obtener cursos del docente (usa correo del Principal si está disponible)
    @GetMapping("/docente/cursos")
    public ResponseEntity<List<CursoGrupoDTO>> obtenerCursos(Principal principal, @RequestParam(value="correo", required=false) String correoParam) {
        String correo = correoParam;
        if (correo == null && principal != null) {
            correo = principal.getName(); // ajustar según tu UserDetails (si el name es el correo)
        }
        if (correo == null) return ResponseEntity.badRequest().build();
        List<CursoGrupoDTO> cursos = service.getCursosPorDocente(correo);
        return ResponseEntity.ok(cursos);
    }

    // 2) Obtener sílabos por curso (para saber si existe al menos uno)
    @GetMapping("/cursos/{codigo}/silabos")
    public ResponseEntity<?> obtenerSilabos(@PathVariable String codigo) {
        List<SilaboDTO> silabos = service.getSilabosPorCurso(codigo);
        if (silabos == null || silabos.isEmpty()) {
            Map<String,String> resp = Map.of("advertencia", "No existe ningún sílabo registrado para el curso " + codigo);
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.ok(silabos);
    }

    @PostMapping("/cursos/{codigo}/silabo")
    public ResponseEntity<?> subirSilabo(
            @PathVariable String codigo,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "idDocente", required = false) Integer idDocente) {

        Map<String,Object> resp = new HashMap<>();
        if (file == null || file.isEmpty()) {
            resp.put("message", "Archivo vacío");
            return ResponseEntity.badRequest().body(resp);
        }

        String original = Paths.get(file.getOriginalFilename()).getFileName().toString();
        if (!original.toLowerCase().endsWith(".pdf")) {
            resp.put("message", "Solo se aceptan archivos PDF");
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(resp);
        }

        if (file.getSize() > 30L * 1024 * 1024) { // límite configurable
            resp.put("message", "Archivo demasiado grande");
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(resp);
        }

        try {
            // Normalizar y asegurar path absoluto
            Path base = Paths.get(silabosBaseDir);
            if (!base.isAbsolute()) {
                // si el path no es absoluto, lo convertimos a temp dir + path para evitar resoluciones relativas a webapp
                base = Paths.get(System.getProperty("java.io.tmpdir")).resolve(base).toAbsolutePath().normalize();
            } else {
                base = base.toAbsolutePath().normalize();
            }

            // Crear directorios si no existen
            Files.createDirectories(base);

            String filename = codigo + "_" + System.currentTimeMillis() + ".pdf";
            Path target = base.resolve(filename);

            // Usar stream copy (más controlable)
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // Registrar en BD (ajusta service.registrarSilabo)
            int rows = service.registrarSilabo(codigo, idDocente, target.toString());

            resp.put("message", "Sílabo subido correctamente");
            resp.put("ruta", target.toString());
            resp.put("rows", rows);
            return ResponseEntity.ok(resp);

        } catch (IOException ex) {
            ex.printStackTrace(); // registra en log del servidor
            resp.put("message", "Error guardando archivo: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.put("message", "Error en servidor: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

 // Obtener sílabo + contenido (unidades y temas) para un curso y usuario (idUsuario como param)
    @GetMapping("/cursos/{codigo}/contenido")
    public ResponseEntity<?> obtenerContenidoPorCurso(
            @PathVariable("codigo") String codigoCurso,
            @RequestParam(value = "idUsuario", required = false) Integer idUsuario) {

        if (idUsuario == null) {
            // intenta sacar de sesión / principal si lo tienes
            return ResponseEntity.badRequest().body(Map.of("message", "Falta idUsuario"));
        }
        ContenidoDTO contenido = service.getSilaboYContenido(codigoCurso, idUsuario);
        //SilaboDTO silabo = service.getSilaboYContenido(codigoCurso, idUsuario);
        List<UnidadDTO> unidades = service.getUnidadesPorCursoYUsuario(codigoCurso, idUsuario);
        if (unidades == null) {
            return ResponseEntity.ok(new ContenidoDTO(null, Collections.emptyList()));
            //return ResponseEntity.ok(Map.of("silabo", null, "unidades", unidades)); // unidades vacías
        }
        return ResponseEntity.ok(contenido);
        //return ResponseEntity.ok(Map.of("silabo", silabo, "unidades", unidades));
    }

    // Guardar contenido (unidades y temas) — cuerpo JSON con { units: [...], idDocente?, idCiclo?, grupoTeoria?, rutaArchivo? }
    @PostMapping("/cursos/{codigo}/contenido")
    public ResponseEntity<?> guardarContenido(
            @PathVariable("codigo") String codigoCurso,
            @RequestBody Map<String, Object> payload) {

        try {
            Integer idUsuario = payload.get("idDocente") instanceof Number ? ((Number)payload.get("idDocente")).intValue() : null;
            // Nota: "idDocente" en tu frontend contiene idUsuario; ajustar si es distinto
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> unitsRaw = (List<Map<String,Object>>) payload.get("units");
            List<UnidadDTO> unidades = new ArrayList<>();
            int unitNum = 1;
            if (unitsRaw != null) {
                for (Map<String,Object> u : unitsRaw) {
                    UnidadDTO ud = new UnidadDTO();
                    ud.setNumeroUnidad(u.get("numero") instanceof Number ? ((Number)u.get("numero")).intValue() : unitNum++);
                    ud.setNombreUnidad((String) u.getOrDefault("name", u.get("nombre")));
                    @SuppressWarnings("unchecked")
                    List<Map<String,Object>> temasRaw = (List<Map<String,Object>>) u.get("themes");
                    List<TemaDTO> temas = new ArrayList<>();
                    int temaNum = 1;
                    if (temasRaw != null) {
                        for (Map<String,Object> t : temasRaw) {
                            TemaDTO td = new TemaDTO();
                            td.setNumeroTema(t.get("numero") instanceof Number ? ((Number)t.get("numero")).intValue() : temaNum++);
                            td.setNombreTema((String) t.getOrDefault("name", t.get("nombre")));
                            temas.add(td);
                        }
                    }
                    ud.setTemas(temas);
                    unidades.add(ud);
                }
            }
            // Parámetros opcionales:
            Integer idCiclo = payload.get("idCiclo") instanceof Number ? ((Number)payload.get("idCiclo")).intValue() : null;
            String grupoTeoria = payload.get("grupoTeoria") != null ? payload.get("grupoTeoria").toString() : null;
            String rutaArchivo = payload.get("rutaArchivo") != null ? payload.get("rutaArchivo").toString() : null;
            Integer idDocente = idUsuario; // Usar idUsuario como idDocente ya que no se envía idDocenteReal

            int res = service.guardarContenido(codigoCurso, idUsuario, idCiclo, grupoTeoria, idDocente, rutaArchivo, unidades);
            return ResponseEntity.ok(Map.of("ok", true, "res", res));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Error guardando contenido", "error", ex.getMessage()));
        }
    }



}

