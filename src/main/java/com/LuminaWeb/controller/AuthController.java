package com.LuminaWeb.controller;

import com.LuminaWeb.dao.UsuarioDAO;
import com.LuminaWeb.dao.EstudianteDAO;
import com.LuminaWeb.dao.UsuarioDAO.ResultadoLogin;
import com.LuminaWeb.dao.EstudianteDAO.Estudiante;
import com.LuminaWeb.dao.EstudianteDAO.Horario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

/**
 * AuthController actualizado para usar el estilo Spring:
 * - Devuelve objetos (Spring los convierte a JSON)
 * - Maneja DataAccessException en lugar de SQLException
 * - Inyección por constructor/autowired
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioDAO usuarioDAO;
    private final EstudianteDAO estudianteDAO;

    @Autowired
    public AuthController(UsuarioDAO usuarioDAO, EstudianteDAO estudianteDAO) {
        this.usuarioDAO = usuarioDAO;
        this.estudianteDAO = estudianteDAO;
    }

    // ===== Clases internas =====
    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class LoginResponse {
        public boolean success;
        public String rol;
        public String mensaje;
        public Estudiante estudiante; // puede ser null
        public List<Horario> horarios; // puede ser null o empty

        public LoginResponse() {}

        public LoginResponse(boolean s, String r, String m) {
            this.success = s;
            this.rol = r;
            this.mensaje = m;
        }
    }

    // ===== Endpoint principal =====
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginReq, HttpSession session) {
        try {
            // Si tu UsuarioDAO.verificarCredenciales ya no lanza SQLException, esto funciona bien.
            ResultadoLogin resultado = usuarioDAO.verificarCredenciales(loginReq.email, loginReq.password);

            System.out.println("Login attempt: " + loginReq.email);

            if (resultado != null && resultado.ok) {
                session.setAttribute("usuario", loginReq.email);
                session.setAttribute("rol", resultado.rol);

                LoginResponse lr = new LoginResponse(true, resultado.rol, "Login exitoso");

                String rolLower = (resultado.rol == null) ? "" : resultado.rol.toLowerCase();
                if (rolLower.contains("estudiante") || rolLower.equals("estudiante")) {
                    Estudiante e = estudianteDAO.obtenerPorCorreo(loginReq.email);
                    lr.estudiante = e;
                    if (e != null) {
                        List<Horario> horarios = estudianteDAO.obtenerHorariosPorIdUsuario(e.idUsuario);
                        lr.horarios = horarios;
                        session.setAttribute("estudiante", e);
                        session.setAttribute("horarios", horarios);
                    } else {
                        lr.horarios = Collections.emptyList();
                    }
                } else {
                    lr.estudiante = null;
                    lr.horarios = Collections.emptyList();
                }
                return ResponseEntity.ok(lr);

            } else {
                LoginResponse lr = new LoginResponse(false, null,
                        (resultado == null ? "Credenciales inválidas" : resultado.mensaje));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(lr);
            }

        } catch (DataAccessException dae) {
            // Errores de acceso a BD (JdbcTemplate)
            dae.printStackTrace();
            LoginResponse lr = new LoginResponse(false, null, "Error interno de base de datos");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(lr);
        } catch (Exception ex) {
            // Cualquier otro error inesperado
            ex.printStackTrace();
            LoginResponse lr = new LoginResponse(false, null, "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(lr);
        }
    }
}
