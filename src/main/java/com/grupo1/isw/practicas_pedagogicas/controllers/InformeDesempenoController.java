package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.EstructuraInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.InformeDesempenoService;
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

/** N-19: el profesor genera el informe de desempeño con la estructura del semestre, fija la nota final y lo publica. */
@Controller
@RequestMapping("/profesor/desempeno")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class InformeDesempenoController {
    private final InformeDesempenoService servicio;

    public InformeDesempenoController(InformeDesempenoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String secciones(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("secciones", servicio.seccionesDelProfesor(usuario.getIdUsuario()));
        return "profesor/desempeno/lista";
    }

    @GetMapping("/{idSeccion}")
    public String estudiantes(@PathVariable Long idSeccion, @AuthenticationPrincipal UsuarioDetails usuario,
                              Model model) {
        model.addAttribute("seccion", servicio.seccion(usuario.getIdUsuario(), idSeccion));
        model.addAttribute("filas", servicio.estudiantes(usuario.getIdUsuario(), idSeccion));
        return "profesor/desempeno/seccion";
    }

    @GetMapping("/{idSeccion}/{idInscripcion}")
    public String detalle(@PathVariable Long idSeccion, @PathVariable Long idInscripcion,
                          @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("detalle", servicio.detalle(usuario.getIdUsuario(), idInscripcion));
        return "profesor/desempeno/detalle";
    }

    @PostMapping("/{idSeccion}/{idInscripcion}/generar")
    public String generar(@PathVariable Long idSeccion, @PathVariable Long idInscripcion,
                          @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.generar(usuario.getIdUsuario(), idInscripcion);
            redirect.addFlashAttribute("mensaje", "Informe generado con la nota sugerida.");
        } catch (EstructuraInvalidaException e) {
            redirect.addFlashAttribute("errores", e.getErrores());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion, idInscripcion);
    }

    @PostMapping("/{idSeccion}/{idInscripcion}/nota-final")
    public String notaFinal(@PathVariable Long idSeccion, @PathVariable Long idInscripcion,
                            @RequestParam(required = false) String nota,
                            @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.fijarNotaFinal(usuario.getIdUsuario(), idInscripcion, nota);
            redirect.addFlashAttribute("mensaje", "Nota final guardada. El estudiante la verá cuando publiques el informe.");
        } catch (EstructuraInvalidaException e) {
            redirect.addFlashAttribute("errores", e.getErrores());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion, idInscripcion);
    }

    @PostMapping("/{idSeccion}/{idInscripcion}/publicar")
    public String publicar(@PathVariable Long idSeccion, @PathVariable Long idInscripcion,
                           @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            servicio.publicar(usuario.getIdUsuario(), idInscripcion);
            redirect.addFlashAttribute("mensaje", "Informe publicado: el estudiante ya ve su nota final.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion, idInscripcion);
    }

    private String volver(Long idSeccion, Long idInscripcion) {
        return "redirect:/profesor/desempeno/" + idSeccion + "/" + idInscripcion;
    }
}
