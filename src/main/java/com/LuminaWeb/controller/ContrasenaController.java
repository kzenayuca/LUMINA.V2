package com.LuminaWeb.controller;

import com.LuminaWeb.dto.ChangePasswordRequest;
import com.LuminaWeb.service.ContrasenaUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudiante")
public class ContrasenaController {

    private final ContrasenaUserService estudianteService;

    public ContrasenaController(ContrasenaUserService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @PostMapping("/{id}/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(@PathVariable("id") int id,
                                              @RequestBody ChangePasswordRequest req) {
        try {
            estudianteService.cambiarContrasena(id, req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok().body("{\"message\":\"Contraseña cambiada\"}");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            // log.error("Error al cambiar contraseña", e);
            return ResponseEntity.status(500).body("{\"error\":\"Error interno al cambiar contraseña\"}");
        }
    }
}
