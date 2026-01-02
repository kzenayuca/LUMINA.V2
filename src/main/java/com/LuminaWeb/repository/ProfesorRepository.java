package com.LuminaWeb.repository;

import com.LuminaWeb.dto.ProfesorPerfilDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class ProfesorRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ProfesorPerfilDTO obtenerDatosPorIdUsuario(int idUsuario) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("CALL sp_obtener_datos_Profesor(?)", idUsuario);
            ProfesorPerfilDTO dto = new ProfesorPerfilDTO();
            dto.setIdUsuario(((Number)row.get("idUsuario")).intValue());
            dto.setEmail((String) row.get("email"));
            dto.setNombreCompleto((String) row.get("nombreCompleto"));
            
            dto.setFechaCreacion(row.get("fechaCreacion") != null ? row.get("fechaCreacion").toString() : null);
            Object ca = row.get("cursosActivos");
            if (ca != null) dto.setCursosActivos(((Number)ca).intValue());

            
            dto.setDepartamento((String) row.get("departamento"));

            dto.setIniciales((String) row.get("iniciales"));
            return dto;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener datos del Profesor: " + ex.getMessage(), ex);
        }
    }
}
