package com.LuminaWeb.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;

@Repository
public class ContrasenaUserDAO {

    private final JdbcTemplate jdbc;

    public ContrasenaUserDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // devuelve la contraseña (texto plano, según tu pedido) para el usuario
    public String getPasswordById(int idUsuario) {
        try {
            String sql = "SELECT password_hash FROM usuarios WHERE id_usuario = ?"; // si tu columna se llama diferente, cámbiala
            return jdbc.queryForObject(sql, new Object[]{idUsuario}, String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // Opción A: actualizar directamente con SQL (inserta la nueva contraseña tal cual)
    public int updatePassword(int idUsuario, String newPassword) {
        String sql = "UPDATE usuarios SET password_hash = ? WHERE id_usuario = ?";
        return jdbc.update(sql, newPassword, idUsuario);
    }

    // Opción B: llamar al procedimiento almacenado que ya definiste
    public int callSpUpdatePassword(int idUsuario, String newPassword) {
        return jdbc.update("CALL sp_update_password(?, ?)", idUsuario, newPassword);
    }
}
