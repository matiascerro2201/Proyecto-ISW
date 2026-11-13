package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Notificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertaPlanificacionEstudianteService {

    @Autowired
    private PlanificacionRepository planificacionRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // Se ejecuta según la periodicidad que configures (ej. todos los días a las 08:00 AM)
    @Scheduled(cron = "0 0 8 * * ?") 
    @Transactional
    public void verificarPlanificacionesPendientes() {
        // 1. Buscamos las planificaciones que estén en estado BORRADOR (pendientes de elaborar/enviar)
        // Nota: Si no tienes este método en tu PlanificacionRepository, puedes agregarlo fácilmente.
        List<Planificacion> planificacionesPendientes = planificacionRepository.findByEstado(EstadoPlanificacion.BORRADOR);

        // 2. Recorremos cada planificación pendiente para notificar a su respectivo estudiante
        for (Planificacion planificacion : planificacionesPendientes) {
            // Obtenemos el estudiante a través de la línea y su inscripción
            var estudiante = planificacion.getLinea().getInscripcion().getEstudiante();

            if (estudiante != null) {
                // 3. Creamos la notificación
                Notificacion notificacion = new Notificacion();
                notificacion.setUsuario(estudiante);
                notificacion.setTipo("PLANIFICACION_PENDIENTE");
                notificacion.setMensaje("Tienes una planificación de clase pendiente de elaborar según el calendario de tu asignatura.");
                notificacion.setLeida(false);
                notificacion.setFecha(LocalDateTime.now());

                // 4. Guardamos la notificación en la base de datos
                notificacionRepository.save(notificacion);
            }
        }
    }
}