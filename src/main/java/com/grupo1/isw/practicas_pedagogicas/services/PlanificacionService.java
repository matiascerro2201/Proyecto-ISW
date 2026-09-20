package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlanificacionService {

    private final PlanificacionRepository planificacionRepository;

    public PlanificacionService(PlanificacionRepository planificacionRepository) {
        this.planificacionRepository = planificacionRepository;
    }

    @Transactional
    public void evaluarPlanificacion(Long idPlanificacion, Long idColaborador, String retroalimentacion, EstadoPlanificacion nuevoEstado) {
        Planificacion planificacion = obtenerPlanificacion(idPlanificacion);

        if (!planificacion.getLinea().getColaborador().getIdUsuario().equals(idColaborador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para evaluar esta planificación");
        }

        if (nuevoEstado != EstadoPlanificacion.APROBADA && nuevoEstado != EstadoPlanificacion.RECHAZADA) {
            throw new IllegalArgumentException("La decisión debe ser APROBADA o RECHAZADA");
        }

        if (planificacion.getEstado() != EstadoPlanificacion.ENVIADA) {
            throw new IllegalStateException("Solo se pueden evaluar planificaciones en estado ENVIADA");
        }

        String retro = (retroalimentacion == null || retroalimentacion.isBlank()) ? null : retroalimentacion.trim();

        if (nuevoEstado == EstadoPlanificacion.RECHAZADA && retro == null) {
            throw new IllegalArgumentException("Debe ingresar retroalimentación para rechazar la planificación");
        }

        planificacion.setRetroalimentacion(retro);
        planificacion.setEstado(nuevoEstado);
        planificacion.setFechaRetro(LocalDateTime.now());

        planificacionRepository.save(planificacion);
    }

    public Planificacion obtenerPlanificacion(Long id) {
        return planificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planificación no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Planificacion> obtenerPendientesDelColaborador(Long idColaborador) {
        return planificacionRepository
                .findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
                        EstadoPlanificacion.ENVIADA, idColaborador);
    }
}
