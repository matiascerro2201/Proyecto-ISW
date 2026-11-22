package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorColaborador;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.AlertaRevisionPlanificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaRevisionPlanificacionServiceTest {
    private static final Long ID_COLABORADOR = 1L;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private AlertaRevisionPlanificacionService service;

    private Planificacion planificacion(LocalDate fechaRevision, boolean conColaborador) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, null, 2026, 2), null);
        ProfesorColaborador colaborador = null;
        if (conColaborador) {
            colaborador = new ProfesorColaborador();
            colaborador.setIdUsuario(ID_COLABORADOR);
        }
        Planificacion p = new Planificacion(new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD),
                null, LocalDateTime.of(2026, 11, 10, 9, 0));
        p.setIdPlanificacion(33L);
        p.setFechaRevisionSolicitada(fechaRevision);
        return p;
    }

    private void dadoQueLaBusquedaDevuelve(Planificacion... planificaciones) {
        when(planificacionRepository
                .findByEstadoAndRecordatorioRevisionEnviadoFalseAndFechaRevisionSolicitadaBetween(
                        EstadoPlanificacion.ENVIADA, HOY, HOY.plusDays(2)))
                .thenReturn(List.of(planificaciones));
    }

    @Test
    @DisplayName("Cuando faltan 2 días notifica al colaborador con enlace a la evaluación y marca el aviso")
    void faltanDosDias_notificaAlColaborador() {
        Planificacion p = planificacion(HOY.plusDays(2), true);
        dadoQueLaBusquedaDevuelve(p);

        int avisos = service.avisarRevisionesProximas(HOY);

        assertEquals(1, avisos);
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_COLABORADOR), eq(NotificacionService.TIPO_REVISION_PROXIMA),
                mensaje.capture(), eq("/planificaciones/33/evaluar"));
        assertTrue(mensaje.getValue().contains("Camila Soto"), mensaje.getValue());
        assertTrue(mensaje.getValue().contains("25/11/2026"), mensaje.getValue());
        assertTrue(p.isRecordatorioRevisionEnviado());
        verify(planificacionRepository).save(p);
    }

    @Test
    @DisplayName("Si no hay planificaciones dentro de la ventana no se notifica a nadie")
    void sinPlanificaciones_noNotifica() {
        dadoQueLaBusquedaDevuelve();

        assertEquals(0, service.avisarRevisionesProximas(HOY));

        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Una línea sin colaborador se omite y no queda marcada como avisada")
    void sinColaborador_seOmite() {
        Planificacion p = planificacion(HOY.plusDays(2), false);
        dadoQueLaBusquedaDevuelve(p);

        assertEquals(0, service.avisarRevisionesProximas(HOY));

        verify(notificacionService, never()).crear(any(), any(), any(), any());
        assertEquals(false, p.isRecordatorioRevisionEnviado());
    }
}
