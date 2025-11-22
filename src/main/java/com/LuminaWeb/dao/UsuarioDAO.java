package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.mindrot.jbcrypt.BCrypt;

/**
 * UsuarioDAO adaptado a Spring (JdbcTemplate).
 * Reemplaza el uso de DBUtil por inyección de DataSource / JdbcTemplate.
 */
@Repository
public class UsuarioDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UsuarioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class ResultadoLogin {
        public boolean ok;
        public String rol;
        public String mensaje;
    }

    public ResultadoLogin verificarCredenciales(String correo, String passwordPlano) {
        ResultadoLogin res = new ResultadoLogin();

        String sql = "SELECT u.password_hash, u.estado_cuenta, t.nombre_tipo "
                   + "FROM usuarios u "
                   + "INNER JOIN tipos_usuario t ON u.tipo_id = t.tipo_id "
                   + "WHERE u.correo_institucional = ?";

        try {
            // queryForMap lanza EmptyResultDataAccessException si no hay fila
            java.util.Map<String, Object> row = jdbcTemplate.queryForMap(sql, correo);

            String hashDB = row.get("password_hash") == null ? null : row.get("password_hash").toString();
            String estado = row.get("estado_cuenta") == null ? null : row.get("estado_cuenta").toString();
            String rol = row.get("nombre_tipo") == null ? null : row.get("nombre_tipo").toString();

            if (estado == null || !"ACTIVO".equalsIgnoreCase(estado)) {
                res.ok = false;
                res.mensaje = "Cuenta " + (estado == null ? "inactiva" : estado.toLowerCase());
                return res;
            }

            // === COMPROBACIÓN ADICIONAL PARA PRUEBAS ===
            // Si el usuario escribe EXACTAMENTE el valor que está en password_hash,
            // lo aceptamos (solo para testing). Muy inseguro: eliminar en producción.
            if (passwordPlano != null && hashDB != null && passwordPlano.equals(hashDB)) {
                res.ok = true;
                res.rol = rol;
                return res;
            }
            // =================================================

            // Verificación estándar con bcrypt
            boolean pasa = false;
            if (hashDB != null && passwordPlano != null) {
                try {
                    pasa = BCrypt.checkpw(passwordPlano, hashDB);
                } catch (Exception e) {
                    pasa = false;
                }
            }

            if (pasa) {
                res.ok = true;
                res.rol = rol;
            } else {
                res.ok = false;
                res.mensaje = "Contraseña incorrecta";
            }

        } catch (EmptyResultDataAccessException ex) {
            res.ok = false;
            res.mensaje = "Usuario no encontrado";
        } catch (DataAccessException dae) {
            res.ok = false;
            res.mensaje = "Error en base de datos: " + dae.getMessage();
        } catch (Exception e) {
            res.ok = false;
            res.mensaje = "Error interno: " + e.getMessage();
        }

        return res;
    }
}
