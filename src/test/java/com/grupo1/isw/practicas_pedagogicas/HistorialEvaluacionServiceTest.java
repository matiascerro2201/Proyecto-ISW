package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Cada vez que el colaborador evalúa una planificación queda registrada en su historial, con la decisión,
 * la retroalimentación y la fecha. Se usan planificaciones reales (no simuladas) para comprobar el historial.
 */
@ExtendWith(MockitoExtension.class)
class HistorialEvaluacionServiceTest {

    private static final Long ID_PLANIFICACION = 10L;
    private static final Long ID_COLABORADOR = 1L;
    private static final Long ID_OTRO_COLABORADOR = 2L;
    private static final Long ID_ESTUDIANTE = 4L;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private LineaPracticaRepository lineaPracticaRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private PlanificacionService service;

    private LineaPractica linea() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(ID_ESTUDIANTE);
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, null, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(ID_COLABORADOR);
        return new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);
    }

    private Planificacion enviada() {
        Planificacion planificacion = new Planificacion(linea(), "Texto inicial", LocalDateTime.now().minusDays(2));
        planificacion.setIdPlanificacion(ID_PLANIFICACION);
        return planificacion;
    }

    private void dadoQueExiste(Planificacion planificacion) {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(planificacion));
    }

    @Test
    @DisplayName("HU-26. Rechazar registra una evaluación RECHAZADA con la retroalimentación y la misma fecha del rechazo")
    void rechazar_registraLaEvaluacion() {
        Planificacion planificacion = enviada();
        dadoQueExiste(planificacion);

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR, "  Faltan objetivos medibles  ",
                EstadoPlanificacion.RECHAZADA);

        assertEquals(1, planificacion.getEvaluaciones().size());
        EvaluacionPlanificacion evaluacion = planificacion.getEvaluaciones().get(0);
        assertEquals(EstadoPlanificacion.RECHAZADA, evaluacion.getDecision());
        assertEquals("Faltan objetivos medibles", evaluacion.getRetroalimentacion());
        assertEquals(planificacion.getFechaRetro(), evaluacion.getFecha());
        assertSame(planificacion, evaluacion.getPlanificacion());
        assertEquals(EstadoPlanificacion.RECHAZADA, planificacion.getEstado());
    }

    @Test
    @DisplayName("HU-26. Aprobar sin comentario registra APROBADA con retroalimentación null, igual que la planificación")
    void aprobarSinComentario_registraNull() {
        Planificacion planificacion = enviada();
        dadoQueExiste(planificacion);

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR, "   ", EstadoPlanificacion.APROBADA);

        EvaluacionPlanificacion evaluacion = planificacion.getEvaluaciones().get(0);
        assertEquals(EstadoPlanificacion.APROBADA, evaluacion.getDecision());
        assertNull(evaluacion.getRetroalimentacion());
        assertNull(planificacion.getRetroalimentacion());
    }

    @Test
    @DisplayName("HU-26. Una planificación aprobada al primer envío tiene una sola evaluación, y es APROBADA")
    void aprobadaAlPrimerEnvio() {
        Planificacion planificacion = enviada();
        dadoQueExiste(planificacion);

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR, "Excelente", EstadoPlanificacion.APROBADA);

        assertEquals(1, planificacion.getEvaluaciones().size());
        assertEquals(EstadoPlanificacion.APROBADA, planificacion.getEvaluaciones().get(0).getDecision());
    }

    @Test
    @DisplayName("HU-26. Rechazar, reenviar y aprobar deja ambas evaluaciones, y la primera conserva la retroalimentación")
    void cicloCompleto_conservaElHistorial() {
        Planificacion planificacion = enviada();
        dadoQueExiste(planificacion);

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR, "Faltan objetivos medibles",
                EstadoPlanificacion.RECHAZADA);
        service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, "Texto mejorado");

        // Al reenviar, la planificación olvida el rechazo: justo lo que el historial viene a conservar.
        assertEquals(EstadoPlanificacion.ENVIADA, planificacion.getEstado());
        assertNull(planificacion.getRetroalimentacion());
        assertNull(planificacion.getFechaRetro());
        assertEquals(1, planificacion.getEvaluaciones().size());

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR, "Ahora sí", EstadoPlanificacion.APROBADA);

        List<EvaluacionPlanificacion> historial = planificacion.getEvaluaciones();
        assertEquals(2, historial.size());
        assertEquals(EstadoPlanificacion.RECHAZADA, historial.get(0).getDecision());
        assertEquals("Faltan objetivos medibles", historial.get(0).getRetroalimentacion());
        assertEquals(EstadoPlanificacion.APROBADA, historial.get(1).getDecision());
        assertEquals("Ahora sí", historial.get(1).getRetroalimentacion());
        assertFalse(historial.get(1).getFecha().isBefore(historial.get(0).getFecha()));
    }

    @Test
    @DisplayName("HU-26. Una evaluación que falla no registra nada")
    void evaluacionesQueFallan_noRegistran() {
        Planificacion planificacion = enviada();
        dadoQueExiste(planificacion);

        assertThrows(IllegalArgumentException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_COLABORADOR, "x", EstadoPlanificacion.ENVIADA));
        assertThrows(IllegalArgumentException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_COLABORADOR, "  ", EstadoPlanificacion.RECHAZADA));
        ResponseStatusException ajeno = assertThrows(ResponseStatusException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_OTRO_COLABORADOR, "x", EstadoPlanificacion.APROBADA));

        assertEquals(403, ajeno.getStatusCode().value());
        assertTrue(planificacion.getEvaluaciones().isEmpty());
        assertEquals(EstadoPlanificacion.ENVIADA, planificacion.getEstado());
    }

    @Test
    @DisplayName("HU-26. Evaluar una planificación que ya fue evaluada falla y no agrega otra evaluación")
    void yaEvaluada_noRegistraOtra() {
        Planificacion planificacion = enviada();
        planificacion.setEstado(EstadoPlanificacion.APROBADA);
        dadoQueExiste(planificacion);

        assertThrows(IllegalStateException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_COLABORADOR, "x", EstadoPlanificacion.RECHAZADA));

        assertTrue(planificacion.getEvaluaciones().isEmpty());
    }

    @Test
    @DisplayName("HU-26. Evaluar un borrador da 404 y no registra nada")
    void borrador_noSeEvalua() {
        Planificacion borrador = Planificacion.borrador(linea(), "Borrador");
        borrador.setIdPlanificacion(ID_PLANIFICACION);
        dadoQueExiste(borrador);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_COLABORADOR, "x", EstadoPlanificacion.APROBADA));

        assertEquals(404, ex.getStatusCode().value());
        assertTrue(borrador.getEvaluaciones().isEmpty());
    }

    @Test
    @DisplayName("HU-26. Guardar cambios y enviar un borrador no registran evaluaciones")
    void operacionesDelEstudiante_noRegistranEvaluaciones() {
        Planificacion borrador = Planificacion.borrador(linea(), "Borrador");
        borrador.setIdPlanificacion(ID_PLANIFICACION);
        dadoQueExiste(borrador);

        service.guardarCambios(ID_ESTUDIANTE, ID_PLANIFICACION, "Borrador editado");
        service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, "Borrador final");

        assertEquals(EstadoPlanificacion.ENVIADA, borrador.getEstado());
        assertTrue(borrador.getEvaluaciones().isEmpty());
    }
}
