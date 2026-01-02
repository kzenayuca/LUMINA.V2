package com.LuminaWeb.dto;

public class TemaDTO {

    private Integer idTema;             // id_tema en BD
    private Integer numeroTema;         // numero_tema
    private String nombreTema;
    private Integer duracionEstimada;   // minutos
    private String estado;              // "ACTIVO" / "INACTIVO" según tu BD

    public TemaDTO() {
    }

    public TemaDTO(Integer idTema, Integer numeroTema, String nombreTema, Integer duracionEstimada, String estado) {
        this.idTema = idTema;
        this.numeroTema = numeroTema;
        this.nombreTema = nombreTema;
        this.duracionEstimada = duracionEstimada;
        this.estado = estado;
    }

    public Integer getIdTema() {
        return idTema;
    }
    public void setIdTema(Integer idTema) {
        this.idTema = idTema;
    }

    public Integer getNumeroTema() {
        return numeroTema;
    }
    public void setNumeroTema(Integer numeroTema) {
        this.numeroTema = numeroTema;
    }

    public String getNombreTema() {
        return nombreTema;
    }
    public void setNombreTema(String nombreTema) {
        this.nombreTema = nombreTema;
    }

    public Integer getDuracionEstimada() {
        return duracionEstimada;
    }
    public void setDuracionEstimada(Integer duracionEstimada) {
        this.duracionEstimada = duracionEstimada;
    }

    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "TemaDTO{" +
                "idTema=" + idTema +
                ", numeroTema=" + numeroTema +
                ", nombreTema='" + nombreTema + '\'' +
                ", duracionEstimada=" + duracionEstimada +
                ", estado='" + estado + '\'' +
                '}';
    }
}
