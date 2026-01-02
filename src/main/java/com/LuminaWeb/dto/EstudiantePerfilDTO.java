package com.LuminaWeb.dto;

public class EstudiantePerfilDTO {
    private Integer idUsuario;
    private String email;
    private String codigo; // CUI
    private String nombreCompleto;
    private Integer numeroMatricula;
    private String fechaCreacion;
    private Integer cursosActivos;
    private String promedioGeneral; // puede ser null
    private String asistencia;      // puede ser null
    private Integer creditosAprobados; // puede ser null
    private String carrera;
    private String facultad;
    private String semestre;
    private String iniciales;

    // getters y setters
    // (usa tu IDE para generarlos)
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public Integer getNumeroMatricula() { return numeroMatricula; }
    public void setNumeroMatricula(Integer numeroMatricula) { this.numeroMatricula = numeroMatricula; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Integer getCursosActivos() { return cursosActivos; }
    public void setCursosActivos(Integer cursosActivos) { this.cursosActivos = cursosActivos; }
    public String getPromedioGeneral() { return promedioGeneral; }
    public void setPromedioGeneral(String promedioGeneral) { this.promedioGeneral = promedioGeneral; }
    public String getAsistencia() { return asistencia; }
    public void setAsistencia(String asistencia) { this.asistencia = asistencia; }
    public Integer getCreditosAprobados() { return creditosAprobados; }
    public void setCreditosAprobados(Integer creditosAprobados) { this.creditosAprobados = creditosAprobados; }
    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }
    public String getFacultad() { return facultad; }
    public void setFacultad(String facultad) { this.facultad = facultad; }
    public String getSemestre() { return semestre; }
    public void setSemestre(String semestre) { this.semestre = semestre; }
    public String getIniciales() { return iniciales; }
    public void setIniciales(String iniciales) { this.iniciales = iniciales; }
}
