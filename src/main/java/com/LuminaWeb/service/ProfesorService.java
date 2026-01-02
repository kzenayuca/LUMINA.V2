package com.LuminaWeb.service;

import com.LuminaWeb.dto.ProfesorPerfilDTO;
import com.LuminaWeb.repository.ProfesorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfesorService {

    @Autowired
    private ProfesorRepository repository;

    public ProfesorPerfilDTO obtenerPerfilPorIdUsuario(int idUsuario) {
        return repository.obtenerDatosPorIdUsuario(idUsuario);
    }
}