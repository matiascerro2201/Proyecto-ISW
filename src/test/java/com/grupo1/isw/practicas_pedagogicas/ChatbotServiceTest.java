package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.HallazgoChatbot;
import com.grupo1.isw.practicas_pedagogicas.models.InteraccionChatbot;
import com.grupo1.isw.practicas_pedagogicas.models.NivelHallazgo;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.TipoInteraccion;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU-26 y HU-30: servicio del chat. Se prueba con límites pequeños (3 mensajes por día, 50 caracteres,
 * 2 intercambios de historial, 2 revisiones por planificación) para poder recorrer los bordes con pocos datos.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_PLANIFICACION = 10L;
    private static final int MAX_POR_DIA = 3;
    private static final int MAX_CARACTERES = 50;
    private static final int MAX_HISTORIAL = 2;
    private static final int MAX_REVISIONES = 2;

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
        service = servicioCon(MAX_REVISIONES);
    }

    private ChatbotService servicioCon(int maxRevisiones) {
        return new ChatbotService(planificacionService, interaccionRepository, categoriaRepository,
                new InstruccionesChatbot(), chatbotClient, MAX_POR_DIA, MAX_CARACTERES, MAX_HISTORIAL, maxRevisiones);
    }

    // ------------------------------------------------------------------ ayudas

    /** HU-31/32: el asistente lee el texto extraído del PDF, no el cuadro de texto antiguo. */
    private Planificacion conArchivo(Planificacion planificacion, String textoExtraido) {
        planificacion.adjuntarArchivo("archivo.pdf", "plan.pdf", 1234L, textoExtraido,
                LocalDateTime.of(2026, 10, 1, 9, 0));
        return planificacion;
    }

    private Planificacion borrador() {
        Planificacion borrador = Planificacion.borrador(null, null);
        conArchivo(borrador, "Texto del borrador");
        borrador.setIdPlanificacion(ID_PLANIFICACION);
        return borrador;
    }

    private Planificacion rechazada(String retroalimentacion, LocalDateTime fechaRetro) {
        Planificacion rechazada = new Planificacion(null, null, LocalDateTime.of(2026, 10, 1, 9, 0));
        conArchivo(rechazada, "Texto mejorado");
        rechazada.setIdPlanificacion(ID_PLANIFICACION);
        rechazada.setEstado(EstadoPlanificacion.RECHAZADA);
        rechazada.setRetroalimentacion(retroalimentacion);
        rechazada.setFechaRetro(fechaRetro);
        return rechazada;
    }

    private Planificacion enEstado(EstadoPlanificacion estado) {
        Planificacion planificacion = new Planificacion(null, null, LocalDateTime.of(2026, 10, 1, 9, 0));
        conArchivo(planificacion, "Texto");
        planificacion.setIdPlanificacion(ID_PLANIFICACION);
        planificacion.setEstado(estado);
        return planificacion;
    }

    private PlanificacionVista vistaConArchivo(String etiqueta, EstadoPlanificacion estado, String retroalimentacion,
                                               LocalDateTime fechaEnvio, LocalDateTime fechaRetro) {
        return new PlanificacionVista(ID_PLANIFICACION, etiqueta, null, estado, retroalimentacion,
                fechaEnvio, fechaRetro, "plan.pdf", 1234L, LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    private InteraccionChatbot previa(String mensaje, String respuesta) {
        return new InteraccionChatbot(null, mensaje, respuesta, LocalDateTime.of(2026, 10, 2, 10, 0));
    }

    private InteraccionChatbot previa(String mensaje, String respuesta, TipoInteraccion tipo) {
        return new InteraccionChatbot(null, mensaje, respuesta, LocalDateTime.of(2026, 10, 2, 10, 0), tipo);
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

    // ------------------------------------------------------- HU-31/32: PDF

    @Test
    @DisplayName("HU-31/32. El asistente recibe el texto extraído del PDF, no el texto antiguo de la planificación")
    void enviarMensaje_usaElTextoExtraidoDelPdf() {
        Planificacion planificacion = Planificacion.borrador(null, "Texto viejo del cuadro de texto");
        conArchivo(planificacion, "Texto extraído del PDF");
        planificacion.setIdPlanificacion(ID_PLANIFICACION);
        dadoQueLaPlanificacionEs(planificacion);
        dadoQueHayAspectos();
        dadoQueElAsistenteResponde(new Respuesta("Respuesta", List.of()));

        enviar("¿Cómo está?");

        Solicitud solicitud = solicitudEnviada();
        assertTrue(solicitud.instrucciones().contains("Texto extraído del PDF"));
        assertFalse(solicitud.instrucciones().contains("Texto viejo"));
    }

    @Test
    @DisplayName("HU-31/32. Sin PDF subido no se puede conversar con el asistente: IllegalState y no se consulta")
    void enviarMensaje_sinArchivo_lanzaIllegalState() {
        Planificacion sinArchivo = Planificacion.borrador(null, "Plan antiguo solo con texto");
        sinArchivo.setIdPlanificacion(ID_PLANIFICACION);
        dadoQueLaPlanificacionEs(sinArchivo);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.enviarMensaje(ID_ESTUDIANTE, ID_PLANIFICACION, "Hola"));

        assertTrue(ex.getMessage().contains("PDF"), ex.getMessage());
        verifyNoInteractions(chatbotClient, interaccionRepository);
    }

    @Test
    @DisplayName("HU-31/32. La pantalla del chat de una planificación sin PDF lanza IllegalState")
    void obtenerConversacion_sinArchivo_lanzaIllegalState() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(new PlanificacionVista(ID_PLANIFICACION, "L", "Plan antiguo",
                        EstadoPlanificacion.BORRADOR, null, null, null));

        assertThrows(IllegalStateException.class,
                () -> service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION));

        verifyNoInteractions(interaccionRepository);
    }

    // --------------------------------------------------------------- pantalla

    @Test
    @DisplayName("HU-26. La conversación de un borrador trae los turnos en orden y los mensajes que quedan hoy")
    void obtenerConversacion_borrador() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("Práctica (ESPECIALIDAD)", EstadoPlanificacion.BORRADOR,
                        null, null, null));
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
        assertEquals("plan.pdf", vista.archivoOriginal());
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 0), vista.archivoFecha());
        assertEquals(ChatbotService.DESDE_SIEMPRE, fechaConsultadaDelHistorial());
    }

    @Test
    @DisplayName("HU-26. Pasado el límite diario los mensajes restantes son 0, nunca negativos")
    void obtenerConversacion_restantesNoSonNegativos() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.BORRADOR, null, null, null));
        dadoQueSeUsaronHoy(MAX_POR_DIA + 4);

        assertEquals(0, service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION).mensajesRestantesHoy());
    }

    @Test
    @DisplayName("HU-26. La conversación de una rechazada es solo la posterior al rechazo e incluye la retroalimentación")
    void obtenerConversacion_rechazada() {
        LocalDateTime rechazo = LocalDateTime.of(2026, 10, 3, 12, 0);
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.RECHAZADA,
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

    // ------------------------------------------------ HU-30: revisiones y preguntas

    private void dadoQueSeUsaronRevisiones(long revisiones) {
        when(interaccionRepository.countByPlanificacion_IdPlanificacionAndTipo(ID_PLANIFICACION,
                TipoInteraccion.REVISION)).thenReturn(revisiones);
    }

    private void pedirRevision() {
        service.pedirRevision(ID_ESTUDIANTE, ID_PLANIFICACION);
    }

    /** Escenario normal de una revisión: borrador con PDF, aspectos, ninguna revisión usada y un asistente que responde. */
    private void dadoUnEscenarioDeRevision(Respuesta respuesta) {
        dadoUnEscenarioNormal(respuesta);
        dadoQueSeUsaronRevisiones(0);
    }

    @Test
    @DisplayName("HU-30. Un mensaje escrito se guarda como PREGUNTA y usa las instrucciones de pregunta")
    void enviarMensaje_seGuardaComoPregunta() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));

        enviar("¿Cómo redacto un objetivo medible?");

        assertEquals(TipoInteraccion.PREGUNTA, interaccionGuardada().getTipo());
        String instruccionesEnviadas = solicitudEnviada().instrucciones();
        assertTrue(instruccionesEnviadas.contains("TIPO DE CONSULTA: PREGUNTA"));
        assertFalse(instruccionesEnviadas.contains("TIPO DE CONSULTA: REVISIÓN COMPLETA"));
    }

    @Test
    @DisplayName("HU-30. Las preguntas no consultan ni gastan revisiones: con el tope usado se siguen aceptando")
    void enviarMensaje_conRevisionesAgotadas_sigueAceptandoPreguntas() {
        dadoUnEscenarioNormal(new Respuesta("Consejo", List.of()));
        lenient().when(interaccionRepository.countByPlanificacion_IdPlanificacionAndTipo(ID_PLANIFICACION,
                TipoInteraccion.REVISION)).thenReturn((long) MAX_REVISIONES);

        enviar("Una duda más");

        assertEquals(TipoInteraccion.PREGUNTA, interaccionGuardada().getTipo());
        verify(interaccionRepository, never()).countByPlanificacion_IdPlanificacionAndTipo(any(), any());
    }

    @Test
    @DisplayName("HU-30. Una revisión se guarda como REVISION, con el mensaje fijo, la respuesta y los hallazgos")
    void pedirRevision_guardaLaInteraccionComoRevision() {
        dadoUnEscenarioDeRevision(new Respuesta("  Revisión del asistente  ", List.of(
                new Hallazgo("OBJETIVOS", NivelHallazgo.DEBIL, "No son medibles"),
                new Hallazgo("EVALUACION", NivelHallazgo.ADECUADO, "Bien planteada"))));

        pedirRevision();

        InteraccionChatbot guardada = interaccionGuardada();
        assertEquals(TipoInteraccion.REVISION, guardada.getTipo());
        assertEquals(ChatbotService.MENSAJE_REVISION, guardada.getMensajeEstudiante());
        assertEquals("Revisión del asistente", guardada.getRespuestaBot());
        assertEquals(2, guardada.getHallazgos().size());
        assertSame(OBJETIVOS, guardada.getHallazgos().get(0).getCategoria());
    }

    @Test
    @DisplayName("HU-30. La solicitud de una revisión lleva el mensaje fijo y las instrucciones de revisión completa")
    void pedirRevision_armaLaSolicitud() {
        dadoUnEscenarioDeRevision(new Respuesta("Revisión", List.of()));

        pedirRevision();

        Solicitud solicitud = solicitudEnviada();
        assertEquals(ChatbotService.MENSAJE_REVISION, solicitud.mensaje());
        assertEquals(List.of("OBJETIVOS", "EVALUACION"), solicitud.categorias());
        assertTrue(solicitud.instrucciones().contains("TIPO DE CONSULTA: REVISIÓN COMPLETA"));
        assertFalse(solicitud.instrucciones().contains("TIPO DE CONSULTA: PREGUNTA"));
        assertTrue(solicitud.instrucciones().contains("Texto del borrador"));
    }

    @Test
    @DisplayName("HU-30. Una revisión de una planificación rechazada considera lo conversado desde el rechazo y el comentario")
    void pedirRevision_rechazada_desdeElRechazo() {
        LocalDateTime rechazo = LocalDateTime.of(2026, 10, 3, 12, 0);
        dadoQueLaPlanificacionEs(rechazada("Faltan objetivos medibles.", rechazo));
        dadoQueHayAspectos();
        dadoQueSeUsaronRevisiones(0);
        dadoQueElAsistenteResponde(new Respuesta("Revisión", List.of()));

        pedirRevision();

        assertEquals(rechazo, fechaConsultadaDelHistorial());
        assertTrue(solicitudEnviada().instrucciones().contains("Faltan objetivos medibles."));
    }

    @Test
    @DisplayName("HU-30. Con una revisión menos que el tope todavía se puede pedir otra")
    void pedirRevision_aunHayCupo() {
        dadoUnEscenarioNormal(new Respuesta("Revisión", List.of()));
        dadoQueSeUsaronRevisiones(MAX_REVISIONES - 1);

        pedirRevision();

        assertEquals(TipoInteraccion.REVISION, interaccionGuardada().getTipo());
    }

    @Test
    @DisplayName("HU-30. Al llegar al tope de revisiones lanza IllegalState, invita a consultar y no llama al asistente")
    void pedirRevision_topeAlcanzado_lanzaIllegalState() {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueSeUsaronRevisiones(MAX_REVISIONES);

        IllegalStateException ex = assertThrows(IllegalStateException.class, this::pedirRevision);

        assertTrue(ex.getMessage().contains("las " + MAX_REVISIONES + " revisiones completas"), ex.getMessage());
        assertTrue(ex.getMessage().contains("profesor, tutor o colaborador"), ex.getMessage());
        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @Test
    @DisplayName("HU-30. Con un tope de una sola revisión el mensaje queda en singular")
    void pedirRevision_topeDeUna_mensajeEnSingular() {
        service = servicioCon(1);
        dadoQueLaPlanificacionEs(borrador());
        dadoQueSeUsaronRevisiones(1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, this::pedirRevision);

        assertTrue(ex.getMessage().contains("la revisión completa"), ex.getMessage());
        assertFalse(ex.getMessage().contains("1 revisiones"), ex.getMessage());
    }

    @Test
    @DisplayName("HU-30. Si se agotaron las revisiones y el límite diario, el aviso es el de las revisiones")
    void pedirRevision_topeYLimiteDiario_avisaDeLasRevisiones() {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueSeUsaronRevisiones(MAX_REVISIONES);
        lenient().when(interaccionRepository
                .countByPlanificacion_Linea_Inscripcion_Estudiante_IdUsuarioAndFechaGreaterThanEqual(
                        eq(ID_ESTUDIANTE), any(LocalDateTime.class)))
                .thenReturn((long) MAX_POR_DIA);

        IllegalStateException ex = assertThrows(IllegalStateException.class, this::pedirRevision);

        assertTrue(ex.getMessage().contains("revisiones completas"), ex.getMessage());
    }

    @Test
    @DisplayName("HU-30. Una revisión también cuenta para el límite diario: con cupo se acepta")
    void pedirRevision_limiteDiario_aunHayCupo() {
        dadoUnEscenarioDeRevision(new Respuesta("Revisión", List.of()));
        dadoQueSeUsaronHoy(MAX_POR_DIA - 1);

        pedirRevision();

        verify(interaccionRepository).save(any(InteraccionChatbot.class));
    }

    @Test
    @DisplayName("HU-30. Al llegar al máximo diario no se puede pedir una revisión, aunque queden revisiones")
    void pedirRevision_limiteDiarioAlcanzado_lanzaIllegalState() {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueSeUsaronRevisiones(0);
        dadoQueSeUsaronHoy(MAX_POR_DIA);

        IllegalStateException ex = assertThrows(IllegalStateException.class, this::pedirRevision);

        assertTrue(ex.getMessage().contains(MAX_POR_DIA + " mensajes por día"), ex.getMessage());
        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @ParameterizedTest(name = "HU-30. Si el asistente falla por {0}, la revisión no se guarda ni se gasta")
    @EnumSource(Causa.class)
    void pedirRevision_falloDelAsistente_noGuardaNada(Causa causa) {
        dadoQueLaPlanificacionEs(borrador());
        dadoQueHayAspectos();
        dadoQueSeUsaronRevisiones(0);
        when(chatbotClient.responder(any(Solicitud.class))).thenThrow(new ChatbotNoDisponibleException(causa, "x"));

        ChatbotNoDisponibleException ex = assertThrows(ChatbotNoDisponibleException.class, this::pedirRevision);

        assertEquals(causa, ex.getCausa());
        verify(interaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-30. Una respuesta en blanco no se guarda como revisión")
    void pedirRevision_respuestaEnBlanco_noSeGuarda() {
        dadoUnEscenarioDeRevision(new Respuesta("   ", List.of()));

        assertThrows(ChatbotNoDisponibleException.class, this::pedirRevision);

        verify(interaccionRepository, never()).save(any());
    }

    @ParameterizedTest(name = "HU-30. No se puede pedir una revisión de una planificación {0}")
    @EnumSource(value = EstadoPlanificacion.class, names = {"ENVIADA", "APROBADA"})
    void pedirRevision_estadoNoPermitido_lanzaIllegalState(EstadoPlanificacion estado) {
        dadoQueLaPlanificacionEs(enEstado(estado));

        assertThrows(IllegalStateException.class, this::pedirRevision);

        verifyNoInteractions(chatbotClient, interaccionRepository);
    }

    @Test
    @DisplayName("HU-30. Sin PDF subido no se puede pedir una revisión")
    void pedirRevision_sinArchivo_lanzaIllegalState() {
        Planificacion sinArchivo = Planificacion.borrador(null, "Plan antiguo solo con texto");
        sinArchivo.setIdPlanificacion(ID_PLANIFICACION);
        dadoQueLaPlanificacionEs(sinArchivo);

        IllegalStateException ex = assertThrows(IllegalStateException.class, this::pedirRevision);

        assertTrue(ex.getMessage().contains("PDF"), ex.getMessage());
        verifyNoInteractions(chatbotClient, interaccionRepository);
    }

    @ParameterizedTest(name = "HU-30. Revisión de una planificación ajena o inexistente: {0}")
    @EnumSource(value = HttpStatus.class, names = {"FORBIDDEN", "NOT_FOUND"})
    void pedirRevision_planificacionAjenaOInexistente_sePropaga(HttpStatus estado) {
        when(planificacionService.obtenerPlanificacionDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenThrow(new ResponseStatusException(estado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, this::pedirRevision);

        assertEquals(estado.value(), ex.getStatusCode().value());
        verificarQueNoSeLlamoAlAsistenteNiSeGuardo();
    }

    @ParameterizedTest(name = "HU-30. El tope de revisiones por planificación debe ser al menos 1: {0}")
    @ValueSource(ints = {0, -1})
    void constructor_topeDeRevisionesInvalido_lanzaIllegalArgument(int invalido) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> servicioCon(invalido));

        assertTrue(ex.getMessage().contains("max-revisiones-por-planificacion"), ex.getMessage());
    }

    // ----------------------------------------- HU-30: pantalla (contadores y tipos)

    @Test
    @DisplayName("HU-30. La pantalla informa las revisiones que quedan y el tope")
    void obtenerConversacion_revisionesRestantes() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.BORRADOR, null, null, null));
        dadoQueSeUsaronRevisiones(1);

        VistaChat vista = service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertEquals(MAX_REVISIONES - 1, vista.revisionesRestantes());
        assertEquals(MAX_REVISIONES, vista.maxRevisiones());
    }

    @Test
    @DisplayName("HU-30. Sin revisiones usadas quedan todas")
    void obtenerConversacion_sinRevisionesUsadas() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.BORRADOR, null, null, null));

        assertEquals(MAX_REVISIONES,
                service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION).revisionesRestantes());
    }

    @Test
    @DisplayName("HU-30. Pasado el tope las revisiones restantes son 0, nunca negativas")
    void obtenerConversacion_revisionesRestantesNoSonNegativas() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.BORRADOR, null, null, null));
        dadoQueSeUsaronRevisiones(MAX_REVISIONES + 3);

        assertEquals(0, service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION).revisionesRestantes());
    }

    @Test
    @DisplayName("HU-30. En una rechazada las revisiones de antes del rechazo siguen contando")
    void obtenerConversacion_rechazada_lasRevisionesNoSeReinician() {
        LocalDateTime rechazo = LocalDateTime.of(2026, 10, 3, 12, 0);
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.RECHAZADA,
                        "Falta la evaluación", rechazo.minusDays(1), rechazo));
        dadoQueSeUsaronRevisiones(MAX_REVISIONES);

        VistaChat vista = service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertTrue(vista.turnos().isEmpty(), "la conversación nueva empieza vacía");
        assertEquals(0, vista.revisionesRestantes());
    }

    @Test
    @DisplayName("HU-30. Cada turno de la pantalla lleva su tipo; lo anterior a HU-30 queda como pregunta")
    void obtenerConversacion_turnosConSuTipo() {
        when(planificacionService.obtenerVistaDelEstudiante(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenReturn(vistaConArchivo("L", EstadoPlanificacion.BORRADOR, null, null, null));
        dadoQueElHistorialEs(
                previa(ChatbotService.MENSAJE_REVISION, "r1", TipoInteraccion.REVISION),
                previa("¿Y el cierre?", "r2", TipoInteraccion.PREGUNTA),
                previa("de antes de HU-30", "r3"));

        VistaChat vista = service.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION);

        assertEquals(TipoInteraccion.REVISION, vista.turnos().get(0).tipo());
        assertEquals(TipoInteraccion.PREGUNTA, vista.turnos().get(1).tipo());
        assertEquals(TipoInteraccion.PREGUNTA, vista.turnos().get(2).tipo());
    }
}
