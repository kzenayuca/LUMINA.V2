package com.LuminaWeb.dto;

import java.util.ArrayList;
import java.util.List;

public class ContenidoDTO {
    private SilaboDTO silabo;
    private List<UnidadDTO> unidades = new ArrayList<>();

    public ContenidoDTO() {}

    public ContenidoDTO(SilaboDTO silabo, List<UnidadDTO> unidades) {
        this.silabo = silabo;
        this.unidades = unidades != null ? unidades : new ArrayList<>();
    }

    public SilaboDTO getSilabo() { return silabo; }
    public void setSilabo(SilaboDTO silabo) { this.silabo = silabo; }

    public List<UnidadDTO> getUnidades() { return unidades; }
    public void setUnidades(List<UnidadDTO> unidades) { this.unidades = unidades != null ? unidades : new ArrayList<>(); }
}
