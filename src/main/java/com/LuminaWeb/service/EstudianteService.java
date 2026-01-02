package com.LuminaWeb.service;

import com.LuminaWeb.dto.EstudiantePerfilDTO;
import com.LuminaWeb.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EstudianteService {

    @Autowired
    private EstudianteRepository repository;

    public EstudiantePerfilDTO obtenerPerfilPorIdUsuario(int idUsuario) {
        return repository.obtenerDatosPorIdUsuario(idUsuario);
    }
}
