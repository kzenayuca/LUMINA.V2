package com.LuminaWeb.dao;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ContenidoDAO {

    private final JdbcClient jdbc;

    public ContenidoDAO(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public String obtenerContenidoPorCui(String cui) {

        String sql = "CALL sp_obtener_contenido_estudiante(:cui)";

        try {
            return jdbc
                    .sql(sql)
                    .param("cui", cui)
                    .query(String.class)
                    .single();       // devuelve UN solo valor (no-deprecated)
        } catch (Exception e) {
            System.err.println("Error DAO: " + e.getMessage());
            return "[]";
        }
    }
}
