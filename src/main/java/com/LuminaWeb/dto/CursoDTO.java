package com.LuminaWeb.dto;

public class CursoDTO {
    private String codigoCurso;
    private String nombreCurso;
    private Integer grupoId;
    private String letraGrupo;
    private String tipoClase;

    // Constructores
    public CursoDTO() {}
    public CursoDTO(String codigo, String nombre, Integer grupoId, String letra, String tipoClase) {
        this.codigoCurso = codigo;
        this.nombreCurso = nombre;
        this.grupoId = grupoId;
        this.letraGrupo = letra;
        this.tipoClase = tipoClase;
    }

    // getters / setters
    public String getCodigoCurso() { return codigoCurso; }
    public void setCodigoCurso(String codigoCurso) { this.codigoCurso = codigoCurso; }
    public String getNombreCurso() { return nombreCurso; }
    public void setNombreCurso(String nombreCurso) { this.nombreCurso = nombreCurso; }
    public Integer getGrupoId() { return grupoId; }
    public void setGrupoId(Integer grupoId) { this.grupoId = grupoId; }
    public String getLetraGrupo() { return letraGrupo; }
    public void setLetraGrupo(String letraGrupo) { this.letraGrupo = letraGrupo; }
    public String getTipoClase() { return tipoClase; }
    public void setTipoClase(String tipoClase) { this.tipoClase = tipoClase; }
}
