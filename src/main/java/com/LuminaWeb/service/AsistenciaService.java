package com.LuminaWeb.service;

import com.LuminaWeb.dto.*;
import com.LuminaWeb.repository.AsistenciaRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AsistenciaService {

    private final AsistenciaRepository repo;

    public AsistenciaService(AsistenciaRepository repo) { this.repo = repo; }

    public List<CursoGrupoDTO> obtenerCursosPorDocente(String correo) {
        return repo.getCursosYGruposPorDocente(correo);
    }

    public Map<String,Object> verificarSilabo(String codigoCurso) {
        List<Map<String,Object>> rows = repo.getSilabosPorCursoRaw(codigoCurso);
        Map<String,Object> resp = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            resp.put("advertencia", "No existe ningún sílabo registrado para el curso " + codigoCurso);
        } else {
            resp.put("silabos", rows);
        }
        return resp;
    }

    // abra control y cree registros necesarios (control_asistencia, asistencias_docente, asistencias_estudiante)
    public Map<String,Object> abrirControl(Integer grupoId, String correoDocente, String ip, String tipoUbicacion) {
        Map<String,Object> resp = new HashMap<>();
        // Determinar dia semana actual
        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        String dia = switch(dow) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            default -> "NINGUNO";
        };
        // obtener horarios
        List<Map<String,Object>> horarios = repo.getHorariosPorGrupoYDia(grupoId, dia);
        if (horarios == null || horarios.isEmpty()) {
            resp.put("estado", "CERRADO");
            resp.put("mensaje", "No hay horario activo para este grupo en el día " + dia);
            resp.put("estudiantes", Collections.emptyList());
            return resp;
        }

        // Para cada horario activo (normalmente 1) procesar
        Map<String,Object> h = horarios.get(0);
        Integer idHorario = (Integer) h.get("id_horario");
        LocalTime horaInicio = ((java.sql.Time) h.get("hora_inicio")).toLocalTime();
        // hora cierre = hora_inicio + 20 minutos
        LocalTime horaCierre = horaInicio.plusMinutes(20);
        LocalTime ahora = LocalTime.now();

        String fecha = LocalDate.now().toString(); // yyyy-MM-dd
        String horaApertura = horaInicio.toString();
        String horaCierreStr = horaCierre.toString();

        String estado = (ahora.isAfter(horaInicio.minusSeconds(1)) && ahora.isBefore(horaCierre.plusSeconds(1))) ? "ABIERTO" : "CERRADO";
        // insertar control_asistencia
        int idControl = repo.insertarControlAsistencia(idHorario, fecha, horaApertura, horaCierreStr, estado);

        // obtener id docente
        Integer idDocente = repo.getIdDocentePorCorreo(correoDocente);
        boolean docentePresente = "ABIERTO".equals(estado);

        // insertar asistencias_docente
        String horaRegistro = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (idDocente != null) {
            repo.insertarAsistenciaDocente(idHorario, idDocente, fecha, horaRegistro, ip, tipoUbicacion, docentePresente);
        }

        // obtener estudiantes del grupo e insertar asistencias_estudiante con FALTA por defecto
        List<EstudianteAsistenciaDTO> estudiantes = repo.getEstudiantesPorGrupo(grupoId, fecha, idHorario, idDocente != null ? idDocente : -1);
        // insertar en bloque (ignora duplicados)
        repo.insertarAsistenciasEstudianteBulk(idHorario, fecha, estudiantes, idDocente != null ? idDocente : -1);

        resp.put("estado", estado);
        resp.put("idHorario", idHorario);
        resp.put("idControl", idControl);
        resp.put("mensaje", docentePresente ? "Asistencia docente registrada: PRESENTE" : "Registro docente insertado: AUSENTE");
        resp.put("estudiantes", estudiantes);
        return resp;
    }

    public void guardarAsistencias(GuardarAsistenciaRequest req) {
        // actualizar cada registro en asistencias_estudiante
        for (EstudianteAsistenciaDTO s : req.getActualizaciones()) {
            repo.actualizarAsistenciaEstudiante(s.getIdMatricula(), req.getIdHorario(), req.getFecha(), s.getEstado_asistencia());
        }
        // Marcar un tema como completado para el curso correspondiente
        String codigoCurso = repo.getCodigoCursoPorHorario(req.getIdHorario());
        if (codigoCurso != null) {
            repo.marcarTemaCompletado(codigoCurso);
        }
    }
}
