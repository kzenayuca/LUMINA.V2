package com.LuminaWeb.dto;

public class EstudianteAsistenciaDTO {
    private Integer idMatricula;
    private String cui;
    private String numeroMatricula;
    private String nombre;
    private String estado_asistencia; // 'PRESENTE' o 'FALTA'
    private Integer idAsistencia; // si ya existe

    // getters / setters
    public Integer getIdMatricula() { return idMatricula; }
    public void setIdMatricula(Integer idMatricula) { this.idMatricula = idMatricula; }
    public String getCui() { return cui; }
    public void setCui(String cui) { this.cui = cui; }
    public String getNumeroMatricula() { return numeroMatricula; }
    public void setNumeroMatricula(String numeroMatricula) { this.numeroMatricula = numeroMatricula; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEstado_asistencia() { return estado_asistencia; }
    public void setEstado_asistencia(String estado_asistencia) { this.estado_asistencia = estado_asistencia; }
    public Integer getIdAsistencia() { return idAsistencia; }
    public void setIdAsistencia(Integer idAsistencia) { this.idAsistencia = idAsistencia; }
}
