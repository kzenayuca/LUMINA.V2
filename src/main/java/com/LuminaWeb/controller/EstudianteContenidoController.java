package com.LuminaWeb.controller;

import com.LuminaWeb.service.ContenidoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudiante")
public class EstudianteContenidoController {

    private final ContenidoService contenidoService;

    public EstudianteContenidoController(ContenidoService contenidoService) {
        this.contenidoService = contenidoService;
    }

    /**
     * GET /api/estudiante/{cui}/contenido
     * Devuelve JSON con estructura: [ { id, nombre, descripcion, progreso, semanas: [...] }, ... ]
     */
    @GetMapping(value = "/{cui}/contenido", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> obtenerContenido(@PathVariable("cui") String cui) {
        String json = contenidoService.getContenidoPorCui(cui);
        if (json == null || json.trim().isEmpty()) json = "[]";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }
}
