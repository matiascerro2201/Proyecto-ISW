package com.grupo1.isw.practicas_pedagogicas;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La fecha de revisión que pide el estudiante al enviar: obligatoria y mínimo 7 días después del envío. */
@ExtendWith(MockitoExtension.class)
class FechaRevisionPlanificacionTest {
    private static final Long ID_PLANIFICACION = 10L;
    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_PROFESOR = 1L;

    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private LineaPracticaRepository lineaPracticaRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private PlanificacionService service;

    private Planificacion planificacion(EstadoPlanificacion estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(ID_ESTUDIANTE);
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        ProfesorAsignatura profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(ID_PROFESOR);
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, profesor, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(99L); // el colaborador ya no revisa planificaciones
        LineaPractica linea = new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);

        Planificacion p = estado == EstadoPlanificacion.BORRADOR
                ? Planificacion.borrador(linea, null)
                : new Planificacion(linea, null, LocalDateTime.of(2026, 10, 1, 9, 0));
        p.setIdPlanificacion(ID_PLANIFICACION);
        p.setEstado(estado);
        p.adjuntarArchivo("archivo.pdf", "plan.pdf", 1234L, "Texto", LocalDateTime.of(2026, 10, 1, 9, 0));
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    @DisplayName("La fecha mínima es hoy + 7 días")
    void fechaMinima_esHoyMasSieteDias() {
        assertEquals(LocalDate.now().plusDays(7), PlanificacionService.fechaMinimaRevision());
    }

    @Test
    @DisplayName("Enviar con exactamente 7 días de plazo guarda la fecha en la planificación y notifica con ella")
    void enviar_conSieteDias_guardaLaFecha() {
        Planificacion borrador = planificacion(EstadoPlanificacion.BORRADOR);
        LocalDate fecha = LocalDate.now().plusDays(7);

        service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, fecha);

        assertEquals(EstadoPlanificacion.ENVIADA, borrador.getEstado());
        assertEquals(fecha, borrador.getFechaRevisionSolicitada());
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_PROFESOR), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), any());
        assertTrue(mensaje.getValue().contains(fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                mensaje.getValue());
    }

    @Test
    @DisplayName("Enviar con 6 días de plazo se rechaza: sigue borrador, sin fecha y sin notificar")
    void enviar_conSeisDias_seRechaza() {
        Planificacion borrador = planificacion(EstadoPlanificacion.BORRADOR);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, LocalDate.now().plusDays(6)));

        assertTrue(ex.getMessage().contains("7 días"), ex.getMessage());
        assertEquals(EstadoPlanificacion.BORRADOR, borrador.getEstado());
        assertNull(borrador.getFechaRevisionSolicitada());
        verify(planificacionRepository, never()).save(any());
        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Enviar con una fecha pasada se rechaza")
    void enviar_conFechaPasada_seRechaza() {
        planificacion(EstadoPlanificacion.BORRADOR);

        assertThrows(IllegalArgumentException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, LocalDate.now().minusDays(1)));
    }

    @Test
    @DisplayName("Enviar sin fecha se rechaza")
    void enviar_sinFecha_seRechaza() {
        Planificacion borrador = planificacion(EstadoPlanificacion.BORRADOR);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, null));

        assertTrue(ex.getMessage().contains("fecha"), ex.getMessage());
        assertEquals(EstadoPlanificacion.BORRADOR, borrador.getEstado());
        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Reenviar una rechazada exige una fecha nueva válida y reinicia el recordatorio")
    void reenviar_exigeFechaNuevaYReiniciaRecordatorio() {
        Planificacion rechazada = planificacion(EstadoPlanificacion.RECHAZADA);
        rechazada.setFechaRevisionSolicitada(LocalDate.now().minusDays(3));
        rechazada.marcarRecordatorioRevisionEnviado();

        assertThrows(IllegalArgumentException.class,
                () -> service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, LocalDate.now().plusDays(2)));
        assertEquals(EstadoPlanificacion.RECHAZADA, rechazada.getEstado());

        LocalDate nueva = LocalDate.now().plusDays(10);
        service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, nueva);

        assertEquals(EstadoPlanificacion.ENVIADA, rechazada.getEstado());
        assertEquals(nueva, rechazada.getFechaRevisionSolicitada());
        assertEquals(false, rechazada.isRecordatorioRevisionEnviado());
    }
}
