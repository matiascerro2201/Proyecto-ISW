package com.grupo1.isw.practicas_pedagogicas.service;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeFinalResponse;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.repository.InformeFinalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class InformeFinalService {

    private final InformeFinalRepository informeFinalRepository;

    public InformeFinalService(InformeFinalRepository informeFinalRepository) {
        this.informeFinalRepository = informeFinalRepository;
    }

    /**
     * HU-15 + HU-16: devuelve el informe final solo si el solicitante es su dueño
     * y el informe ya fue publicado.
     *
     * Orden de validación (importante): existencia -> propiedad -> publicación.
     * La propiedad se valida ANTES que la publicación para que un tercero no pueda
     * deducir si el informe de otro estudiante ya fue publicado.
     *
     * @param idInforme    id del informe solicitado
     * @param idEstudiante id (id_usuario) del estudiante que hace la petición
     * @throws ResponseStatusException 404 si no existe o aún no está publicado; 403 si no es el dueño
     */
    @Transactional(readOnly = true)
    public InformeFinalResponse obtenerInformeParaEstudiante(Long idInforme, Long idEstudiante) {

        InformeFinal informe = informeFinalRepository.findByIdConPropietario(idInforme)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El informe final no existe"));

        // HU-16: informe -> inscripcion -> estudiante debe coincidir con quien consulta
        Long idPropietario = informe.getInscripcion().getEstudiante().getIdUsuario();
        if (!Objects.equals(idPropietario, idEstudiante)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para ver este informe");
        }

        // HU-15: la publicación se deriva de fecha_publicacion (no existe campo "estado")
        if (informe.getFechaPublicacion() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El informe final aún no ha sido publicado");
        }

        return InformeFinalResponse.desde(informe);
    }

    /**
     * Lista los informes publicados del estudiante (uno por asignatura cursada).
     * La propiedad queda garantizada porque la consulta filtra por el id del estudiante autenticado.
     */
    @Transactional(readOnly = true)
    public List<InformeFinalResponse> listarInformesPublicados(Long idEstudiante) {
        return informeFinalRepository.findPublicadosByEstudiante(idEstudiante)
                .stream()
                .map(InformeFinalResponse::desde)
                .toList();
    }
}
