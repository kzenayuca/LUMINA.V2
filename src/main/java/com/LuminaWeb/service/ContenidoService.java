package com.LuminaWeb.service;

import org.springframework.stereotype.Service;
import com.LuminaWeb.dao.ContenidoDAO;

@Service
public class ContenidoService {
    private final ContenidoDAO dao;
    public ContenidoService(ContenidoDAO dao) { this.dao = dao; }

    public String getContenidoPorCui(String cui) {
        return dao.obtenerContenidoPorCui(cui);
    }
}
