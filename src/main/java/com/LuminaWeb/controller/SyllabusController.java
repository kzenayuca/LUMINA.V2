package com.LuminaWeb.controller;

import com.LuminaWeb.service.SyllabusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SyllabusController {

    private final SyllabusService service;

    public SyllabusController(SyllabusService service) {
        this.service = service;
    }

    // 1) obtener cursos del profesor por correo
    @GetMapping("/profesor/cursos")
    public ResponseEntity<List<Map<String,String>>> obtenerCursos(@RequestParam String correo) {
        List<Map<String,String>> lista = service.getCursosPorProfesor(correo);
        return ResponseEntity.ok(lista);
    }

    // 2) comprobar existencia de silabo
    @GetMapping("/silabo/existe")
    public ResponseEntity<Map<String,Boolean>> existeSilabo(@RequestParam String codigo) {
        boolean existe = service.existeSilabo(codigo);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    // 3) subir silabo (pdf + excel)
    @PostMapping("/silabo/upload")
    public ResponseEntity<?> uploadSyllabus(
            @RequestParam String curso,
            @RequestParam(required=false) String correoProfesor,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam("excelFile") MultipartFile excelFile
    ) {
        try {
            service.procesarUploadSyllabus(curso, correoProfesor, pdfFile, excelFile);
            return ResponseEntity.ok(Map.of("success", true, "message", "Sílabo guardado e importado correctamente", "curso", curso));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}