package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeTutorResponse;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * HU-14: el profesor supervisor (tutor) consulta los informes finales de los estudiantes a su cargo
 * para monitorear su desempeño.
 *
 * <p>Regla de visibilidad: el tutor ve solo informes <b>publicados</b>, igual que el estudiante (HU-16).
 * Mientras el profesor de la asignatura no publica, el informe es un borrador y su nota puede cambiar.
 * Si el cliente decide mostrar también los pendientes, el cambio está en dos lugares:
 * {@code InformeFinalRepository.findPublicadosDelTutor} y {@link #exigirPublicado}.
 *
 * <p>Orden de las validaciones en {@link #obtener}: existe (404) → es de un estudiante de este tutor (403)
 * → está publicado (404). La propiedad va antes que la publicación, como en {@link InformeFinalService},
 * para que nadie pueda deducir si el informe de un estudiante ajeno ya se publicó.
 */
@Service
public class InformeTutorService {
    private final InformeFinalRepository informeFinalRepository;
    private final LineaPracticaRepository lineaPracticaRepository;

    public InformeTutorService(InformeFinalRepository informeFinalRepository,
                               LineaPracticaRepository lineaPracticaRepository) {
        this.informeFinalRepository = informeFinalRepository;
        this.lineaPracticaRepository = lineaPracticaRepository;
    }

    /** Informes publicados de los estudiantes del tutor, el más reciente primero; sin repetir ninguno. */
    @Transactional(readOnly = true)
    public List<InformeTutorResponse> listar(Long idTutor) {
        return informeFinalRepository.findPublicadosDelTutor(idTutor).stream()
                .map(InformeTutorResponse::desde)
                // defensa extra: la consulta ya usa DISTINCT, pero un informe nunca debe verse dos veces
                .distinct()
                .toList();
    }

    /**
     * @throws ResponseStatusException 404 si el informe no existe o aún no se publicó,
     *                                 403 si el estudiante no es del tutor
     */
    @Transactional(readOnly = true)
    public InformeTutorResponse obtener(Long idTutor, Long idInforme) {
        InformeFinal informe = informeFinalRepository.findByIdConPropietario(idInforme)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El informe final no existe"));

        Long idInscripcion = informe.getInscripcion().getIdInscripcion();
        if (!lineaPracticaRepository.existsByInscripcion_IdInscripcionAndTutor_IdUsuario(idInscripcion, idTutor)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para ver este informe");
        }

        exigirPublicado(informe);
        return InformeTutorResponse.desde(informe);
    }

    private void exigirPublicado(InformeFinal informe) {
        if (informe.getFechaPublicacion() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El informe final aún no ha sido publicado");
        }
    }
}
