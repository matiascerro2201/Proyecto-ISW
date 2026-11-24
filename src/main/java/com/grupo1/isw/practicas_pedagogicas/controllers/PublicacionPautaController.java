package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PublicacionPautaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** N-18: el profesor de la asignatura publica la nota y el detalle de cada pauta aplicada. */
@Controller
@RequestMapping("/profesor/pautas-semestre")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class PublicacionPautaController {
    private final PublicacionPautaService servicio;

    public PublicacionPautaController(PublicacionPautaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String secciones(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("secciones", servicio.seccionesDelProfesor(usuario.getIdUsuario()));
        return "profesor/pautas-semestre/lista";
    }

    @GetMapping("/{idSeccion}")
    public String panel(@PathVariable Long idSeccion, @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("panel", servicio.panel(usuario.getIdUsuario(), idSeccion));
        return "profesor/pautas-semestre/seccion";
    }

    @GetMapping("/{idSeccion}/{idEvaluacion}")
    public String detalle(@PathVariable Long idSeccion, @PathVariable Long idEvaluacion,
                          @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("detalle", servicio.detalle(usuario.getIdUsuario(), idSeccion, idEvaluacion));
        return "profesor/pautas-semestre/detalle";
    }

    @PostMapping("/{idSeccion}/publicar-todas")
    public String publicarTodas(@PathVariable Long idSeccion, @AuthenticationPrincipal UsuarioDetails usuario,
                                RedirectAttributes redirect) {
        int cantidad = servicio.publicarTodas(usuario.getIdUsuario(), idSeccion);
        redirect.addFlashAttribute("mensaje", cantidad == 0 ? "No había pautas por publicar."
                : "Se publicaron " + cantidad + " pauta(s).");
        return "redirect:/profesor/pautas-semestre/" + idSeccion;
    }

    @PostMapping("/{idSeccion}/{idEvaluacion}/publicar")
    public String publicar(@PathVariable Long idSeccion, @PathVariable Long idEvaluacion,
                           @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.publicar(usuario.getIdUsuario(), idSeccion, idEvaluacion);
            redirect.addFlashAttribute("mensaje", "Pauta publicada: el estudiante ya puede ver su nota y el detalle.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profesor/pautas-semestre/" + idSeccion + "/" + idEvaluacion;
    }

    @PostMapping("/{idSeccion}/{idEvaluacion}/retirar")
    public String retirar(@PathVariable Long idSeccion, @PathVariable Long idEvaluacion,
                          @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.retirar(usuario.getIdUsuario(), idSeccion, idEvaluacion);
            redirect.addFlashAttribute("mensaje", "Publicación retirada: el estudiante ya no ve esta pauta.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profesor/pautas-semestre/" + idSeccion + "/" + idEvaluacion;
    }
}
