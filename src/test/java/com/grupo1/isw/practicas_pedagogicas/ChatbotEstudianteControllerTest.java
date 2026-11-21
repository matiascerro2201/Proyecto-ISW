package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.controllers.ChatbotEstudianteController;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.ChatbotService;
import com.grupo1.isw.practicas_pedagogicas.services.ChatbotService.VistaChat;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException.Causa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-30: respuestas del controlador del chat, en particular el botón "Pedir revisión completa". */
@ExtendWith(MockitoExtension.class)
class ChatbotEstudianteControllerTest {

    private static final Long ID_ESTUDIANTE = 4L;
    private static final Long ID_PLANIFICACION = 10L;
    private static final String VISTA_CHAT = "estudiante/planificaciones/chat";
    private static final String CHAT = "/estudiante/planificaciones/10/chat";

    @Mock
    private ChatbotService chatbotService;

    private ChatbotEstudianteController controlador;
    private Model model;
    private RedirectAttributesModelMap redirect;
    private UsuarioDetails usuario;
    private VistaChat vista;

    @BeforeEach
    void preparar() {
        controlador = new ChatbotEstudianteController(chatbotService);
        model = new ExtendedModelMap();
        redirect = new RedirectAttributesModelMap();
        Usuario estudiante = new Usuario();
        estudiante.setIdUsuario(ID_ESTUDIANTE);
        estudiante.setRol(RolUsuario.ESTUDIANTE);
        usuario = new UsuarioDetails(estudiante);
        vista = new VistaChat(ID_PLANIFICACION, "Práctica", "plan.pdf", LocalDateTime.of(2026, 10, 1, 9, 0),
                null, false, List.of(), 5, 1500, 1, 2);
    }

    private void dadoQueElChatSePuedeMostrar() {
        when(chatbotService.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION)).thenReturn(vista);
    }

    // ------------------------------------------------------ pedir revisión

    @Test
    @DisplayName("HU-30. Una revisión pedida correctamente redirige al chat")
    void pedirRevision_ok_redirigeAlChat() {
        String destino = controlador.pedirRevision(ID_PLANIFICACION, usuario, model, redirect);

        assertEquals("redirect:" + CHAT + "#escribir", destino);
        verify(chatbotService).pedirRevision(ID_ESTUDIANTE, ID_PLANIFICACION);
    }

    @Test
    @DisplayName("HU-30. Sin revisiones o sin cupo diario vuelve al chat con el motivo")
    void pedirRevision_noSePuede_muestraElMotivo() {
        doThrow(new IllegalStateException("Ya usaste las 2 revisiones completas"))
                .when(chatbotService).pedirRevision(ID_ESTUDIANTE, ID_PLANIFICACION);
        dadoQueElChatSePuedeMostrar();

        String destino = controlador.pedirRevision(ID_PLANIFICACION, usuario, model, redirect);

        assertEquals(VISTA_CHAT, destino);
        assertEquals("Ya usaste las 2 revisiones completas", model.getAttribute("error"));
        assertSame(vista, model.getAttribute("chat"));
        assertEquals("", model.getAttribute("textoEscrito"));
    }

    @ParameterizedTest(name = "HU-30. Si el asistente no responde ({0}), vuelve al chat con el aviso para el estudiante")
    @EnumSource(Causa.class)
    void pedirRevision_asistenteNoDisponible_muestraElAviso(Causa causa) {
        doThrow(new ChatbotNoDisponibleException(causa, "x"))
                .when(chatbotService).pedirRevision(ID_ESTUDIANTE, ID_PLANIFICACION);
        dadoQueElChatSePuedeMostrar();

        String destino = controlador.pedirRevision(ID_PLANIFICACION, usuario, model, redirect);

        assertEquals(VISTA_CHAT, destino);
        assertEquals(causa.getMensajeParaElEstudiante(), model.getAttribute("error"));
    }

    @Test
    @DisplayName("HU-30. Si la planificación ya no admite chat vuelve a la lista con el motivo")
    void pedirRevision_yaNoAdmiteChat_vuelveALaLista() {
        doThrow(new IllegalStateException("Solo se puede conversar con el asistente sobre una planificación en borrador o rechazada"))
                .when(chatbotService).pedirRevision(ID_ESTUDIANTE, ID_PLANIFICACION);
        when(chatbotService.obtenerConversacion(ID_ESTUDIANTE, ID_PLANIFICACION))
                .thenThrow(new IllegalStateException("No se puede conversar"));

        String destino = controlador.pedirRevision(ID_PLANIFICACION, usuario, model, redirect);

        assertEquals("redirect:/estudiante/planificaciones", destino);
        assertEquals("No se puede conversar", redirect.getFlashAttributes().get("error"));
    }

    // --------------------------------------------------------- preguntas

    @Test
    @DisplayName("HU-30. Una pregunta enviada correctamente redirige al chat")
    void enviar_ok_redirigeAlChat() {
        String destino = controlador.enviar(ID_PLANIFICACION, "¿Cómo va mi cierre?", usuario, model, redirect);

        assertEquals("redirect:" + CHAT + "#escribir", destino);
        verify(chatbotService).enviarMensaje(ID_ESTUDIANTE, ID_PLANIFICACION, "¿Cómo va mi cierre?");
    }

    @Test
    @DisplayName("HU-30. Una pregunta rechazada vuelve al chat conservando lo que escribió el estudiante")
    void enviar_rechazada_conservaElTexto() {
        doThrow(new IllegalArgumentException("El mensaje es demasiado largo"))
                .when(chatbotService).enviarMensaje(ID_ESTUDIANTE, ID_PLANIFICACION, "texto largo");
        dadoQueElChatSePuedeMostrar();

        String destino = controlador.enviar(ID_PLANIFICACION, "texto largo", usuario, model, redirect);

        assertEquals(VISTA_CHAT, destino);
        assertEquals("El mensaje es demasiado largo", model.getAttribute("error"));
        assertEquals("texto largo", model.getAttribute("textoEscrito"));
    }

    // ----------------------------------------------------------- pantalla

    @Test
    @DisplayName("HU-30. Al abrir el chat se entrega la vista con los contadores y sin error")
    void ver_entregaLaVista() {
        dadoQueElChatSePuedeMostrar();

        String destino = controlador.ver(ID_PLANIFICACION, usuario, model, redirect);

        assertEquals(VISTA_CHAT, destino);
        VistaChat entregada = (VistaChat) model.getAttribute("chat");
        assertEquals(1, entregada.revisionesRestantes());
        assertEquals(2, entregada.maxRevisiones());
        assertNull(model.getAttribute("error"));
        assertEquals("", model.getAttribute("textoEscrito"));
        assertFalse(redirect.getFlashAttributes().containsKey("error"));
    }
}
