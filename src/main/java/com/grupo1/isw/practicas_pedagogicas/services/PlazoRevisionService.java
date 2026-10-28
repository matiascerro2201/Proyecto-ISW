package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.PlazoRevisionResponse;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.SeccionAsignaturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HU-12: el profesor de la asignatura define y visualiza la fecha límite para revisar los informes de
 * cada una de sus secciones.
 *
 * <p>Un informe queda "por revisar" mientras no esté publicado (HU-11), sin contar inscripciones retiradas.
 */
@Service
public class PlazoRevisionService {
    private final SeccionAsignaturaRepository seccionRepository;
    private final ProfesorAsignaturaRepository profesorRepository;
    private final Clock reloj;

    @Autowired
    public PlazoRevisionService(SeccionAsignaturaRepository seccionRepository,
                                ProfesorAsignaturaRepository profesorRepository) {
        this(seccionRepository, profesorRepository, Clock.systemDefaultZone());
    }

    /** Para pruebas: permite fijar el día actual. */
    public PlazoRevisionService(SeccionAsignaturaRepository seccionRepository,
                                ProfesorAsignaturaRepository profesorRepository,
                                Clock reloj) {
        this.seccionRepository = seccionRepository;
        this.profesorRepository = profesorRepository;
        this.reloj = reloj;
    }

    /** Secciones del profesor ordenadas por urgencia: primero la fecha más próxima, al final las sin fecha. */
    @Transactional(readOnly = true)
    public List<PlazoRevisionResponse> listarPlazos(Long idProfesor) {
        LocalDate hoy = LocalDate.now(reloj);
        Map<Long, List<InscripcionAsignatura>> porSeccion = profesorRepository.findInscripcionesDelProfesor(idProfesor)
                .stream()
                .filter(ins -> ins.getEstado() != EstadoInscripcion.RETIRADO)
                .collect(Collectors.groupingBy(ins -> ins.getSeccion().getIdSeccion()));

        return seccionRepository.findDelProfesor(idProfesor).stream()
                .map(seccion -> {
                    List<InscripcionAsignatura> inscripciones = porSeccion.getOrDefault(seccion.getIdSeccion(), List.of());
                    int pendientes = (int) inscripciones.stream().filter(PlazoRevisionService::porRevisar).count();
                    return PlazoRevisionResponse.de(seccion.getIdSeccion(), seccion.getAsignatura().getNombre(),
                            seccion.getAnio(), seccion.getSemestre(), seccion.getFechaLimiteRevision(),
                            pendientes, inscripciones.size(), hoy);
                })
                .sorted(Comparator.comparing(PlazoRevisionResponse::fechaLimite,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Define, cambia o quita (texto vacío) la fecha límite de revisión de una sección.
     *
     * @return la fecha guardada, o null si se quitó
     * @throws IllegalArgumentException la fecha no es válida, es anterior a hoy o está a más de un año
     * @throws ResponseStatusException  404 si la sección no existe, 403 si no la dicta el profesor
     */
    @Transactional
    public LocalDate guardarFechaLimite(Long idProfesor, Long idSeccion, String fechaTexto) {
        SeccionAsignatura seccion = seccionRepository.findById(idSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sección no existe"));
        if (seccion.getProfesor() == null
                || !Objects.equals(seccion.getProfesor().getIdUsuario(), idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No dictas esta sección");
        }

        LocalDate fecha = parsear(fechaTexto);
        seccion.setFechaLimiteRevision(fecha);
        seccionRepository.save(seccion);
        return fecha;
    }

    private LocalDate parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(texto.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha límite no es válida");
        }
        LocalDate hoy = LocalDate.now(reloj);
        if (fecha.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a hoy");
        }
        if (fecha.isAfter(hoy.plusYears(1))) {
            throw new IllegalArgumentException("La fecha límite no puede estar a más de un año");
        }
        return fecha;
    }

    private static boolean porRevisar(InscripcionAsignatura inscripcion) {
        return inscripcion.getInformeFinal() == null || inscripcion.getInformeFinal().getFechaPublicacion() == null;
    }
}
