package com.LuminaWeb.controller;

import com.LuminaWeb.dto.EstudiantePerfilDTO;
import com.LuminaWeb.service.EstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@RequestMapping("/api/estudiante")
public class EstudianteApiController {

    @Autowired
    private EstudianteService estudianteService;

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> obtenerPerfil(@PathVariable int idUsuario) {
        try {
            EstudiantePerfilDTO dto = estudianteService.obtenerPerfilPorIdUsuario(idUsuario);
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
