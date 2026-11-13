package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionAplicadaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AlertaPlazoEvaluacionService {

    private final EvaluacionAplicadaRepository evaluacionRepository;
    private final NotificacionService notificacionService;

    public AlertaPlazoEvaluacionService(EvaluacionAplicadaRepository evaluacionRepository,
                                        NotificacionService notificacionService) {
        this.evaluacionRepository = evaluacionRepository;
        this.notificacionService = notificacionService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void verificarPlazosPendientes() {
        // Calculamos la fecha objetivo directamente sin declarar variables intermedias sin usar
        LocalDate fechaAlerta = LocalDate.now().plusDays(2);

        List<EvaluacionAplicada> pautasPendientes = evaluacionRepository.findByEstadoAndFechaLimite("PENDIENTE", fechaAlerta);

        for (EvaluacionAplicada eval : pautasPendientes) {
            LineaPractica linea = eval.getLinea();
            if (linea != null) {
                String mensaje = "Estimado/a, la pauta de evaluación se encuentra próxima a vencer el " + eval.getFechaLimite();
                String enlace = "/evaluaciones/linea/" + linea.getIdLinea();

                // Notificar al Profesor Colaborador
                if (linea.getColaborador() != null) {
                    notificacionService.crear(
                            linea.getColaborador().getIdUsuario(),
                            "PAUTA_PENDIENTE",
                            mensaje,
                            enlace
                    );
                }

                // Notificar al Profesor Tutor (si existe)
                if (linea.getTutor() != null) {
                    notificacionService.crear(
                            linea.getTutor().getIdUsuario(),
                            "PAUTA_PENDIENTE",
                            mensaje,
                            enlace
                    );
                }
            }
        }
    }
}
