package com.LuminaWeb.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DocenteDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Devuelve todos los docentes activos para los selects
    public List<Map<String, Object>> listarDocentes() {
        String sql = "SELECT id_docente, apellidos_nombres FROM docentes ORDER BY apellidos_nombres";
        return jdbcTemplate.queryForList(sql);
    }
}
