package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El historial de evaluaciones que lleva cada planificación. */
class EvaluacionPlanificacionTest {

    private static final LocalDateTime PRIMERA = LocalDateTime.of(2026, 10, 2, 10, 0);
    private static final LocalDateTime SEGUNDA = LocalDateTime.of(2026, 10, 9, 15, 30);

    private Planificacion enviada() {
        return new Planificacion(null, "Texto", LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    @Test
    @DisplayName("HU-26. Una planificación nueva, enviada o en borrador, no tiene evaluaciones")
    void sinEvaluacionesAlComienzo() {
        assertTrue(enviada().getEvaluaciones().isEmpty());
        assertTrue(Planificacion.borrador(null, "Texto").getEvaluaciones().isEmpty());
    }

    @Test
    @DisplayName("HU-26. Registrar una evaluación la agrega al historial, en orden y apuntando a su planificación")
    void registrarAgregaEnOrden() {
        Planificacion planificacion = enviada();

        planificacion.registrarEvaluacion(EstadoPlanificacion.RECHAZADA, "Faltan objetivos", PRIMERA);
        planificacion.registrarEvaluacion(EstadoPlanificacion.APROBADA, null, SEGUNDA);

        List<EvaluacionPlanificacion> historial = planificacion.getEvaluaciones();
        assertEquals(2, historial.size());
        assertEquals(EstadoPlanificacion.RECHAZADA, historial.get(0).getDecision());
        assertEquals("Faltan objetivos", historial.get(0).getRetroalimentacion());
        assertEquals(PRIMERA, historial.get(0).getFecha());
        assertSame(planificacion, historial.get(0).getPlanificacion());
        assertEquals(EstadoPlanificacion.APROBADA, historial.get(1).getDecision());
        assertNull(historial.get(1).getRetroalimentacion());
        assertEquals(SEGUNDA, historial.get(1).getFecha());
    }

    @Test
    @DisplayName("HU-26. El historial conserva la retroalimentación aunque la planificación la borre al reenviarse")
    void conservaLaRetroalimentacion() {
        Planificacion planificacion = enviada();
        planificacion.setRetroalimentacion("Faltan objetivos");
        planificacion.registrarEvaluacion(EstadoPlanificacion.RECHAZADA, "Faltan objetivos", PRIMERA);

        planificacion.setRetroalimentacion(null);
        planificacion.setFechaRetro(null);
        planificacion.setEstado(EstadoPlanificacion.ENVIADA);

        assertEquals("Faltan objetivos", planificacion.getEvaluaciones().get(0).getRetroalimentacion());
        assertEquals(EstadoPlanificacion.RECHAZADA, planificacion.getEvaluaciones().get(0).getDecision());
    }

    @Test
    @DisplayName("HU-26. El historial es de solo lectura: no se puede modificar desde fuera")
    void historialDeSoloLectura() {
        Planificacion planificacion = enviada();
        planificacion.registrarEvaluacion(EstadoPlanificacion.APROBADA, null, PRIMERA);

        assertThrows(UnsupportedOperationException.class, () -> planificacion.getEvaluaciones().clear());
        assertThrows(UnsupportedOperationException.class, () -> planificacion.getEvaluaciones().remove(0));
        assertEquals(1, planificacion.getEvaluaciones().size());
    }

    @Test
    @DisplayName("HU-26. Una evaluación no tiene identificador hasta que se guarda")
    void sinIdAntesDeGuardar() {
        Planificacion planificacion = enviada();
        planificacion.registrarEvaluacion(EstadoPlanificacion.APROBADA, null, PRIMERA);

        assertNull(planificacion.getEvaluaciones().get(0).getIdEvaluacion());
    }
}
