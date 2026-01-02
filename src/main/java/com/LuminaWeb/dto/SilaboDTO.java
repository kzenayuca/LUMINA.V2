package com.LuminaWeb.dto;

import java.sql.Timestamp;

public class SilaboDTO {

    private Integer idSilabo;
    private String codigoCurso;
    private Integer idCiclo;
    private String grupoTeoria;
    private String rutaArchivo;
    private Integer idDocente;
    private Timestamp fechaSubida;
    private String estado;
    private String advertencia; // si el procedure devuelve advertencia

    // Constructor vacío
    public SilaboDTO() {
    }

    // Constructor completo (opcional)
    public SilaboDTO(Integer idSilabo, String codigoCurso, Integer idCiclo, String grupoTeoria,
                     String rutaArchivo, Integer idDocente, Timestamp fechaSubida, String estado) {
        this.idSilabo = idSilabo;
        this.codigoCurso = codigoCurso;
        this.idCiclo = idCiclo;
        this.grupoTeoria = grupoTeoria;
        this.rutaArchivo = rutaArchivo;
        this.idDocente = idDocente;
        this.fechaSubida = fechaSubida;
        this.estado = estado;
    }

    public Integer getIdSilabo() {
        return idSilabo;
    }

    public void setIdSilabo(Integer idSilabo) {
        this.idSilabo = idSilabo;
    }

    public String getCodigoCurso() {
        return codigoCurso;
    }

    public void setCodigoCurso(String codigoCurso) {
        this.codigoCurso = codigoCurso;
    }

    public Integer getIdCiclo() {
        return idCiclo;
    }

    public void setIdCiclo(Integer idCiclo) {
        this.idCiclo = idCiclo;
    }

    public String getGrupoTeoria() {
        return grupoTeoria;
    }

    public void setGrupoTeoria(String grupoTeoria) {
        this.grupoTeoria = grupoTeoria;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Integer getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(Integer idDocente) {
        this.idDocente = idDocente;
    }

    public Timestamp getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(Timestamp fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    public String getAdvertencia() { return advertencia; }
    public void setAdvertencia(String advertencia) { this.advertencia = advertencia; }

    @Override
    public String toString() {
        return "SilaboDTO{" +
                "idSilabo=" + idSilabo +
                ", codigoCurso='" + codigoCurso + '\'' +
                ", idCiclo=" + idCiclo +
                ", grupoTeoria='" + grupoTeoria + '\'' +
                ", rutaArchivo='" + rutaArchivo + '\'' +
                ", idDocente=" + idDocente +
                ", fechaSubida=" + fechaSubida +
                ", estado='" + estado + '\'' +
                '}';
    }
}
