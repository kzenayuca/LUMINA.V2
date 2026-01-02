package com.LuminaWeb.service;

import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

@Service
public class SecVerCursosService {
    
    public List<Map<String, Object>> listarCursos() throws SQLException {
        List<Map<String, Object>> cursos = new ArrayList<>();
        
        String sql = "SELECT c.codigo_curso, c.nombre_curso, c.tiene_laboratorio, c.estado " +
                    "FROM cursos c " +
                    "WHERE c.estado = 'ACTIVO' " +
                    "ORDER BY c.codigo_curso";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> curso = new HashMap<>();
                curso.put("codigo_curso", rs.getString("codigo_curso"));
                curso.put("nombre_curso", rs.getString("nombre_curso"));
                curso.put("tiene_laboratorio", rs.getInt("tiene_laboratorio"));
                curso.put("estado", rs.getString("estado"));
                
                cursos.add(curso);
            }
        }
        
        return cursos;
    }
    
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/lumina_bd?useSSL=false&serverTimezone=UTC";
            String user = "root";
            String password = "Piudz2012";
            
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado", e);
        }
    }
}