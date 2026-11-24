package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.repositories.EntregaEstudianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * N-11: cuando vence el plazo de una entrega, quien no entregó recibe un 1,0. La nota queda sin publicar y el
 * profesor la puede cambiar (N-14). Se revisa cada hora y también al abrir las pantallas del profesor.
 */
@Service
public class NotaAutomaticaService {
    private static final Logger log = LoggerFactory.getLogger(NotaAutomaticaService.class);

    private final EntregaEstudianteRepository repositorio;
    private final Clock reloj;

    @Autowired
    public NotaAutomaticaService(EntregaEstudianteRepository repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }

    public NotaAutomaticaService(EntregaEstudianteRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** Todos los días a partir de las 00:05 y luego cada hora, para que ningún plazo vencido quede sin nota. */
    @Scheduled(cron = "0 5 * * * *")
    public void aplicarProgramado() {
        try {
            int creadas = aplicar(null);
            if (creadas > 0) {
                log.info("Notas 1,0 automáticas creadas por entregas vencidas: {}", creadas);
            }
        } catch (RuntimeException e) {
            log.error("No se pudieron aplicar las notas automáticas por entregas vencidas", e);
        }
    }

    /**
     * Crea las notas 1,0 pendientes. Una fecha límite vence al terminar su día, así que solo cuentan las entregas
     * cuya fecha es anterior a hoy.
     *
     * @param idSeccion un semestre en particular, o null para revisarlos todos
     * @return cuántas notas creó
     */
    public int aplicar(Long idSeccion) {
        return repositorio.crearNotasAutomaticas(LocalDate.now(reloj), LocalDateTime.now(reloj), idSeccion);
    }
}
