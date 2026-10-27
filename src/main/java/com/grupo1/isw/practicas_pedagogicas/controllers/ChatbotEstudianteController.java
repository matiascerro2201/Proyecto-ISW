package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.ChatbotService;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Conversación del estudiante con el asistente sobre una de sus planificaciones. */
@Controller
@RequestMapping("/estudiante/planificaciones/{id}/chat")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class ChatbotEstudianteController {
    private static final String BASE = "/estudiante/planificaciones";
    private static final String VISTA_CHAT = "estudiante/planificaciones/chat";

    private final ChatbotService chatbotService;

    public ChatbotEstudianteController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping
    public String ver(@PathVariable Long id,
                      @AuthenticationPrincipal UsuarioDetails usuario,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        return mostrarChat(model, redirectAttributes, usuario.getIdUsuario(), id, "", null);
    }

    @PostMapping
    public String enviar(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String mensaje,
                         @AuthenticationPrincipal UsuarioDetails usuario,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            chatbotService.enviarMensaje(idEstudiante, id, mensaje);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return mostrarChat(model, redirectAttributes, idEstudiante, id, mensaje, e.getMessage());
        } catch (ChatbotNoDisponibleException e) {
            return mostrarChat(model, redirectAttributes, idEstudiante, id, mensaje,
                    e.getCausa().getMensajeParaElEstudiante());
        }
        // El ancla lleva al formulario, justo debajo de la respuesta nueva.
        return "redirect:" + BASE + "/" + id + "/chat#escribir";
    }

    /**
     * Muestra el chat, con lo que el estudiante había escrito si hubo un error para que no lo pierda.
     * Si la planificación ya no admite chat (por ejemplo, ya se envió), vuelve a la lista con el motivo.
     */
    private String mostrarChat(Model model, RedirectAttributes redirectAttributes, Long idEstudiante, Long id,
                               String textoEscrito, String error) {
        try {
            model.addAttribute("chat", chatbotService.obtenerConversacion(idEstudiante, id));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        model.addAttribute("textoEscrito", textoEscrito);
        model.addAttribute("error", error);
        return VISTA_CHAT;
    }
}
