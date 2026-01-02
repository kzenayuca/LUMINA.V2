package com.LuminaWeb.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.LuminaWeb.dao.SyllabusDao;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class SyllabusService {

    private final SyllabusDao dao;
    // Ruta donde guardar archivos (ajusta a tu servidor)
    private final String uploadBase = "uploads/silabos";

    public SyllabusService(SyllabusDao dao) {
        this.dao = dao;
    }

    public List<Map<String,String>> getCursosPorProfesor(String correo) {
        // delegar al DAO (implementa la consulta JOIN adecuada)
        return dao.getCursosPorCorreoProfesor(correo);
    }

    public boolean existeSilabo(String codigoCurso) {
        return dao.existeSilabo(codigoCurso);
    }

    public void procesarUploadSyllabus(String codigoCurso, String correoProfesor, MultipartFile pdfFile, MultipartFile excelFile) throws Exception {
        // validaciones
        if (pdfFile == null || excelFile == null) throw new IllegalArgumentException("Debe enviar PDF y Excel.");
        if (pdfFile.getSize() > 10L * 1024 * 1024 || excelFile.getSize() > 10L * 1024 * 1024) {
            throw new IllegalArgumentException("Uno de los archivos excede 10MB.");
        }

        // crear carpeta si no existe
        Files.createDirectories(Paths.get(uploadBase));

        // guardar PDF (nombre único con timestamp)
        String pdfFilename = codigoCurso + "_silabo_" + System.currentTimeMillis() + ".pdf";
        File pdfDest = Paths.get(uploadBase, pdfFilename).toFile();
        pdfFile.transferTo(pdfDest);

        // guardar Excel
        String excelFilename = codigoCurso + "_temario_" + System.currentTimeMillis() + "_" + excelFile.getOriginalFilename();
        File excelDest = Paths.get(uploadBase, excelFilename).toFile();
        excelFile.transferTo(excelDest);

        // Insertar fila en tabla silabos (si no existe)
        if (!dao.existeSilabo(codigoCurso)) {
            // aquí suponemos que se necesita grupo_teoria = 'A' por defecto (ajusta según tu lógica)
            dao.insertarSilabo(codigoCurso, /*id_ciclo*/ null, "A", pdfDest.getPath(), correoProfesor);
        } else {
            // si ya existe, podrías actualizar ruta y estado según política; por ahora vamos a lanzar error
            // o bien actualizar. Optamos por actualizar la ruta y poner estado PENDIENTE -> APROBADO según reglas
            dao.actualizarRutaSilabo(codigoCurso, pdfDest.getPath(), correoProfesor);
        }

        // Leer excel y poblar unidades/temas
        try (InputStream in = Files.newInputStream(excelDest.toPath())) {
            dao.importarUnidadesYTemasDesdeExcel(in, /*id_silabo*/ null /* dao puede devolver id_silabo si lo requiere */);
        }
    }
}