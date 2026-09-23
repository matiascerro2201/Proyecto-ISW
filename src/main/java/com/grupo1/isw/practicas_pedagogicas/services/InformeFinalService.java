package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeFinalResponse;
import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.repositories.InformeFinalRepository;
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

    @Transactional(readOnly = true)
    public InformeFinalResponse obtenerInformeParaEstudiante(Long idInforme, Long idEstudiante) {
        InformeFinal informe = informeFinalRepository.findByIdConPropietario(idInforme)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El informe final no existe"));

        Long idPropietario = informe.getInscripcion().getEstudiante().getIdUsuario();
        if (!Objects.equals(idPropietario, idEstudiante)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para ver este informe");
        }

        if (informe.getFechaPublicacion() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El informe final aún no ha sido publicado");
        }

        return InformeFinalResponse.desde(informe);
    }

    @Transactional(readOnly = true)
    public List<InformeFinalResponse> listarInformesPublicados(Long idEstudiante) {
        return informeFinalRepository.findPublicadosByEstudiante(idEstudiante)
                .stream()
                .map(InformeFinalResponse::desde)
                .toList();
    }
}
