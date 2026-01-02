package com.LuminaWeb.dto;

import java.util.ArrayList;
import java.util.List;

public class UnidadDTO {

    private Integer unidadId;       // unidad_id en BD (puede ser null si viene del frontend)
    private Integer numeroUnidad;   // numero_unidad
    private String nombreUnidad;
    private List<TemaDTO> temas = new ArrayList<>(); // nunca debe ser null

    public UnidadDTO() {
        // Inicializado arriba
    }

    public UnidadDTO(Integer unidadId, Integer numeroUnidad, String nombreUnidad, List<TemaDTO> temas) {
        this.unidadId = unidadId;
        this.numeroUnidad = numeroUnidad;
        this.nombreUnidad = nombreUnidad;
        this.temas = (temas != null) ? temas : new ArrayList<>();
    }

    public Integer getUnidadId() {
        return unidadId;
    }
    public void setUnidadId(Integer unidadId) {
        this.unidadId = unidadId;
    }

    public Integer getNumeroUnidad() {
        return numeroUnidad;
    }
    public void setNumeroUnidad(Integer numeroUnidad) {
        this.numeroUnidad = numeroUnidad;
    }

    public String getNombreUnidad() {
        return nombreUnidad;
    }
    public void setNombreUnidad(String nombreUnidad) {
        this.nombreUnidad = nombreUnidad;
    }

    public List<TemaDTO> getTemas() {
        return temas;
    }
    public void setTemas(List<TemaDTO> temas) {
        this.temas = (temas != null) ? temas : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "UnidadDTO{" +
                "unidadId=" + unidadId +
                ", numeroUnidad=" + numeroUnidad +
                ", nombreUnidad='" + nombreUnidad + '\'' +
                ", temas=" + temas +
                '}';
    }
}
