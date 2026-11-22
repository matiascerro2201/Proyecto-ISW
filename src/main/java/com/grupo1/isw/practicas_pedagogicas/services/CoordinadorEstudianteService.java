package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.EstudianteDetalle;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.EstudianteResumen;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.repositories.CoordinadorConsultaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HU-37: lista de todos los estudiantes del sistema y, de cada uno, sus cursos y el estado de sus actividades. */
@Service
public class CoordinadorEstudianteService {
    private final CoordinadorConsultaRepository consulta;
    private final ActividadInscripcionService actividadService;

    public CoordinadorEstudianteService(CoordinadorConsultaRepository consulta,
                                        ActividadInscripcionService actividadService) {
        this.consulta = consulta;
        this.actividadService = actividadService;
    }

    /** Todos los estudiantes por nombre. El texto busca en nombre, correo y carrera, sin importar tildes. */
    @Transactional(readOnly = true)
    public List<EstudianteResumen> listar(String texto) {
        String buscado = TextoBusqueda.normalizar(texto);
        Map<Long, Long> cursos = new HashMap<>();
        for (Object[] f : consulta.contarInscripcionesPorEstudiante()) {
            cursos.put(((Number) f[0]).longValue(), ((Number) f[1]).longValue());
        }
        return consulta.findEstudiantes().stream()
                .filter(e -> TextoBusqueda.contiene(e.getNombre(), buscado)
                        || TextoBusqueda.contiene(e.getEmail(), buscado)
                        || TextoBusqueda.contiene(e.getCarrera(), buscado))
                .map(e -> new EstudianteResumen(e.getIdUsuario(), e.getNombre(), e.getEmail(), e.getCarrera(),
                        e.isActivo(), cursos.getOrDefault(e.getIdUsuario(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstudianteDetalle detalle(Long idEstudiante) {
        Estudiante estudiante = consulta.findEstudiante(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new EstudianteDetalle(estudiante.getIdUsuario(), estudiante.getNombre(), estudiante.getEmail(),
                estudiante.getCarrera(), estudiante.isActivo(),
                actividadService.armar(consulta.findInscripcionesDeEstudiante(idEstudiante)));
    }
}
