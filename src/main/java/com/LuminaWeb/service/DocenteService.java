package com.LuminaWeb.service;

import com.LuminaWeb.dao.DocenteDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DocenteService {

    @Autowired
    private DocenteDAO docenteDAO;

    public List<Map<String, Object>> listarDocentes() {
        return docenteDAO.listarDocentes();
    }
}
