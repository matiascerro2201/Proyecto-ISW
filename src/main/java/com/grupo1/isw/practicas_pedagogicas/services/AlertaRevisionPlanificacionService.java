package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Avisa al profesor de la asignatura cuando faltan 2 días para la fecha en que el estudiante pidió la revisión
 * de su planificación. Solo cuenta mientras la planificación sigue ENVIADA (sin evaluar).
 * <p>
 * Corre todos los días a las 08:00. Revisa desde hoy hasta hoy + 2 días y marca cada planificación avisada,
 * así el aviso sale una sola vez y, si la aplicación estuvo caída a las 08:00, se envía al día siguiente
 * en lugar de perderse.
 */
@Service
public class AlertaRevisionPlanificacionService {
    public static final int DIAS_DE_ANTICIPACION = 2;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PlanificacionRepository planificacionRepository;
    private final NotificacionService notificacionService;

    public AlertaRevisionPlanificacionService(PlanificacionRepository planificacionRepository,
                                              NotificacionService notificacionService) {
        this.planificacionRepository = planificacionRepository;
        this.notificacionService = notificacionService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void avisarRevisionesProximas() {
        avisarRevisionesProximas(LocalDate.now());
    }

    /** Separado para poder probarlo con una fecha fija. Devuelve cuántos avisos se crearon. */
    @Transactional
    public int avisarRevisionesProximas(LocalDate hoy) {
        List<Planificacion> proximas = planificacionRepository
                .findByEstadoAndRecordatorioRevisionEnviadoFalseAndFechaRevisionSolicitadaBetween(
                        EstadoPlanificacion.ENVIADA, hoy, hoy.plusDays(DIAS_DE_ANTICIPACION));

        int avisos = 0;
        for (Planificacion planificacion : proximas) {
            LineaPractica linea = planificacion.getLinea();
            ProfesorAsignatura profesor = profesorDe(linea);
            if (profesor == null) {
                continue;
            }
            notificacionService.crear(profesor.getIdUsuario(),
                    NotificacionService.TIPO_REVISION_PROXIMA,
                    mensaje(planificacion, linea, hoy),
                    "/planificaciones/" + planificacion.getIdPlanificacion() + "/evaluar");
            planificacion.marcarRecordatorioRevisionEnviado();
            planificacionRepository.save(planificacion);
            avisos++;
        }
        return avisos;
    }

    /** Quien revisa: el profesor de la sección en que está inscrito el estudiante. */
    private static ProfesorAsignatura profesorDe(LineaPractica linea) {
        if (linea == null || linea.getInscripcion() == null || linea.getInscripcion().getSeccion() == null) {
            return null;
        }
        return linea.getInscripcion().getSeccion().getProfesor();
    }

    private static String mensaje(Planificacion planificacion, LineaPractica linea, LocalDate hoy) {
        long dias = ChronoUnit.DAYS.between(hoy, planificacion.getFechaRevisionSolicitada());
        String cuando = dias <= 0 ? "es hoy" : dias == 1 ? "es mañana" : "es en " + dias + " días";
        String asignatura = linea.getInscripcion().getSeccion().getAsignatura().getNombre();
        return "La revisión de la planificación de " + linea.getInscripcion().getEstudiante().getNombre()
                + " (" + asignatura + ") fue solicitada para el "
                + planificacion.getFechaRevisionSolicitada().format(FORMATO_FECHA) + " (" + cuando + ").";
    }
}
