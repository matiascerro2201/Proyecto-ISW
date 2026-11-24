package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.AvisoNotificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.EvaluacionesEsperadasRepository.EvaluacionEsperada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * N-17: avisos a colaboradores y tutores por las pautas con fecha límite que siguen sin aplicar: uno cuando el plazo
 * está cerca y otro cuando ya venció. Son solo avisos: nunca bloquean la evaluación. Cada aviso se crea una sola vez
 * (si el plazo cambia, el mensaje cambia y vuelve a avisar).
 */
@Service
public class AvisoPlazoService {
    private static final Logger LOG = LoggerFactory.getLogger(AvisoPlazoService.class);
    public static final String TIPO_PROXIMO = "PLAZO_PAUTA_PROXIMO";
    public static final String TIPO_VENCIDO = "PLAZO_PAUTA_VENCIDO";
    /** Se avisa desde este número de días antes del plazo. */
    public static final int DIAS_PREVIOS = 3;
    /** Un plazo vencido hace más de esto ya no genera aviso (evita avisar de semestres antiguos). */
    public static final int DIAS_MAXIMOS_ATRASO = 30;
    static final String ENLACE = "/evaluador/pautas";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EvaluacionesEsperadasRepository repositorio;
    private final AvisoNotificacionRepository avisos;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    @Autowired
    public AvisoPlazoService(EvaluacionesEsperadasRepository repositorio, AvisoNotificacionRepository avisos,
                             NotificacionService notificaciones) {
        this(repositorio, avisos, notificaciones, Clock.systemDefaultZone());
    }

    public AvisoPlazoService(EvaluacionesEsperadasRepository repositorio, AvisoNotificacionRepository avisos,
                             NotificacionService notificaciones, Clock reloj) {
        this.repositorio = repositorio;
        this.avisos = avisos;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    /** Todos los días a las 07:15. */
    @Scheduled(cron = "0 15 7 * * *")
    public void revisarProgramado() {
        try {
            int creados = revisar();
            if (creados > 0) {
                LOG.info("Avisos de plazo de pautas creados: {}", creados);
            }
        } catch (RuntimeException e) {
            LOG.error("No se pudieron crear los avisos de plazo de pautas", e);
        }
    }

    /** @return cuántos avisos nuevos se crearon */
    public int revisar() {
        LocalDate hoy = LocalDate.now(reloj);
        int creados = 0;
        for (EvaluacionEsperada e : repositorio.conPlazo()) {
            if (e.aplicada() || e.idEvaluador() == null) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(hoy, e.fechaLimite());
            if (dias >= 0 && dias <= DIAS_PREVIOS) {
                creados += avisar(e, TIPO_PROXIMO, "Se acerca el plazo: la pauta «" + e.pauta() + "» de "
                        + e.estudiante() + " (" + e.asignatura() + ") vence el " + e.fechaLimite().format(FECHA)
                        + ".");
            } else if (dias < 0 && dias >= -DIAS_MAXIMOS_ATRASO) {
                creados += avisar(e, TIPO_VENCIDO, "Venció el plazo (" + e.fechaLimite().format(FECHA)
                        + ") de la pauta «" + e.pauta() + "» de " + e.estudiante() + " (" + e.asignatura()
                        + "). Sigue pendiente y puedes aplicarla igual.");
            }
        }
        return creados;
    }

    private int avisar(EvaluacionEsperada e, String tipo, String mensaje) {
        if (avisos.existe(e.idEvaluador(), tipo, mensaje)) {
            return 0;
        }
        notificaciones.crear(e.idEvaluador(), tipo, mensaje, ENLACE);
        return 1;
    }
}
