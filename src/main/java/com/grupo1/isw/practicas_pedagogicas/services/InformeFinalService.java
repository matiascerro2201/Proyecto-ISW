package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.DesgloseInforme;
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
    private final ConstructorInforme constructorInforme;

    public InformeFinalService(InformeFinalRepository informeFinalRepository,
                               ConstructorInforme constructorInforme) {
        this.informeFinalRepository = informeFinalRepository;
        this.constructorInforme = constructorInforme;
    }

    @Transactional(readOnly = true)
    public InformeFinalResponse obtenerInformeParaEstudiante(Long idInforme, Long idEstudiante) {
        return InformeFinalResponse.desde(cargarPublicadoDelEstudiante(idInforme, idEstudiante));
    }

    /** HU-10: cómo se evaluó al estudiante y por qué; mismas reglas de acceso que el informe (dueño y publicado). */
    @Transactional(readOnly = true)
    public DesgloseInforme obtenerDesgloseParaEstudiante(Long idInforme, Long idEstudiante) {
        InformeFinal informe = cargarPublicadoDelEstudiante(idInforme, idEstudiante);
        return constructorInforme.construir(informe.getInscripcion());
    }

    private InformeFinal cargarPublicadoDelEstudiante(Long idInforme, Long idEstudiante) {
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

        return informe;
    }

    @Transactional(readOnly = true)
    public List<InformeFinalResponse> listarInformesPublicados(Long idEstudiante) {
        return informeFinalRepository.findPublicadosByEstudiante(idEstudiante)
                .stream()
                .map(InformeFinalResponse::desde)
                .toList();
    }
}
