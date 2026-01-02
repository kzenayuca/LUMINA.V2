package com.LuminaWeb.service;

// CursoService.java
import com.LuminaWeb.dao.CursoDAO;
import com.LuminaWeb.dto.ContenidoDTO;
import com.LuminaWeb.dto.CursoGrupoDTO;
import com.LuminaWeb.dto.SilaboDTO;
import com.LuminaWeb.dto.UnidadDTO;
import java.util.*;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CursoService {
    private final CursoDAO dao;
    public CursoService(CursoDAO dao) { this.dao = dao; }

    public List<CursoGrupoDTO> getCursosPorDocente(String correo) {
        return dao.obtenerCursosPorDocente(correo);
    }

    public List<SilaboDTO> getSilabosPorCurso(String codigo) {
        return dao.obtenerSilabosPorCurso(codigo);
    }

    public int saveContenido(String codigoCurso, Integer idDocente, String jsonContenido) {
        return dao.guardarContenido(codigoCurso, idDocente, jsonContenido);
    }

    public int registrarSilabo(String codigoCurso, Integer idDocente, String rutaArchivo) {
        return dao.registrarSilabo(codigoCurso, idDocente, rutaArchivo);
    }


    //PARA LO DE CONTENIDO
public ContenidoDTO getSilaboYContenido(String codigoCurso, Integer idUsuario) {
    SilaboDTO silabo = dao.obtenerSilaboPorCursoYUsuario(codigoCurso, idUsuario);
    if (silabo == null) {
        return new ContenidoDTO(null, java.util.Collections.emptyList());
    }
    List<UnidadDTO> unidades = dao.obtenerUnidadesConTemasPorSilabo(silabo.getIdSilabo());
    return new ContenidoDTO(silabo, unidades);
}


    public List<UnidadDTO> getUnidadesPorCursoYUsuario(String codigoCurso, Integer idUsuario) {
        SilaboDTO silabo = dao.obtenerSilaboPorCursoYUsuario(codigoCurso, idUsuario);
        if (silabo == null) return Collections.emptyList();
        return dao.obtenerUnidadesConTemasPorSilabo(silabo.getIdSilabo());
    }

    public int guardarContenido(String codigoCurso, Integer idUsuario, Integer idCiclo, String grupoTeoria, Integer idDocente, String rutaArchivo, List<UnidadDTO> unidades) {
        return dao.guardarSilaboYContenido(codigoCurso, idUsuario, idCiclo, grupoTeoria, idDocente, rutaArchivo, unidades);
    }
}
