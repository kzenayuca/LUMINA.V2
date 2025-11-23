package com.LuminaWeb.service;

import com.LuminaWeb.dao.ContrasenaUserDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContrasenaUserService {

    private final ContrasenaUserDAO usuarioDAO;

    public ContrasenaUserService(ContrasenaUserDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Cambia la contraseña comparando y guardando en texto plano (según solicitud).
     * Lanzará IllegalArgumentException con mensaje claro si falla alguna validación.
     */
    @Transactional
    public void cambiarContrasena(int idUsuario, String currentPassword, String newPassword) {
        // obtener contraseña actual (texto plano)
        String stored = usuarioDAO.getPasswordById(idUsuario);
        if (stored == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        // comparar texto plano (asegúrate de que tu DB almacena en el mismo formato)
        if (!stored.equals(currentPassword)) {
            throw new IllegalArgumentException("Contraseña actual incorrecta");
        }

        // validaciones de nueva contraseña
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        // actualizar: elige updatePassword o callSpUpdatePassword
        int rows = usuarioDAO.callSpUpdatePassword(idUsuario, newPassword);
        // int rows = usuarioDAO.updatePassword(idUsuario, newPassword);

        if (rows == 0) {
            throw new RuntimeException("No se pudo actualizar la contraseña");
        }
    }
}
