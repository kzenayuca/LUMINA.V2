package com.LuminaWeb.controller;

import com.LuminaWeb.dto.*;
import com.LuminaWeb.service.AsistenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencia")
public class AsistenciaController {

    private final AsistenciaService service;

    public AsistenciaController(AsistenciaService service) { this.service = service; }

    // POST /api/asistencia/cursos  { correo: "djaraa@unsa.edu.pe" }
    @PostMapping("/cursos")
    public ResponseEntity<List<CursoGrupoDTO>> obtenerCursos(@RequestBody Map<String,String> body) {
        String correo = body.get("correo");
        List<CursoGrupoDTO> cursos = service.obtenerCursosPorDocente(correo);
        return ResponseEntity.ok(cursos);
    }

    // POST /api/asistencia/verificar-silabo  { codigoCurso: "MAT101" }
    @PostMapping("/verificar-silabo")
    public ResponseEntity<Map<String,Object>> verificarSilabo(@RequestBody Map<String,String> body) {
        String codigo = body.get("codigoCurso");
        Map<String,Object> r = service.verificarSilabo(codigo);
        return ResponseEntity.ok(r);
    }

    // POST /api/asistencia/abrir-control  { grupoId, correoDocente, ip, tipoUbicacion }
    @PostMapping("/abrir-control")
    public ResponseEntity<Map<String,Object>> abrirControl(@RequestBody Map<String,Object> body) {
        Integer grupoId = (Integer) body.get("grupoId");
        String correo = (String) body.get("correoDocente");
        String ip = (String) body.get("ip");
        String tipoUbicacion = (String) body.get("tipoUbicacion");
        Map<String,Object> r = service.abrirControl(grupoId, correo, ip, tipoUbicacion);
        return ResponseEntity.ok(r);
    }

    // POST /api/asistencia/guardar-estudiantes
    @PostMapping("/guardar-estudiantes")
    public ResponseEntity<Map<String,Object>> guardarAsistencias(@RequestBody GuardarAsistenciaRequest req) {
        service.guardarAsistencias(req);
        return ResponseEntity.ok(Map.of("mensaje", "Asistencias actualizadas correctamente"));
    }
}
