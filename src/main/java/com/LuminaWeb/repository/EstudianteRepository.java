package com.LuminaWeb.repository;

import com.LuminaWeb.dto.EstudiantePerfilDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class EstudianteRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public EstudiantePerfilDTO obtenerDatosPorIdUsuario(int idUsuario) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("CALL sp_obtener_datos_estudiante(?)", idUsuario);
            EstudiantePerfilDTO dto = new EstudiantePerfilDTO();
            dto.setIdUsuario(((Number)row.get("idUsuario")).intValue());
            dto.setEmail((String) row.get("email"));
            dto.setCodigo((String) row.get("codigo"));
            dto.setNombreCompleto((String) row.get("nombreCompleto"));
            Object nm = row.get("numeroMatricula");
            if (nm != null) dto.setNumeroMatricula(((Number)nm).intValue());
            dto.setFechaCreacion(row.get("fechaCreacion") != null ? row.get("fechaCreacion").toString() : null);
            Object ca = row.get("cursosActivos");
            if (ca != null) dto.setCursosActivos(((Number)ca).intValue());
            dto.setPromedioGeneral(row.get("promedioGeneral") != null ? row.get("promedioGeneral").toString() : null);
            dto.setAsistencia(row.get("asistencia") != null ? row.get("asistencia").toString() : null);
            Object cp = row.get("creditosAprobados");
            if (cp != null) dto.setCreditosAprobados(((Number)cp).intValue());
            dto.setCarrera((String) row.get("carrera"));
            dto.setFacultad((String) row.get("facultad"));
            dto.setSemestre((String) row.get("semestre"));
            dto.setIniciales((String) row.get("iniciales"));
            return dto;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener datos del estudiante: " + ex.getMessage(), ex);
        }
    }
}
