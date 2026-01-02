package com.LuminaWeb.dto;

public class ProfesorPerfilDTO {
    private Integer idUsuario;
    private String email;
    private String nombreCompleto;
    private String departamento;
    private String fechaCreacion;
    private Integer cursosActivos;
    private String iniciales;

    // Getters y Setters
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    

    
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public Integer getCursosActivos() { return cursosActivos; }
    public void setCursosActivos(Integer cursosActivos) { this.cursosActivos = cursosActivos; }
    
    public String getIniciales() { return iniciales; }
    public void setIniciales(String iniciales) { this.iniciales = iniciales; }
}