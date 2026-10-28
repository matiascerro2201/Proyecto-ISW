package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.HallazgoChatbot;
import com.grupo1.isw.practicas_pedagogicas.models.InteraccionChatbot;
import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.repositories.CategoriaPlanificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.InteraccionChatbotRepository;
import com.grupo1.isw.practicas_pedagogicas.services.ChatbotService;
import com.grupo1.isw.practicas_pedagogicas.services.ChatbotService.VistaChat;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService.PlanificacionVista;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Hallazgo;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Mensaje;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Respuesta;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Rol;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotClient.Solicitud;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.InstruccionesChatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU-26: servicio del chat. Se prueba con límites pequeños (3 mensajes por día, 50 caracteres,
 * 2 intercambios de historial) para poder recorrer los bordes con pocos datos.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_PLANIFICACION = 10L;
    private static final int MAX_POR_DIA = 3;
    private static final int MAX_CARACTERES = 50;
    private static final int MAX_HISTORIAL = 2;

    private static final CategoriaPlanificacion OBJETIVOS =
            new CategoriaPlanificacion("OBJETIVOS", "Objetivos", "Descripción de objetivos", 1);
    private static final CategoriaPlanificacion EVALUACION =
            new CategoriaPlanificacion("EVALUACION", "Evaluación", "Descripción de evaluación", 2);

    @Mock
    private PlanificacionService planificacionService;

    @Mock
    private InteraccionChatbotRepository interaccionRepository;

    @Mock
    private CategoriaPlanificacionRepository categoriaRepository;

    @Mock
    private ChatbotClient chatbotClient;

    private ChatbotService service;

    @BeforeEach
    void crearServicio() {
        service = new ChatbotService(planificacionService, interaccionRepository, categoriaRepository,
                new InstruccionesChatbot(), chatbotClient, MAX_POR_DIA, MAX_CARACTERES, MAX_HISTORIAL);
    }

    // ------------------------------------------------------------------ ayudas

    private Planificacion borrador() {
        Planificacion borrador = Planificacion.borrador(null, "Texto del borrador");
        borrador.setIdPlanificacion(ID_PLANIFICACION);
        return borrador;
    }

    private Planificacion rechazada(String retroalimentacion, LocalDateTime fechaRetro) {
        Planificacion rechazada = new Planificacion(null, "Texto mejorado", LocalDateTime.of(2026, 10, 1, 9, 0));
        rechazada.setIdPlanificacion(ID_PLANIFICACION);
        rechazada.setEstado(EstadoPlanificacion.RECHAZADA);
        rechazada.setRetroalimentacion(retroalimentacion);
        rechazada.setFechaRetro(fechaRetro);
        return rechazada;
    }

    private Planificacion enEstado(EstadoPlanificacion estado) {
        Planificacion planificacion = new Planificacion(null, "Texto", LocalDateTime.of(2026, 10, 1, 9, 0));
        planificacion.setIdPlanificacion(ID_PLANIFICACION);
        planificacion.setEstado(estado);
        return planificacion;
    }

    private InteraccionChatbot previa(String mensaje, String respuesta) {
        return new InteraccionChatbot(null, mensaje, respuesta, LocalDateTime.of(2026, 10, 2, 10, 0));
    }

    private void dadoQueLaPlanificacionEs(Planificacion planificacion) {
        when(planificacionService.obtenerPlanificacionDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(planificacion);
    }

    private void dadoQueHayAspectos() {
        when(categoriaRepository.findByActivaTrueOrderByOrdenAsc()).thenReturn(List.of(OBJETIVOS, EVALUACION));
    }

    private void dadoQueElAsistenteResponde(Respuesta respuesta) {
        when(chatbotClient.responder(any(Solicitud.class))).thenReturn(respuesta);
    }

    private void dadoQueElHistorialEs(InteraccionChatbot... previas) {
        when(interaccionRepository
                .findByPlanificacion_IdPlanificacionAndFechaGreaterThanEqualOrderByFechaAscIdInteraccionAsc(
                        eq(ID_PLANIFICACION), any(LocalDateTime.class)))
                .thenReturn(List.of(previas));
    }

    private void dadoQueSeUsaronHoy(long mensajes) {
        when(interaccionRepository
                .countByPlanificacion_Linea_Inscripcion_Estudiante_IdUsuarioAndFechaGreaterThanEqual(
                        eq(ID_ESTUDIANTE), any(LocalDateTime.class)))
                .thenReturn(mensajes);
    }

    private void enviar(String mensaje) {
        service.enviarMensaje(ID_ESTUDIANTE, ID_PLANIFICACION, mensaje);
    }

    /** Escenario normal: un borrador, con aspectos configurados y un asistente que responde. */
    private void dadoUnEscenarioNormal(Respuesta respuesta) {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueHayAspectos();
        dadoQueElAsistenteResponde(respuesta);
    }

    private InteraccionChatbot interaccionGuardada() {
        ArgumentCaptor<InteraccionChatbot> guardada = ArgumentCaptor.forClass(InteraccionChatbot.class);
        verify(interaccionRepository).save(guardada.capture());
        return guardada.getValue();
    }

    private Solicitud solicitudEnviada() {
        ArgumentCaptor<Solicitud> solicitud = ArgumentCaptor.forClass(Solicitud.class);
        verify(chatbotClient).responder(solicitud.capture());
        return solicitud.getValue();
    }

    private LocalDateTime fechaConsultadaDelHistorial() {
        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(interaccionRepository)
                .findByPlanificacion_IdPlanificacionAndFechaGreaterThanEqualOrderByFechaAscIdInteraccionAsc(
                        eq(ID_PLANIFICACION), desde.capture());
        return desde.getValue();
    }

    private void verificarQueNoSeLlamoAlAsistenteNiSeGuardo() {
        verifyNoInteractions(chatbotClient);
        verify(interaccionRepository, never()).save(any());
    }

    // ---------------------------------------------------------- camino normal

    @Test
    @DisplayName("HU-26. Un mensaje se envía al asistente y se guarda con su respuesta y sus hallazgos")
    void enviarMensaje_guardaLaInteraccionConSusHallazgos() {
        LocalDateTime antes = LocalDateTime.now();
        dadoUnEscenarioNormal(new Respuesta("  Consejo del asistente  ", List.of(
                new Hallazgo("OBJETIVOS", NivelHallazgo.DEBIL, "No son medibles"),
                new Hallazgo("EVALUACION", NivelHallazgo.ADECUADO, "Bien planteada"))));

        enviar("  Revisa mis objetivos  ");

        InteraccionChatbot guardada = interaccionGuardada();
        assertEquals("Revisa mis objetivos", guardada.getMensajeEstudiante());
        assertEquals("Consejo del asistente", guardada.getRespuestaBot());
        assertFalse(guardada.getFecha().isBefore(antes));
        assertEquals(2, guardada.getHallazgos().size());
        HallazgoChatbot primero = guardada.getHallazgos().get(0);
        assertSame(OBJETIVOS, primero.getCategoria());
        assertEquals(NivelHallazgo.DEBIL, primero.getNivel());
        assertEquals("No son medibles", primero.getObservacion());
        assertSame(guardada, primero.getInteraccion());
    }

    @Test
    @DisplayName("HU-26. La solicitud lleva el mensaje, los códigos de los aspectos y las instrucciones con el texto del borrador")
    void enviarMensaje_armaLaSolicitud() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));

        enviar("Revisa mis objetivos");

        Solicitud solicitud = solicitudEnviada();
        assertEquals("Revisa mis objetivos", solicitud.mensaje());
        assertEquals(List.of("OBJETIVOS", "EVALUACION"), solicitud.categorias());
        assertTrue(solicitud.historial().isEmpty());
        assertTrue(solicitud.instrucciones().contains("Texto del borrador"));
        assertFalse(solicitud.instrucciones().contains("RETROALIMENTACIÓN DEL PROFESOR"));
    }

    // ------------------------------------------------- quién y en qué estado

    @ParameterizedTest(name = "HU-26. Planificación ajena o inexistente: {0}")
    @EnumSource(value = HttpStatus.class, names = {"FORBIDDEN", "NOT_FOUND"})
    void enviarMensaje_planificacionAjenaOInexistente_sePropaga(HttpStatus estado) {
        when(planificacionService.obtenerPlanificacionDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenThrow(new ResponseStatusException(estado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> enviar("Hola"));

        assertEquals(estado.value(), ex.getStatusCode().value());
        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @ParameterizedTest(name = "HU-26. No se puede conversar sobre una planificación {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"ENVIADA", "APROBADA"})
    void enviarMensaje_estadoNoPermitido_lanzaIllegalState(EstadoPlanificacion estado) {
        dadoQueLaPlanificacionEs(enEstado(estado));

        assertThrows(IllegalStateException.class, () -> enviar("Hola"));

        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    // ---------------------------------------------------- mensaje y límites

    @ParameterizedTest(name = "HU-26. Mensaje vacío: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t "})
    void enviarMensaje_mensajeVacio_lanzaIllegalArgument(String vacio) {
        dadoQueLaPlanificacionEs(borrador());

        assertThrows(IllegalArgumentException.class, () -> enviar(vacio));

        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @Test
    @DisplayName("HU-26. Un mensaje de exactamente el máximo se acepta y uno más largo no")
    void enviarMensaje_largoMaximo() {
        String enElLimite = "a".repeat(MAX_CARACTERES);
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));

        enviar(enElLimite);

        assertEquals(enElLimite, interaccionGuardada().getMensajeEstudiante());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> enviar("a".repeat(MAX_CARACTERES + 1)));
        assertTrue(ex.getMessage().contains(String.valueOf(MAX_CARACTERES)));
    }

    @Test
    @DisplayName("HU-26. Los saltos de línea que manda el navegador (CRLF) cuentan como un carácter, igual que en el cuadro")
    void enviarMensaje_saltosDeLineaCrLf_cuentanComoUno() {
        // 24 + CRLF + 23 + CRLF + 1: 52 caracteres tal como llegan, 50 con saltos normales (el máximo del test).
        String conCrLf = "a".repeat(24) + "\r\n" + "b".repeat(23) + "\r\n" + "c";
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));

        enviar(conCrLf);

        assertEquals("a".repeat(24) + "\n" + "b".repeat(23) + "\n" + "c", interaccionGuardada().getMensajeEstudiante());
    }

    @Test
    @DisplayName("HU-26. Con saltos de línea, un carácter más allá del máximo sigue siendo demasiado largo")
    void enviarMensaje_saltosDeLineaCrLf_siSePasaSeRechaza() {
        String sePasa = "a".repeat(24) + "\r\n" + "b".repeat(23) + "\r\n" + "cc";
        dadoQueLaPlanificacionEs(borrador());

        assertThrows(IllegalArgumentException.class, () -> enviar(sePasa));

        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @Test
    @DisplayName("HU-26. Con un mensaje menos que el máximo diario se acepta, y desde la medianoche se cuenta")
    void enviarMensaje_limiteDiario_aunHayCupo() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));
        dadoQueSeUsaronHoy(MAX_POR_DIA - 1);

        enviar("Hola");

        verify(interaccionRepository).save(any(InteraccionChatbot.class));
        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(interaccionRepository)
                .countByPlanificacion_Linea_Inscripcion_Estudiante_IdUsuarioAndFechaGreaterThanEqual(
                        eq(ID_ESTUDIANTE), desde.capture());
        assertEquals(LocalTime.MIDNIGHT, desde.getValue().toLocalTime());
    }

    @Test
    @DisplayName("HU-26. Al llegar al máximo diario lanza IllegalState y no llama al asistente")
    void enviarMensaje_limiteDiarioAlcanzado_lanzaIllegalState() {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueSeUsaronHoy(MAX_POR_DIA);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> enviar("Hola"));

        assertTrue(ex.getMessage().contains(MAX_POR_DIA + " mensajes por día"));
        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @Test
    @DisplayName("HU-26. Orden de las validaciones: el estado se comprueba antes que el mensaje")
    void enviarMensaje_ordenDeLasValidaciones() {
        dadoQueLaPlanificacionEs(enEstado(EstadoPlanificacion.APROBADA));
        assertThrows(IllegalStateException.class, () -> enviar(""));
        verifyNoInteractions(interaccionRepository);
    }

    @Test
    @DisplayName("HU-26. Un mensaje vacío se rechaza sin consultar siquiera el límite diario")
    void enviarMensaje_mensajeVacio_noConsultaElLimite() {
        dadoQueLaPlanificacionEs(borrador());

        assertThrows(IllegalArgumentException.class, () -> enviar(""));

        verifyNoInteractions(interaccionRepository);
    }

    // ------------------------------------------- fallos del asistente

    @ParameterizedTest(name = "HU-26. Si el asistente falla por {0}, se propaga y no se guarda nada")
    @EnumSource(Causa.class)
    void enviarMensaje_falloDelAsistente_noGuardaNada(Causa causa) {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueHayAspectos();
        when(chatbotClient.responder(any(Solicitud.class))).thenThrow(new ChatbotNoDisponibleException(causa, "x"));

        ChatbotNoDisponibleException ex = assertThrows(ChatbotNoDisponibleException.class, () -> enviar("Hola"));

        assertEquals(causa, ex.getCausa());
        verify(interaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-26. Un error inesperado del cliente se envuelve como ERROR y no se guarda nada")
    void enviarMensaje_errorInesperado_seEnvuelve() {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueHayAspectos();
        when(chatbotClient.responder(any(Solicitud.class))).thenThrow(new IllegalArgumentException("fallo interno"));

        ChatbotNoDisponibleException ex = assertThrows(ChatbotNoDisponibleException.class, () -> enviar("Hola"));

        assertEquals(Causa.ERROR, ex.getCausa());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        verify(interaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-26. Una respuesta en blanco cuenta como fallo y no se guarda")
    void enviarMensaje_respuestaEnBlanco_noSeGuarda() {
        dadoUnEscenarioNormal(new Respuesta("   ", List.of()));

        ChatbotNoDisponibleException ex = assertThrows(ChatbotNoDisponibleException.class, () -> enviar("Hola"));

        assertEquals(Causa.ERROR, ex.getCausa());
        verify(interaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-26. Sin aspectos configurados no se llama al asistente")
    void enviarMensaje_sinAspectos_lanzaErrorSinLlamarAlAsistente() {
        dadoQueLaPlanificacionEs(borrador());
        when(categoriaRepository.findByActivaTrueOrderByOrdenAsc()).thenReturn(List.of());

        ChatbotNoDisponibleException ex = assertThrows(ChatbotNoDisponibleException.class, () -> enviar("Hola"));

        assertEquals(Causa.ERROR, ex.getCausa());
        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    // ------------------------------------------------------------ historial

    @Test
    @DisplayName("HU-26. En un borrador se consulta toda la conversación y solo viajan los últimos intercambios")
    void enviarMensaje_historialRecortado() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));
        dadoQueElHistorialEs(previa("m1", "r1"), previa("m2", "r2"), previa("m3", "r3"));

        enviar("nueva");

        assertEquals(ChatbotService.DESDE_SIEMPRE, fechaConsultadaDelHistorial());
        List<Mensaje> historial = solicitudEnviada().historial();
        assertEquals(4, historial.size());
        assertEquals("m2", historial.get(0).contenido());
        assertEquals(Rol.ESTUDIANTE, historial.get(0).rol());
        assertEquals("r2", historial.get(1).contenido());
        assertEquals(Rol.ASISTENTE, historial.get(1).rol());
        assertEquals("m3", historial.get(2).contenido());
        assertEquals("r3", historial.get(3).contenido());
    }

    @Test
    @DisplayName("HU-26. En una rechazada se consulta desde el rechazo y las instrucciones llevan la retroalimentación")
    void enviarMensaje_rechazada_desdeElRechazo() {
        LocalDateTime rechazo = LocalDateTime.of(2026, 10, 3, 12, 0);
        dadoQueLaPlanificacionEs(rechazada("Faltan objetivos medibles.", rechazo));
        dadoQueHayAspectos();
        dadoQueElAsistenteResponde(new Respuesta("Consejo", List.of()));

        enviar("Sigo con la mejora");

        assertEquals(rechazo, fechaConsultadaDelHistorial());
        String instruccionesEnviadas = solicitudEnviada().instrucciones();
        assertTrue(instruccionesEnviadas.contains("Faltan objetivos medibles."));
        assertTrue(instruccionesEnviadas.contains("Texto mejorado"));
    }

    @Test
    @DisplayName("HU-26. Una rechazada sin fecha de rechazo consulta toda la conversación")
    void enviarMensaje_rechazadaSinFecha_desdeSiempre() {
        dadoQueLaPlanificacionEs(rechazada("Comentario", null));
        dadoQueHayAspectos();
        dadoQueElAsistenteResponde(new Respuesta("Consejo", List.of()));

        enviar("Hola");

        assertEquals(ChatbotService.DESDE_SIEMPRE, fechaConsultadaDelHistorial());
    }

    // -------------------------------------------- valoraciones del asistente

    @Test
    @DisplayName("HU-26. Se ignoran los aspectos desconocidos y repetidos; se reconocen mayúsculas y espacios")
    void enviarMensaje_hallazgosDesconocidosYRepetidos() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of(
                new Hallazgo("objetivos", NivelHallazgo.DEBIL, "  con espacios  "),
                new Hallazgo("OBJETIVOS", NivelHallazgo.ADECUADO, "repetido: se ignora"),
                new Hallazgo("INVENTADA", NivelHallazgo.DEBIL, "aspecto que no existe"),
                new Hallazgo(" EVALUACION ", NivelHallazgo.ADECUADO, "Bien"))));

        enviar("Hola");

        List<HallazgoChatbot> hallazgos = interaccionGuardada().getHallazgos();
        assertEquals(2, hallazgos.size());
        assertSame(OBJETIVOS, hallazgos.get(0).getCategoria());
        assertEquals(NivelHallazgo.DEBIL, hallazgos.get(0).getNivel());
        assertEquals("con espacios", hallazgos.get(0).getObservacion());
        assertSame(EVALUACION, hallazgos.get(1).getCategoria());
    }

    @Test
    @DisplayName("HU-26. Una observación se recorta al tamaño de la columna y una en blanco se guarda como null")
    void enviarMensaje_observaciones() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of(
                new Hallazgo("OBJETIVOS", NivelHallazgo.DEBIL, "x".repeat(400)),
                new Hallazgo("EVALUACION", NivelHallazgo.DEBIL, "   "))));

        enviar("Hola");

        List<HallazgoChatbot> hallazgos = interaccionGuardada().getHallazgos();
        assertEquals(ChatbotService.MAX_CARACTERES_OBSERVACION, hallazgos.get(0).getObservacion().length());
        assertNull(hallazgos.get(1).getObservacion());
    }

    @Test
    @DisplayName("HU-26. Si el asistente no entrega hallazgos se guarda igual la respuesta, recortada si es enorme")
    void enviarMensaje_sinHallazgos_seGuardaLaRespuesta() {
        dadoUnEscenarioNormal(new Respuesta("y".repeat(ChatbotService.MAX_CARACTERES_RESPUESTA + 50), List.of()));

        enviar("Hola");

        InteraccionChatbot guardada = interaccionGuardada();
        assertTrue(guardada.getHallazgos().isEmpty());
        assertEquals(ChatbotService.MAX_CARACTERES_RESPUESTA, guardada.getRespuestaBot().length());
    }

    // --------------------------------------------------------------- pantalla

    @Test
    @DisplayName("HU-26. La conversación de un borrador trae los turnos en orden y los mensajes que quedan hoy")
    void obtenerConversacion_borrador() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(new PlanificacionVista(ID_PLANIFICACION, "Práctica (ESPECIALIDAD)", "Plan actual",
                        EstadoPlanificacion.BORRADOR, null, null, null));
        dadoQueElHistorialEs(previa("m1", "r1"), previa("m2", "r2"));
        dadoQueSeUsaronHoy(1);

        VistaChat vista = service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertEquals(2, vista.turnos().size());
        assertEquals("m1", vista.turnos().get(0).mensajeEstudiante());
        assertEquals("r2", vista.turnos().get(1).respuestaBot());
        assertFalse(vista.rechazada());
        assertEquals(MAX_POR_DIA - 1, vista.mensajesRestantesHoy());
        assertEquals(MAX_CARACTERES, vista.maxCaracteresMensaje());
        assertEquals("Práctica (ESPECIALIDAD)", vista.etiquetaLinea());
        assertEquals("Plan actual", vista.contenido());
        assertEquals(ChatbotService.DESDE_SIEMPRE, fechaConsultadaDelHistorial());
    }

    @Test
    @DisplayName("HU-26. Pasado el límite diario los mensajes restantes son 0, nunca negativos")
    void obtenerConversacion_restantesNoSonNegativos() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(new PlanificacionVista(ID_PLANIFICACION, "L", "Plan", EstadoPlanificacion.BORRADOR,
                        null, null, null));
        dadoQueSeUsaronHoy(MAX_POR_DIA + 4);

        assertEquals(0, service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION).mensajesRestantesHoy());
    }

    @Test
    @DisplayName("HU-26. La conversación de una rechazada es solo la posterior al rechazo e incluye la retroalimentación")
    void obtenerConversacion_rechazada() {
        LocalDateTime rechazo = LocalDateTime.of(2026, 10, 3, 12, 0);
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(new PlanificacionVista(ID_PLANIFICACION, "L", "Plan", EstadoPlanificacion.RECHAZADA,
                        "Falta la evaluación", rechazo.minusDays(1), rechazo));
        dadoQueElHistorialEs(previa("actual", "r"));

        VistaChat vista = service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertTrue(vista.rechazada());
        assertEquals("Falta la evaluación", vista.retroalimentacion());
        assertEquals(1, vista.turnos().size());
        assertEquals(rechazo, fechaConsultadaDelHistorial());
    }

    @ParameterizedTest(name = "HU-26. La pantalla del chat de una planificación {0} lanza IllegalState")
    @EnumSource(value = EstadoPlanificacion.class, names = {"ENVIADA", "APROBADA"})
    void obtenerConversacion_estadoNoPermitido_lanzaIllegalState(EstadoPlanificacion estado) {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(new PlanificacionVista(ID_PLANIFICACION, "L", "Plan", estado,
                        null, LocalDateTime.of(2026, 10, 1, 9, 0), null));

        assertThrows(IllegalStateException.class,
                () -> service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION));

        verifyNoInteractions(interaccionRepository);
    }
}
