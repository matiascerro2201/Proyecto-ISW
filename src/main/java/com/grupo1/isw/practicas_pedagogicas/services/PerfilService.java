package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.PerfilResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PerfilResponse.Curso;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.Etiquetas;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.InscripcionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perfil del usuario con sesión: sus datos y los cursos en que participa, según su rol.
 * Un curso es "actual" mientras la inscripción del estudiante está EN_CURSO; después pasa a "pasado".
 * Siempre se arma con el id de la sesión, así nadie ve el perfil de otro.
 */
@Service
public class PerfilService {
    private static final String SIN_DATO = "—";
    private static final Comparator<Curso> MAS_RECIENTE_PRIMERO = Comparator
            .comparingInt(Curso::anio).reversed()
            .thenComparing(Comparator.comparingInt(Curso::semestre).reversed())
            .thenComparing(Curso::asignatura);

    private final UsuarioRepository usuarioRepository;
    private final InscripcionAsignaturaRepository inscripcionRepository;
    private final LineaPracticaRepository lineaRepository;
    private final SeccionAsignaturaRepository seccionRepository;

    public PerfilService(UsuarioRepository usuarioRepository,
                         InscripcionAsignaturaRepository inscripcionRepository,
                         LineaPracticaRepository lineaRepository,
                         SeccionAsignaturaRepository seccionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.lineaRepository = lineaRepository;
        this.seccionRepository = seccionRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        RolUsuario rol = usuario.getRol();
        List<Curso> actuales = new ArrayList<>();
        List<Curso> pasados = new ArrayList<>();

        switch (rol) {
            case ESTUDIANTE -> repartir(cursosDelEstudiante(idUsuario), actuales, pasados);
            case COLABORADOR -> repartir(cursosDeLineas(lineaRepository.findByColaborador(idUsuario)), actuales, pasados);
            case TUTOR -> repartir(cursosDeLineas(lineaRepository.findByTutor(idUsuario)), actuales, pasados);
            case PROFESOR_ASIGNATURA -> repartir(cursosDelProfesor(idUsuario), actuales, pasados);
            case COORDINADOR -> {
                // el coordinador administra el sistema, no participa de cursos
            }
        }
        actuales.sort(MAS_RECIENTE_PRIMERO);
        pasados.sort(MAS_RECIENTE_PRIMERO);

        return new PerfilResponse(
                usuario.getNombre(),
                usuario.getEmail(),
                Etiquetas.rol(rol),
                usuario instanceof Estudiante est ? est.getCarrera() : null,
                usuario instanceof ProfesorColaborador col && col.getCentro() != null
                        ? col.getCentro().getNombre() : null,
                usuario.getCreadoEn(),
                usuario.isActivo(),
                rol != RolUsuario.COORDINADOR,
                etiquetaDetalle(rol),
                actuales,
                pasados);
    }

    private static String etiquetaDetalle(RolUsuario rol) {
        return switch (rol) {
            case ESTUDIANTE -> "Centro de práctica";
            case COLABORADOR, TUTOR -> "Estudiante";
            case PROFESOR_ASIGNATURA -> "Estudiantes";
            case COORDINADOR -> SIN_DATO;
        };
    }

    /** Cada curso lleva su estado para saber en qué lista va; ver {@link #repartir}. */
    private record CursoConEstado(Curso curso, boolean actual) {
    }

    private List<CursoConEstado> cursosDelEstudiante(Long idEstudiante) {
        return inscripcionRepository.findByEstudianteConAsignatura(idEstudiante).stream()
                .map(ins -> new CursoConEstado(
                        curso(ins, ins.getCentro() != null ? ins.getCentro().getNombre() : SIN_DATO),
                        ins.getEstado() == EstadoInscripcion.EN_CURSO))
                .toList();
    }

    /** Colaborador y tutor: una fila por estudiante inscrito, aunque tenga varias líneas con ellos. */
    private List<CursoConEstado> cursosDeLineas(List<LineaPractica> lineas) {
        Map<Long, InscripcionAsignatura> inscripciones = new LinkedHashMap<>();
        for (LineaPractica linea : lineas) {
            inscripciones.putIfAbsent(linea.getInscripcion().getIdInscripcion(), linea.getInscripcion());
        }
        return inscripciones.values().stream()
                .map(ins -> new CursoConEstado(
                        curso(ins, ins.getEstudiante().getNombre()),
                        ins.getEstado() == EstadoInscripcion.EN_CURSO))
                .toList();
    }

    /** Profesor de asignatura: una fila por sección; está vigente si alguno de sus estudiantes sigue en curso. */
    private List<CursoConEstado> cursosDelProfesor(Long idProfesor) {
        List<InscripcionAsignatura> inscripciones = inscripcionRepository.findByProfesor(idProfesor);
        Map<Long, Long> estudiantesPorSeccion = new HashMap<>();
        Set<Long> seccionesVigentes = inscripciones.stream()
                .filter(ins -> ins.getEstado() == EstadoInscripcion.EN_CURSO)
                .map(ins -> ins.getSeccion().getIdSeccion())
                .collect(Collectors.toSet());
        inscripciones.forEach(ins -> estudiantesPorSeccion.merge(ins.getSeccion().getIdSeccion(), 1L, Long::sum));

        List<CursoConEstado> cursos = new ArrayList<>();
        for (SeccionAsignatura seccion : seccionRepository.findDelProfesor(idProfesor)) {
            long cantidad = estudiantesPorSeccion.getOrDefault(seccion.getIdSeccion(), 0L);
            boolean vigente = seccionesVigentes.contains(seccion.getIdSeccion());
            cursos.add(new CursoConEstado(
                    new Curso(seccion.getAsignatura().getNombre(), seccion.getAnio(), seccion.getSemestre(),
                            vigente ? "En curso" : "Finalizado",
                            cantidad == 1 ? "1 estudiante" : cantidad + " estudiantes"),
                    vigente));
        }
        return cursos;
    }

    private Curso curso(InscripcionAsignatura inscripcion, String detalle) {
        SeccionAsignatura seccion = inscripcion.getSeccion();
        return new Curso(seccion.getAsignatura().getNombre(), seccion.getAnio(), seccion.getSemestre(),
                Etiquetas.estadoInscripcion(inscripcion.getEstado()), detalle);
    }

    private void repartir(List<CursoConEstado> cursos, List<Curso> actuales, List<Curso> pasados) {
        for (CursoConEstado c : cursos) {
            (c.actual() ? actuales : pasados).add(c.curso());
        }
    }
}
