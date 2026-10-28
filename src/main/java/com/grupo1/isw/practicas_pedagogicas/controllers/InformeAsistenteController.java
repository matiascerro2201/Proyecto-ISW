package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.InformeAsistenteService;
import com.grupo1.isw.practicas_pedagogicas.services.InformeAsistenteService.OpcionSeccion;
import com.grupo1.isw.practicas_pedagogicas.services.chatbot.ChatbotNoDisponibleException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * HU-26: el informe del asistente para el profesor de la asignatura. Muestra qué aspectos de las planificaciones
 * les cuestan más a sus estudiantes y un análisis redactado por el asistente. Nunca muestra nombres.
 */
@Controller
@RequestMapping("/profesor/asistente")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class InformeAsistenteController {
    private static final String BASE = "/profesor/asistente";
    private static final String VISTA = "profesor/asistente/informe";

    private final InformeAsistenteService informeService;

    public InformeAsistenteController(InformeAsistenteService informeService) {
        this.informeService = informeService;
    }

    /** Sin el parámetro "seccion" se muestra la más reciente del profesor. */
    @GetMapping
    public String ver(@RequestParam(required = false) Long seccion,
                      @AuthenticationPrincipal UsuarioDetails usuario,
                      Model model) {
        Long idProfesor = usuario.getIdUsuario();
        List<OpcionSeccion> secciones = informeService.listarSecciones(idProfesor);
        model.addAttribute("secciones", secciones);

        if (!secciones.isEmpty()) {
            Long idSeleccionada = seccion != null ? seccion : secciones.get(0).idSeccion();
            model.addAttribute("informe", informeService.obtenerInforme(idProfesor, idSeleccionada));
        }
        return VISTA;
    }

    @PostMapping("/generar")
    public String generar(@RequestParam Long seccion,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          RedirectAttributes redirectAttributes) {
        try {
            informeService.generarAnalisis(usuario.getIdUsuario(), seccion);
            redirectAttributes.addFlashAttribute("mensaje", "El análisis se generó correctamente.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ChatbotNoDisponibleException e) {
            redirectAttributes.addFlashAttribute("error", e.getCausa().getMensajeParaElProfesor());
        }
        return "redirect:" + BASE + "?seccion=" + seccion;
    }
}
