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
import com.grupo1.isw.practicas_pedagogicas.repositories.LineaPracticaRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.PlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PlanificacionServiceTest {

    private static final Long ID_PLANIFICACION = 10L;
    private static final Long ID_COLABORADOR = 1L;
    private static final Long ID_OTRO_COLABORADOR = 2L;
    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_OTRO_ESTUDIANTE = 5L;
    private static final Long ID_LINEA = 7L;
    private static final Long ID_PLANIFICACION_NUEVA = 77L;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private LineaPracticaRepository lineaPracticaRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Planificacion planificacion;

    @InjectMocks
    private PlanificacionService service;

    private void dadoQueExisteYPerteneceA(Long idColaboradorDeLaLinea) {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(planificacion));
        when(planificacion.getLinea().getColaborador().getIdUsuario()).thenReturn(idColaboradorDeLaLinea);
    }

    private void dadoQueEsDelColaboradorYEstaEn(EstadoPlanificacion estadoActual) {
        dadoQueExisteYPerteneceA(ID_COLABORADOR);
        when(planificacion.getEstado()).thenReturn(estadoActual);
    }

    private void verificarQueNoSeGuardoNada() {
        verify(planificacionRepository, never()).save(planificacion);
        verify(planificacion, never()).setEstado(any());
        verify(planificacion, never()).setFechaRetro(any());
    }

    @Test
    @DisplayName("1. Lanza 404 si la planificación no existe")
    void noExiste_lanza404() {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                        "Comentario", EstadoPlanificacion.APROBADA));

        assertEquals(404, ex.getStatusCode().value());
        verify(planificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. Lanza 403 si quien evalúa no es el colaborador de la línea")
    void colaboradorAjeno_lanza403() {
        dadoQueExisteYPerteneceA(ID_OTRO_COLABORADOR);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                        "Comentario", EstadoPlanificacion.APROBADA));

        assertEquals(403, ex.getStatusCode().value());
        verificarQueNoSeGuardoNada();
    }

    @ParameterizedTest(name = "3. Decisión inválida: {0}")
    @NullSource
    @EnumSource(value = EstadoPlanificacion.class, names = "ENVIADA")
    void decisionInvalida_lanzaIllegalArgument(EstadoPlanificacion decisionInvalida) {
        dadoQueExisteYPerteneceA(ID_COLABORADOR);

        assertThrows(IllegalArgumentException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                        "Comentario", decisionInvalida));

        verificarQueNoSeGuardoNada();
    }


    @ParameterizedTest(name = "4. Ya estaba {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"APROBADA", "RECHAZADA"})
    void noEnviada_lanzaIllegalState(EstadoPlanificacion estadoActual) {
        dadoQueEsDelColaboradorYEstaEn(estadoActual);

        assertThrows(IllegalStateException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                        "Comentario", EstadoPlanificacion.APROBADA));

        verificarQueNoSeGuardoNada();
    }

    @ParameterizedTest(name = "5. Rechazo con texto vacío: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void rechazoSinTexto_lanzaIllegalArgument(String retroalimentacionVacia) {
        dadoQueEsDelColaboradorYEstaEn(EstadoPlanificacion.ENVIADA);

        assertThrows(IllegalArgumentException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                        retroalimentacionVacia, EstadoPlanificacion.RECHAZADA));

        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("6. Aprueba: guarda estado, retroalimentación (sin espacios sobrantes) y fecha")
    void aprobar_exito() {
        dadoQueEsDelColaboradorYEstaEn(EstadoPlanificacion.ENVIADA);
        LocalDateTime antes = LocalDateTime.now();

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                "  Buen trabajo, la planificación es clara.  ", EstadoPlanificacion.APROBADA);

        verify(planificacion).setEstado(EstadoPlanificacion.APROBADA);
        verify(planificacion).setRetroalimentacion("Buen trabajo, la planificación es clara.");

        ArgumentCaptor<LocalDateTime> fecha = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planificacion).setFechaRetro(fecha.capture());
        assertFalse(fecha.getValue().isBefore(antes), "fechaRetro debe ser la del momento de la evaluación");

        verify(planificacionRepository).save(planificacion);
    }

    @Test
    @DisplayName("7. Rechaza: guarda estado, retroalimentación y fecha")
    void rechazar_exito() {
        dadoQueEsDelColaboradorYEstaEn(EstadoPlanificacion.ENVIADA);
        LocalDateTime antes = LocalDateTime.now();

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                "Faltan los objetivos de aprendizaje.", EstadoPlanificacion.RECHAZADA);

        verify(planificacion).setEstado(EstadoPlanificacion.RECHAZADA);
        verify(planificacion).setRetroalimentacion("Faltan los objetivos de aprendizaje.");

        ArgumentCaptor<LocalDateTime> fecha = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planificacion).setFechaRetro(fecha.capture());
        assertFalse(fecha.getValue().isBefore(antes), "fechaRetro debe ser la del momento de la evaluación");

        verify(planificacionRepository).save(planificacion);
    }
    
    @ParameterizedTest(name = "Extra. Aprobar con comentario vacío [{0}] guarda NULL")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void aprobarSinComentario_guardaNull(String retroalimentacionVacia) {
        dadoQueEsDelColaboradorYEstaEn(EstadoPlanificacion.ENVIADA);

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_COLABORADOR,
                retroalimentacionVacia, EstadoPlanificacion.APROBADA);

        verify(planificacion).setRetroalimentacion(null);
        verify(planificacion).setEstado(EstadoPlanificacion.APROBADA);
        verify(planificacionRepository).save(planificacion);
    }

    private LineaPractica lineaDe(Long idEstudiante, Long idColaborador) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(idEstudiante);
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, null, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(idColaborador);
        return new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);
    }

    private Planificacion conIdNuevo(Planificacion guardada) {
        guardada.setIdPlanificacion(ID_PLANIFICACION_NUEVA);
        return guardada;
    }

    private void verificarQueNoSeNotifico() {
        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("HU-22. Al crear una planificación se notifica al colaborador de la línea")
    void crear_notificaAlColaboradorDeLaLinea() {
        when(lineaPracticaRepository.findById(ID_LINEA))
                .thenReturn(Optional.of(lineaDe(ID_ESTUDIANTE, ID_COLABORADOR)));
        when(planificacionRepository.save(any(Planificacion.class)))
                .thenAnswer(invocacion -> conIdNuevo(invocacion.getArgument(0)));

        service.crearPlanificacion(ID_ESTUDIANTE, ID_LINEA, "Plan de clase");

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_COLABORADOR), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/planificaciones/77/evaluar"));
        assertTrue(mensaje.getValue().contains("Camila Soto envió"), mensaje.getValue());
    }

    @Test
    @DisplayName("HU-22. Si la línea es de otro estudiante (403) no se guarda ni se notifica")
    void crear_lineaAjena_noNotifica() {
        when(lineaPracticaRepository.findById(ID_LINEA))
                .thenReturn(Optional.of(lineaDe(ID_OTRO_ESTUDIANTE, ID_COLABORADOR)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crearPlanificacion(ID_ESTUDIANTE, ID_LINEA, "Plan de clase"));

        assertEquals(403, ex.getStatusCode().value());
        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-22. Con contenido vacío no se guarda ni se notifica")
    void crear_contenidoVacio_noNotifica() {
        when(lineaPracticaRepository.findById(ID_LINEA))
                .thenReturn(Optional.of(lineaDe(ID_ESTUDIANTE, ID_COLABORADOR)));

        assertThrows(IllegalArgumentException.class,
                () -> service.crearPlanificacion(ID_ESTUDIANTE, ID_LINEA, "   "));

        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-22. Si falla el guardado no se crea la notificación")
    void crear_falloAlGuardar_noNotifica() {
        when(lineaPracticaRepository.findById(ID_LINEA))
                .thenReturn(Optional.of(lineaDe(ID_ESTUDIANTE, ID_COLABORADOR)));
        when(planificacionRepository.save(any(Planificacion.class)))
                .thenThrow(new IllegalStateException("Falla de base de datos"));

        assertThrows(IllegalStateException.class,
                () -> service.crearPlanificacion(ID_ESTUDIANTE, ID_LINEA, "Plan de clase"));

        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-22. Al reenviar una planificación rechazada se notifica al colaborador de la línea")
    void reenviar_notificaAlColaboradorDeLaLinea() {
        Planificacion rechazada = new Planificacion(lineaDe(ID_ESTUDIANTE, ID_COLABORADOR), "Plan viejo",
                LocalDateTime.now().minusDays(2));
        rechazada.setIdPlanificacion(ID_PLANIFICACION);
        rechazada.setEstado(EstadoPlanificacion.RECHAZADA);
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(rechazada));

        service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, "Plan corregido");

        assertEquals(EstadoPlanificacion.ENVIADA, rechazada.getEstado());
        verify(planificacionRepository).save(rechazada);
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_COLABORADOR), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/planificaciones/10/evaluar"));
        assertTrue(mensaje.getValue().contains("Camila Soto reenvió"), mensaje.getValue());
    }

    @Test
    @DisplayName("HU-22. Reenviar una planificación que no está rechazada no notifica")
    void reenviar_noRechazada_noNotifica() {
        Planificacion aprobada = new Planificacion(lineaDe(ID_ESTUDIANTE, ID_COLABORADOR), "Plan",
                LocalDateTime.now().minusDays(2));
        aprobada.setIdPlanificacion(ID_PLANIFICACION);
        aprobada.setEstado(EstadoPlanificacion.APROBADA);
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(aprobada));

        assertThrows(IllegalStateException.class,
                () -> service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, "Plan corregido"));

        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }
}
