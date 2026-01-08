package com.LuminaWeb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Controller
public class ArchivoController {

    private static final Logger logger = LoggerFactory.getLogger(ArchivoController.class);

    // Usa la ruta absoluta definida en application.properties
    @Value("${app.upload.dir:uploads/pruebas}")
    private String uploadDir;

    @GetMapping("/upload-prueba")
    public String mostrarFormulario() {
        return "upload_form";
    }

    @PostMapping("/upload-prueba")
    public String subirArchivo(@RequestParam("archivo") MultipartFile archivo, Model model) {
        if (archivo == null || archivo.isEmpty()) {
            model.addAttribute("mensaje", "No se seleccionó ningún archivo.");
            return "upload_form";
        }

        String originalFilename = StringUtils.cleanPath(archivo.getOriginalFilename());
        if (originalFilename.contains("..")) {
            model.addAttribute("mensaje", "Nombre de archivo inválido.");
            return "upload_form";
        }

        try {
            // Resuelve y normaliza la ruta absoluta
            Path destinoCarpeta = Paths.get(uploadDir).toAbsolutePath().normalize();
            logger.info("Ruta de uploads (absoluta): {}", destinoCarpeta);

            // Crea todos los directorios padres si no existen
            Files.createDirectories(destinoCarpeta);

            // Nombre único para evitar colisiones
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) extension = originalFilename.substring(i);
            String nuevoNombre = UUID.randomUUID().toString() + extension;

            Path destino = destinoCarpeta.resolve(nuevoNombre);

            // Guardar contenido (funciona dentro de JAR y en IDE)
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            String rutaPublica = "/uploads/pruebas/" + nuevoNombre;

            model.addAttribute("mensaje", "Archivo subido correctamente.");
            model.addAttribute("nombreArchivo", originalFilename);
            model.addAttribute("rutaArchivo", rutaPublica);

            logger.info("Archivo subido: original='{}' guardado en '{}'", originalFilename, destino.toString());

            return "upload_exito";

        } catch (IOException e) {
            logger.error("Error al guardar el archivo", e);
            model.addAttribute("mensaje", "Ocurrió un error al guardar el archivo: " + e.getMessage());
            return "upload_form";
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException ex, Model model) {
        model.addAttribute("mensaje", "El archivo es demasiado grande. Aumenta el límite en application.properties.");
        return "upload_form";
    }
}
