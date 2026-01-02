package com.LuminaWeb.dto;

import java.util.List;

public class GuardarAsistenciaRequest {
    private Integer idHorario;
    private String fecha; // yyyy-MM-dd
    private String registradoPorCorreo;
    private List<EstudianteAsistenciaDTO> actualizaciones;

    public Integer getIdHorario() { return idHorario; }
    public void setIdHorario(Integer idHorario) { this.idHorario = idHorario; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getRegistradoPorCorreo() { return registradoPorCorreo; }
    public void setRegistradoPorCorreo(String registradoPorCorreo) { this.registradoPorCorreo = registradoPorCorreo; }
    public List<EstudianteAsistenciaDTO> getActualizaciones() { return actualizaciones; }
    public void setActualizaciones(List<EstudianteAsistenciaDTO> actualizaciones) { this.actualizaciones = actualizaciones; }
}
