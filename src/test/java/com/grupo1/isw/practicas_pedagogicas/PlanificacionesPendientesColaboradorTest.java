package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.PlanificacionPendienteResponse;
import com.grupo1.isw.practicas_pedagogicas.dto.PlanificacionPendienteResponse.Urgencia;
import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Lista de pendientes del profesor de la asignatura: orden por antigüedad e indicador de urgencia. */
@ExtendWith(MockitoExtension.class)
class PlanificacionesPendientesColaboradorTest {
    private static final Long ID_PROFESOR = 1L;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private LineaPracticaRepository lineaPracticaRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private PlanificacionService service;

    private Planificacion planificacion(long id, String estudiante, LocalDateTime envio, LocalDate revision) {
        Estudiante e = new Estudiante();
        e.setNombre(estudiante);
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        ProfesorAsignatura profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(ID_PROFESOR);
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(e, new SeccionAsignatura(asignatura, profesor, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(99L);
        Planificacion p = new Planificacion(new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD),
                null, envio);
        p.setIdPlanificacion(id);
        p.setEstado(EstadoPlanificacion.ENVIADA);
        p.setFechaRevisionSolicitada(revision);
        return p;
    }

    private Urgencia urgenciaCon(LocalDate revision) {
        return PlanificacionPendienteResponse.de(
                planificacion(1, "Camila", LocalDateTime.of(2026, 11, 10, 9, 0), revision), HOY).urgencia();
    }

    @Test
    @DisplayName("La lista sale de la más antigua a la más reciente según la fecha de envío")
    void lista_ordenadaPorAntiguedad() {
        Planificacion reciente = planificacion(1, "Reciente", LocalDateTime.of(2026, 11, 20, 9, 0), HOY.plusDays(10));
        Planificacion antigua = planificacion(2, "Antigua", LocalDateTime.of(2026, 11, 2, 9, 0), HOY.plusDays(20));
        Planificacion media = planificacion(3, "Media", LocalDateTime.of(2026, 11, 12, 9, 0), HOY.plusDays(1));
        when(planificacionRepository.findByEstadoAndLinea_Inscripcion_Seccion_Profesor_IdUsuarioOrderByFechaEnvioAsc(
                EstadoPlanificacion.ENVIADA, ID_PROFESOR)).thenReturn(List.of(reciente, antigua, media));

        List<PlanificacionPendienteResponse> lista = service.listarPendientesDelColaborador(ID_PROFESOR, HOY);

        assertEquals(List.of("Antigua", "Media", "Reciente"),
                lista.stream().map(PlanificacionPendienteResponse::nombreEstudiante).toList());
    }

    @Test
    @DisplayName("Urgencia: más de 2 días EN_PLAZO, 2 o menos PROXIMA, pasada VENCIDA, sin fecha SIN_FECHA")
    void urgencia_segunDiasRestantes() {
        assertEquals(Urgencia.EN_PLAZO, urgenciaCon(HOY.plusDays(3)));
        assertEquals(Urgencia.PROXIMA, urgenciaCon(HOY.plusDays(2)));
        assertEquals(Urgencia.PROXIMA, urgenciaCon(HOY));
        assertEquals(Urgencia.VENCIDA, urgenciaCon(HOY.minusDays(1)));
        assertEquals(Urgencia.SIN_FECHA, urgenciaCon(null));
    }

    @Test
    @DisplayName("Textos y colores de la urgencia y de la antigüedad")
    void textosYClases() {
        PlanificacionPendienteResponse proxima = PlanificacionPendienteResponse.de(
                planificacion(1, "Camila", LocalDateTime.of(2026, 11, 20, 9, 0), HOY.plusDays(2)), HOY);
        assertEquals("Vence en 2 días", proxima.textoEstado());
        assertEquals("pendiente", proxima.claseEstado());
        assertEquals("Enviada hace 3 días", proxima.textoAntiguedad());

        PlanificacionPendienteResponse vencida = PlanificacionPendienteResponse.de(
                planificacion(2, "Diego", LocalDateTime.of(2026, 11, 23, 8, 0), HOY.minusDays(1)), HOY);
        assertEquals("Venció ayer", vencida.textoEstado());
        assertEquals("rechazada", vencida.claseEstado());
        assertEquals("Enviada hoy", vencida.textoAntiguedad());

        assertEquals("Vence mañana", PlanificacionPendienteResponse.de(
                planificacion(3, "Ana", LocalDateTime.of(2026, 11, 1, 9, 0), HOY.plusDays(1)), HOY).textoEstado());
    }
}
