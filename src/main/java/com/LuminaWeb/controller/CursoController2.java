package com.LuminaWeb.controller;

import com.LuminaWeb.service.CursoService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/cursos")
public class CursoController2 {

    @Autowired
    private CursoService2 cursoService;

    // =============================
    // LISTAR TODOS LOS CURSOS
    // =============================
    @GetMapping("/listar")
    public List<Map<String,Object>> listar() {
        return cursoService.listarCursosConDetalles();
    }

    // =============================
    // VERIFICAR SI EXISTE CURSO CON MISMO CÓDIGO Y GRUPO
    // =============================
    @GetMapping("/verificar/{codigo}/{grupo}")
    public ResponseEntity<Map<String, Object>> verificarCurso(
            @PathVariable String codigo, 
            @PathVariable String grupo) {
        try {
            boolean existeGrupo = cursoService.existeCursoConMismoGrupo(codigo, grupo);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("existe", existeGrupo);
            respuesta.put("mensaje", existeGrupo ? 
                "Ya existe un curso con código " + codigo + " y grupo " + grupo : 
                "Puede crear el curso");
            
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar curso"));
        }
    }

    // =============================
    // GUARDAR CURSO
    // =============================
    @PostMapping("/guardar")
    public ResponseEntity<String> guardarCurso(@RequestBody Map<String, Object> datos) {
        try {
            String codigo = datos.get("codigo").toString();
            String nombre = datos.get("nombre").toString();
            String letraGrupo = datos.get("letraGrupo").toString();
            
            if (codigo == null || codigo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El código del curso es obligatorio");
            }
            
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del curso es obligatorio");
            }
            
            boolean existe = cursoService.existeCursoConMismoGrupo(codigo, letraGrupo);
            
            if (existe) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Ya existe un curso con código " + codigo + " y grupo " + letraGrupo);
            }
            
            cursoService.guardarCurso(datos);
            return ResponseEntity.ok("Curso guardado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al guardar curso: " + e.getMessage());
        }
    }

    // =============================
    // OBTENER CURSO POR CÓDIGO Y GRUPO
    // =============================
    @GetMapping("/{codigo}")
    public ResponseEntity<Map<String,Object>> obtenerCurso(
            @PathVariable String codigo,
            @RequestParam(required = false) String grupo) {
        try {
            Map<String,Object> curso;
            
            if (grupo != null && !grupo.trim().isEmpty()) {
                curso = cursoService.obtenerCursoPorCodigoYGrupo(codigo, grupo);
            } else {
                curso = cursoService.obtenerCurso(codigo);
            }
            
            if (curso != null) {
                return ResponseEntity.ok(curso);
            } else {
                String mensaje = grupo != null ? 
                    "No se encontró curso con código " + codigo + " y grupo " + grupo :
                    "No se encontró curso con código " + codigo;
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Map.of("error", mensaje));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error al obtener curso: " + e.getMessage()));
        }
    }

    // =============================
    // ACTUALIZAR CURSO
    // =============================
    @PutMapping("/actualizar/{codigo}")
    public ResponseEntity<String> actualizarCurso(@PathVariable String codigo,
                                                  @RequestBody Map<String,Object> datos) {
        try {
            cursoService.actualizarCurso(codigo, datos);
            return ResponseEntity.ok("Curso actualizado correctamente");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al actualizar curso: " + e.getMessage());
        }
    }

    // =============================
    // ELIMINAR CURSO
    // =============================
    @DeleteMapping("/eliminar/{codigo}")
    public ResponseEntity<String> eliminarCurso(@PathVariable String codigo) {
        try {
            cursoService.eliminarCurso(codigo);
            return ResponseEntity.ok("Curso eliminado correctamente");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al eliminar curso: " + e.getMessage());
        }
    }
}