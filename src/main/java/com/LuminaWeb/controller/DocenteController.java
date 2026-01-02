package com.LuminaWeb.controller;

import com.LuminaWeb.service.DocenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DocenteController {

    @Autowired
    private DocenteService docenteService;

    @GetMapping("/api/docentes")
    public List<Map<String,Object>> listarDocentes() {
        return docenteService.listarDocentes();
    }
}
