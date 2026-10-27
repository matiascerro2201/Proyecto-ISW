package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme;
import com.grupo1.isw.practicas_pedagogicas.dto.EstudianteInformeResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.EstudianteInformeResponse.Estado;
import com.grupo1.isw.practicas_pedagogicas.dto.InformeProfesorResponse;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.ProfesorAsignaturaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** HU-10: el profesor genera el informe final de sus estudiantes; uno ya validado o publicado (HU-11) no se regenera. */
@Service
public class GeneracionInformeService {
    private final ProfesorAsignaturaRepository profesorRepository;
    private final InformeFinalRepository informeFinalRepository;
    private final ConstructorInforme constructor;

    public GeneracionInformeService(ProfesorAsignaturaRepository profesorRepository,
                                    InformeFinalRepository informeFinalRepository,
                                    ConstructorInforme constructor) {
        this.profesorRepository = profesorRepository;
        this.informeFinalRepository = informeFinalRepository;
        this.constructor = constructor;
    }

    /** Los estudiantes de las secciones que dicta el profesor y el estado del informe de cada uno. */
    @Transactional(readOnly = true)
    public List<EstudianteInformeResponse> listarEstudiantes(Long idProfesor) {
        return profesorRepository.findInscripcionesDelProfesor(idProfesor).stream()
                .map(this::aFila)
                .toList();
    }

    /** Genera o regenera el informe; InformeNoGenerableException si falta algo, está retirada o ya fue validado/publicado,
     *  y ResponseStatusException 404/403 si la inscripción no existe o no es del profesor. */
    @Transactional
    public Long generar(Long idProfesor, Long idInscripcion) {
        InscripcionAsignatura inscripcion = cargarDelProfesor(idInscripcion, idProfesor);

        if (inscripcion.getEstado() == EstadoInscripcion.RETIRADO) {
            throw new InformeNoGenerableException(List.of("La inscripción del estudiante está retirada"));
        }

        InformeFinal informe = inscripcion.getInformeFinal();
        if (informe != null && estaCerrado(informe)) {
            throw new InformeNoGenerableException(List.of(
                    "El informe ya fue validado o publicado y no puede volver a generarse"));
        }

        DesgloseInforme desglose = constructor.construir(inscripcion);
        if (!desglose.completo()) {
            throw new InformeNoGenerableException(desglose.pendientes());
        }

        ProfesorAsignatura corrector = inscripcion.getSeccion().getProfesor();
        if (informe == null) {
            informe = new InformeFinal(inscripcion, corrector);
        } else {
            informe.setCorrector(corrector);
        }
        informe.setCalificacionSugerida(desglose.calificacionCalculada());
        informe.setFechaGeneracion(LocalDateTime.now());

        try {
            return informeFinalRepository.saveAndFlush(informe).getIdInforme();
        } catch (DataIntegrityViolationException e) {
            // id_inscripcion es único: dos clics seguidos pueden intentar crear el mismo informe dos veces.
            throw new InformeNoGenerableException(List.of(
                    "El informe de este estudiante ya fue generado. Actualiza la página e inténtalo de nuevo"));
        }
    }

    /** El informe con su desglose; 404 si no existe, 403 si no es de una sección del profesor. */
    @Transactional(readOnly = true)
    public InformeProfesorResponse obtenerParaProfesor(Long idProfesor, Long idInforme) {
        InformeFinal informe = profesorRepository.findInformeConProfesor(idInforme)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El informe final no existe"));

        InscripcionAsignatura inscripcion = informe.getInscripcion();
        verificarProfesor(inscripcion, idProfesor);

        DesgloseInforme desglose = constructor.construir(inscripcion);
        return InformeProfesorResponse.desde(informe, desglose, estaDesactualizado(informe, desglose));
    }

    private EstudianteInformeResponse aFila(InscripcionAsignatura inscripcion) {
        InformeFinal informe = inscripcion.getInformeFinal();
        var seccion = inscripcion.getSeccion();

        Estado estado;
        List<String> pendientes = List.of();
        if (informe != null) {
            estado = informe.getFechaPublicacion() != null ? Estado.PUBLICADO
                    : informe.getFechaValidacion() != null ? Estado.VALIDADO
                    : Estado.GENERADO;
        } else if (inscripcion.getEstado() == EstadoInscripcion.RETIRADO) {
            estado = Estado.PENDIENTE;
            pendientes = List.of("La inscripción del estudiante está retirada");
        } else {
            DesgloseInforme desglose = constructor.construir(inscripcion);
            estado = desglose.completo() ? Estado.LISTO : Estado.PENDIENTE;
            pendientes = desglose.pendientes();
        }

        return new EstudianteInformeResponse(
                inscripcion.getIdInscripcion(),
                inscripcion.getEstudiante().getNombre(),
                seccion.getAsignatura().getNombre(),
                seccion.getAnio(),
                seccion.getSemestre(),
                estado,
                informe == null ? null : informe.getIdInforme(),
                informe == null ? null : informe.getCalificacionSugerida(),
                informe == null ? null : informe.getFechaGeneracion(),
                pendientes);
    }

    private InscripcionAsignatura cargarDelProfesor(Long idInscripcion, Long idProfesor) {
        InscripcionAsignatura inscripcion = profesorRepository.findInscripcionConProfesor(idInscripcion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La inscripción no existe"));
        verificarProfesor(inscripcion, idProfesor);
        return inscripcion;
    }

    private static void verificarProfesor(InscripcionAsignatura inscripcion, Long idProfesor) {
        Long idProfesorDeLaSeccion = inscripcion.getSeccion().getProfesor().getIdUsuario();
        if (!Objects.equals(idProfesorDeLaSeccion, idProfesor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre este estudiante");
        }
    }

    private static boolean estaCerrado(InformeFinal informe) {
        return informe.getFechaValidacion() != null || informe.getFechaPublicacion() != null;
    }

    /** Avisa si la nota guardada ya no refleja los datos (p. ej. el colaborador editó una pauta después de generar). */
    private static boolean estaDesactualizado(InformeFinal informe, DesgloseInforme desglose) {
        if (!desglose.completo()) {
            return true;
        }
        if (informe.getCalificacionSugerida() != null
                && desglose.calificacionCalculada().compareTo(informe.getCalificacionSugerida()) != 0) {
            return true;
        }
        LocalDateTime ultima = desglose.ultimaModificacion();
        return ultima != null
                && informe.getFechaGeneracion() != null
                && ultima.isAfter(informe.getFechaGeneracion());
    }
}
