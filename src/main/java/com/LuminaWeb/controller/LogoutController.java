package com.LuminaWeb.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para cerrar sesión de usuario en LuminaWeb.
 * Endpoint: POST /api/logout
 */
@RestController
@RequestMapping("/api")
public class LogoutController {

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(new Mensaje("Sesión cerrada correctamente"));
    }

    // Clase interna para retornar un JSON simple
    private static class Mensaje {
        public boolean ok = true;
        public String mensaje;

        public Mensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }
}
