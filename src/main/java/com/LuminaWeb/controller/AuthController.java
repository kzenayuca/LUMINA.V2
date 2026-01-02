package com.LuminaWeb.controller;

import com.LuminaWeb.dao.UsuarioDAO;
import com.LuminaWeb.dao.EstudianteDAO;
import com.LuminaWeb.dao.UsuarioDAO.ResultadoLogin;
import com.LuminaWeb.dao.EstudianteDAO.Estudiante;
import com.LuminaWeb.dao.ProfesorDAO;
import com.LuminaWeb.dao.ProfesorDAO.Profesor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioDAO usuarioDAO;
    private final EstudianteDAO estudianteDAO;
    private final ProfesorDAO profesorDAO;

    @Autowired
    public AuthController(UsuarioDAO usuarioDAO, EstudianteDAO estudianteDAO, ProfesorDAO profesorDAO) {
        this.usuarioDAO = usuarioDAO;
        this.estudianteDAO = estudianteDAO;
        this.profesorDAO = profesorDAO;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class LoginResponse {
        public boolean success;
        public String rol;
        public String mensaje;
        public Estudiante estudiante; // puede ser null
        public Profesor profesor; // puede ser null
        public List<com.LuminaWeb.dao.EstudianteDAO.Horario> horariosEstudiante; // puede ser null o empty
        public List<com.LuminaWeb.dao.ProfesorDAO.Horario> horariosProfesor; // puede ser null o empty

        public LoginResponse() {}

        public LoginResponse(boolean s, String r, String m) {
            this.success = s;
            this.rol = r;
            this.mensaje = m;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginReq, HttpSession session) {
        try {
            // Verificar las credenciales
            ResultadoLogin resultado = usuarioDAO.verificarCredenciales(loginReq.email, loginReq.password);

            System.out.println("Login attempt: " + loginReq.email);

            if (resultado != null && resultado.ok) {
                session.setAttribute("usuario", loginReq.email);
                session.setAttribute("rol", resultado.rol);
                // Compatibilidad: otros controladores esperan "tipo_usuario" en sesión
                session.setAttribute("tipo_usuario", resultado.rol);

                LoginResponse lr = new LoginResponse(true, resultado.rol, "Login exitoso");

                String rolLower = (resultado.rol == null) ? "" : resultado.rol.toLowerCase();
                
                // Manejo del rol "estudiante"
                if (rolLower.contains("estudiante") || rolLower.equals("estudiante")) {
                    Estudiante e = estudianteDAO.obtenerPorCorreo(loginReq.email);
                    lr.estudiante = e;
                    if (e != null) {
                        List<com.LuminaWeb.dao.EstudianteDAO.Horario> horarios = estudianteDAO.obtenerHorariosPorIdUsuario(e.idUsuario);
                        lr.horariosEstudiante = horarios;
                        session.setAttribute("estudiante", e);
                        session.setAttribute("horariosEstudiante", horarios);
                    } else {
                        lr.horariosEstudiante = Collections.emptyList();
                    }
                } 
                // Manejo del rol "docente"
                else if (rolLower.contains("docente") || rolLower.equals("docente")) {
                    Profesor p = profesorDAO.obtenerPorCorreo(loginReq.email);
                    lr.profesor = p;
                    if (p != null) {
                        List<com.LuminaWeb.dao.ProfesorDAO.Horario> horarios = profesorDAO.obtenerHorariosPorIdUsuario(p.idUsuario);
                        lr.horariosProfesor = horarios;
                        session.setAttribute("profesor", p);
                        session.setAttribute("horariosProfesor", horarios);
                    } else {
                        lr.horariosProfesor = Collections.emptyList();
                    }
                } 
                // Si no es ni estudiante ni docente
                else {
                    lr.estudiante = null;
                    lr.profesor = null;
                    lr.horariosEstudiante = Collections.emptyList();
                    lr.horariosProfesor = Collections.emptyList();
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
