package com.LuminaWeb.dto;

public class CursoGrupoDTO {

    private String codigoCurso;
    private String nombreCurso;
    private Integer grupoId;
    private String letraGrupo;
    private String tipoClase;

    // Constructor vacío
    public CursoGrupoDTO() {
    }

    // Constructor completo (opcional)
    public CursoGrupoDTO(String codigoCurso, String nombreCurso, Integer grupoId, String letraGrupo, String tipoClase) {
        this.codigoCurso = codigoCurso;
        this.nombreCurso = nombreCurso;
        this.grupoId = grupoId;
        this.letraGrupo = letraGrupo;
        this.tipoClase = tipoClase;
    }

    public String getCodigoCurso() {
        return codigoCurso;
    }

    public void setCodigoCurso(String codigoCurso) {
        this.codigoCurso = codigoCurso;
    }

    public String getNombreCurso() {
        return nombreCurso;
    }

    public void setNombreCurso(String nombreCurso) {
        this.nombreCurso = nombreCurso;
    }

    public Integer getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Integer grupoId) {
        this.grupoId = grupoId;
    }

    public String getLetraGrupo() {
        return letraGrupo;
    }

    public void setLetraGrupo(String letraGrupo) {
        this.letraGrupo = letraGrupo;
    }

    public String getTipoClase() {
        return tipoClase;
    }

    public void setTipoClase(String tipoClase) {
        this.tipoClase = tipoClase;
    }

    @Override
    public String toString() {
        return "CursoGrupoDTO{" +
                "codigoCurso='" + codigoCurso + '\'' +
                ", nombreCurso='" + nombreCurso + '\'' +
                ", grupoId=" + grupoId +
                ", letraGrupo='" + letraGrupo + '\'' +
                ", tipoClase='" + tipoClase + '\'' +
                '}';
    }
}
