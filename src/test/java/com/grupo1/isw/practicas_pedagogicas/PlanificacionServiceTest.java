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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PlanificacionServiceTest {

    private static final Long ID_PLANIFICACION = 10L;
    private static final Long ID_PROFESOR = 1L;
    private static final Long ID_OTRO_PROFESOR = 2L;
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

    private void dadoQueExisteYPerteneceA(Long idProfesorDeLaSeccion) {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(planificacion));
        when(planificacion.getLinea().getInscripcion().getSeccion().getProfesor().getIdUsuario())
                .thenReturn(idProfesorDeLaSeccion);
    }

    private void dadoQueEsDelProfesorYEstaEn(EstadoPlanificacion estadoActual) {
        dadoQueExisteYPerteneceA(ID_PROFESOR);
        when(planificacion.getEstado()).thenReturn(estadoActual);
    }

    private void verificarQueNoSeGuardoNada() {
        verify(planificacionRepository, never()).save(planificacion);
        verify(planificacion, never()).setEstado(any());
        verify(planificacion, never()).setFechaRetro(any());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("1. Lanza 404 si la planificación no existe")
    void noExiste_lanza404() {
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
                        "Comentario", EstadoPlanificacion.APROBADA));

        assertEquals(404, ex.getStatusCode().value());
        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("2. Lanza 403 si quien evalúa no es el profesor de la asignatura de la sección")
    void profesorAjeno_lanza403() {
        dadoQueExisteYPerteneceA(ID_OTRO_PROFESOR);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
                        "Comentario", EstadoPlanificacion.APROBADA));

        assertEquals(403, ex.getStatusCode().value());
        verificarQueNoSeGuardoNada();
    }

    @ParameterizedTest(name = "3. Decisión inválida: {0}")
    @NullSource
    @EnumSource(value = EstadoPlanificacion.class, names = {"BORRADOR", "ENVIADA"})
    void decisionInvalida_lanzaIllegalArgument(EstadoPlanificacion decisionInvalida) {
        dadoQueExisteYPerteneceA(ID_PROFESOR);

        assertThrows(IllegalArgumentException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
                        "Comentario", decisionInvalida));

        verificarQueNoSeGuardoNada();
    }


    @ParameterizedTest(name = "4. Ya estaba {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"APROBADA", "RECHAZADA"})
    void noEnviada_lanzaIllegalState(EstadoPlanificacion estadoActual) {
        dadoQueEsDelProfesorYEstaEn(estadoActual);

        assertThrows(IllegalStateException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
                        "Comentario", EstadoPlanificacion.APROBADA));

        verificarQueNoSeGuardoNada();
    }

    @ParameterizedTest(name = "5. Rechazo con texto vacío: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void rechazoSinTexto_lanzaIllegalArgument(String retroalimentacionVacia) {
        dadoQueEsDelProfesorYEstaEn(EstadoPlanificacion.ENVIADA);

        assertThrows(IllegalArgumentException.class,
                () -> service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
                        retroalimentacionVacia, EstadoPlanificacion.RECHAZADA));

        verificarQueNoSeGuardoNada();
    }

    @Test
    @DisplayName("6. Aprueba: guarda estado, retroalimentación (sin espacios sobrantes) y fecha")
    void aprobar_exito() {
        dadoQueEsDelProfesorYEstaEn(EstadoPlanificacion.ENVIADA);
        LocalDateTime antes = LocalDateTime.now();

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
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
        dadoQueEsDelProfesorYEstaEn(EstadoPlanificacion.ENVIADA);
        LocalDateTime antes = LocalDateTime.now();

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
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
        dadoQueEsDelProfesorYEstaEn(EstadoPlanificacion.ENVIADA);

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR,
                retroalimentacionVacia, EstadoPlanificacion.APROBADA);

        verify(planificacion).setRetroalimentacion(null);
        verify(planificacion).setEstado(EstadoPlanificacion.APROBADA);
        verify(planificacionRepository).save(planificacion);
    }

    private LineaPractica lineaDe(Long idEstudiante, Long idProfesor) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(idEstudiante);
        estudiante.setNombre("Camila Soto");
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Práctica Pedagógica");
        ProfesorAsignatura profesor = new ProfesorAsignatura();
        profesor.setIdUsuario(idProfesor);
        InscripcionAsignatura inscripcion =
                new InscripcionAsignatura(estudiante, new SeccionAsignatura(asignatura, profesor, 2026, 2), null);
        ProfesorColaborador colaborador = new ProfesorColaborador();
        colaborador.setIdUsuario(99L); // el colaborador ya no revisa planificaciones
        return new LineaPractica(inscripcion, colaborador, null, TipoLinea.ESPECIALIDAD);
    }

    private Planificacion conIdNuevo(Planificacion guardada) {
        guardada.setIdPlanificacion(ID_PLANIFICACION_NUEVA);
        return guardada;
    }

    private void verificarQueNoSeNotifico() {
        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    private Planificacion conArchivo(Planificacion planificacion) {
        planificacion.adjuntarArchivo("archivo.pdf", "plan.pdf", 1234L, "Texto extraído del PDF",
                LocalDateTime.now().minusDays(1));
        return planificacion;
    }

    @Test
    @DisplayName("HU-22. Al enviar un borrador con PDF se notifica al profesor de la asignatura")
    void enviar_notificaAlProfesorDeLaAsignatura() {
        Planificacion borrador = conArchivo(Planificacion.borrador(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), null));
        borrador.setIdPlanificacion(77L);
        when(planificacionRepository.findById(77L)).thenReturn(Optional.of(borrador));

        service.enviarBorrador(ID_ESTUDIANTE, 77L, PlanificacionService.fechaMinimaRevision());

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_PROFESOR), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/planificaciones/77/evaluar"));
        assertTrue(mensaje.getValue().contains("Camila Soto envió"), mensaje.getValue());
    }

    @Test
    @DisplayName("HU-22. Si el borrador es de otro estudiante (403) no se guarda ni se notifica")
    void enviar_borradorAjeno_noNotifica() {
        Planificacion ajeno = conArchivo(Planificacion.borrador(lineaDe(ID_OTRO_ESTUDIANTE, ID_PROFESOR), null));
        ajeno.setIdPlanificacion(ID_PLANIFICACION);
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(ajeno));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertEquals(403, ex.getStatusCode().value());
        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-22. Si falla el guardado no se crea la notificación")
    void enviar_falloAlGuardar_noNotifica() {
        Planificacion borrador = conArchivo(Planificacion.borrador(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), null));
        borrador.setIdPlanificacion(ID_PLANIFICACION);
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(borrador));
        when(planificacionRepository.save(any(Planificacion.class)))
                .thenThrow(new IllegalStateException("Falla de base de datos"));

        assertThrows(IllegalStateException.class,
                () -> service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-22. Al reenviar una planificación rechazada se notifica al profesor de la asignatura")
    void reenviar_notificaAlProfesorDeLaAsignatura() {
        Planificacion rechazada = conArchivo(new Planificacion(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), null,
                LocalDateTime.now().minusDays(2)));
        rechazada.setIdPlanificacion(ID_PLANIFICACION);
        rechazada.setEstado(EstadoPlanificacion.RECHAZADA);
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(rechazada));

        service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision());

        assertEquals(EstadoPlanificacion.ENVIADA, rechazada.getEstado());
        verify(planificacionRepository).save(rechazada);
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_PROFESOR), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/planificaciones/10/evaluar"));
        assertTrue(mensaje.getValue().contains("Camila Soto reenvió"), mensaje.getValue());
    }

    @Test
    @DisplayName("HU-31/32. Reenviar una rechazada sin PDF lanza IllegalArgument: sigue rechazada y no se notifica")
    void reenviar_sinArchivo_siguesiendoRechazada() {
        Planificacion rechazada = new Planificacion(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), "Plan antiguo de texto",
                LocalDateTime.now().minusDays(2));
        rechazada.setIdPlanificacion(ID_PLANIFICACION);
        rechazada.setEstado(EstadoPlanificacion.RECHAZADA);
        rechazada.setRetroalimentacion("Faltan objetivos");
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(rechazada));

        assertThrows(IllegalArgumentException.class,
                () -> service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        assertEquals(EstadoPlanificacion.RECHAZADA, rechazada.getEstado());
        assertEquals("Faltan objetivos", rechazada.getRetroalimentacion());
        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-22. Reenviar una planificación que no está rechazada no notifica")
    void reenviar_noRechazada_noNotifica() {
        Planificacion aprobada = new Planificacion(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), "Plan",
                LocalDateTime.now().minusDays(2));
        aprobada.setIdPlanificacion(ID_PLANIFICACION);
        aprobada.setEstado(EstadoPlanificacion.APROBADA);
        when(planificacionRepository.findById(ID_PLANIFICACION)).thenReturn(Optional.of(aprobada));

        assertThrows(IllegalStateException.class,
                () -> service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION, PlanificacionService.fechaMinimaRevision()));

        verify(planificacionRepository, never()).save(any());
        verificarQueNoSeNotifico();
    }

    private Planificacion enviadaDe(Long idEstudiante, Long idProfesor) {
        Planificacion enviada = new Planificacion(lineaDe(idEstudiante, idProfesor), "Plan de clase",
                LocalDateTime.now().minusDays(1));
        enviada.setIdPlanificacion(ID_PLANIFICACION);
        return enviada;
    }

    @Test
    @DisplayName("HU-23. Al aprobar se notifica al estudiante sin copiar la retroalimentación")
    void aprobar_notificaAlEstudiante() {
        when(planificacionRepository.findById(ID_PLANIFICACION))
                .thenReturn(Optional.of(enviadaDe(ID_ESTUDIANTE, ID_PROFESOR)));

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR, "Muy buen trabajo", EstadoPlanificacion.APROBADA);

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_ESTUDIANTE), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/estudiante/planificaciones"));
        assertTrue(mensaje.getValue().contains("aprobada"), mensaje.getValue());
        assertFalse(mensaje.getValue().contains("Muy buen trabajo"), "no debe copiar la retroalimentación");
    }

    @Test
    @DisplayName("HU-23. Al rechazar se notifica al estudiante sin copiar la retroalimentación")
    void rechazar_notificaAlEstudiante() {
        when(planificacionRepository.findById(ID_PLANIFICACION))
                .thenReturn(Optional.of(enviadaDe(ID_ESTUDIANTE, ID_PROFESOR)));

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR, "Faltan los objetivos", EstadoPlanificacion.RECHAZADA);

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(eq(ID_ESTUDIANTE), eq(NotificacionService.TIPO_PLANIFICACION),
                mensaje.capture(), eq("/estudiante/planificaciones"));
        assertTrue(mensaje.getValue().contains("rechazada"), mensaje.getValue());
        assertFalse(mensaje.getValue().contains("Faltan los objetivos"), "no debe copiar la retroalimentación");
    }

    @Test
    @DisplayName("HU-23. Si falla el guardado de la evaluación no se notifica al estudiante")
    void evaluar_falloAlGuardar_noNotifica() {
        when(planificacionRepository.findById(ID_PLANIFICACION))
                .thenReturn(Optional.of(enviadaDe(ID_ESTUDIANTE, ID_PROFESOR)));
        when(planificacionRepository.save(any(Planificacion.class)))
                .thenThrow(new IllegalStateException("Falla de base de datos"));

        assertThrows(IllegalStateException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_PROFESOR, "Comentario", EstadoPlanificacion.APROBADA));

        verificarQueNoSeNotifico();
    }

    @Test
    @DisplayName("HU-23. Evaluar dos veces (dos pestañas) deja una sola notificación")
    void evaluarDosVeces_notificaUnaSolaVez() {
        when(planificacionRepository.findById(ID_PLANIFICACION))
                .thenReturn(Optional.of(enviadaDe(ID_ESTUDIANTE, ID_PROFESOR)));

        service.evaluarPlanificacion(ID_PLANIFICACION, ID_PROFESOR, "Bien", EstadoPlanificacion.APROBADA);
        assertThrows(IllegalStateException.class, () -> service.evaluarPlanificacion(
                ID_PLANIFICACION, ID_PROFESOR, "Bien", EstadoPlanificacion.APROBADA));

        verify(notificacionService).crear(any(), any(), any(), any());
    }

    @Test
    @DisplayName("HU-22/23. Flujo completo: enviar, rechazar y reenviar notifican al destinatario correcto")
    void flujoCompleto_notificaEnCadaPaso() {
        Planificacion borrador = conArchivo(Planificacion.borrador(lineaDe(ID_ESTUDIANTE, ID_PROFESOR), null));
        borrador.setIdPlanificacion(ID_PLANIFICACION_NUEVA);
        when(planificacionRepository.findById(ID_PLANIFICACION_NUEVA)).thenReturn(Optional.of(borrador));

        service.enviarBorrador(ID_ESTUDIANTE, ID_PLANIFICACION_NUEVA, PlanificacionService.fechaMinimaRevision());
        service.evaluarPlanificacion(ID_PLANIFICACION_NUEVA, ID_PROFESOR, "Faltan objetivos",
                EstadoPlanificacion.RECHAZADA);
        service.reenviarPlanificacion(ID_ESTUDIANTE, ID_PLANIFICACION_NUEVA, PlanificacionService.fechaMinimaRevision());

        ArgumentCaptor<Long> destinatarios = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> enlaces = ArgumentCaptor.forClass(String.class);
        verify(notificacionService, times(3)).crear(destinatarios.capture(), any(), any(), enlaces.capture());
        assertEquals(List.of(ID_PROFESOR, ID_ESTUDIANTE, ID_PROFESOR), destinatarios.getAllValues());
        assertEquals(List.of("/planificaciones/77/evaluar", "/estudiante/planificaciones",
                "/planificaciones/77/evaluar"), enlaces.getAllValues());
    }
}
