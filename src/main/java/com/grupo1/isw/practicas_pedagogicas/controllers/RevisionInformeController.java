package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.RevisionInformeService;
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

/** HU-11: el profesor de la asignatura ajusta la calificación sugerida y publica el informe final. */
@Controller
@RequestMapping("/profesor/informes")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class RevisionInformeController {
    private static final String BASE = "/profesor/informes";
    private static final String VISTA_LISTA = "profesor/informes/lista";
    private static final String VISTA_REVISION = "profesor/informes/revision";

    private final RevisionInformeService revisionInformeService;

    public RevisionInformeController(RevisionInformeService revisionInformeService) {
        this.revisionInformeService = revisionInformeService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("informes", revisionInformeService.listarInformes(usuario.getIdUsuario()));
        return VISTA_LISTA;
    }

    @GetMapping("/{idInforme}")
    public String revisar(@PathVariable Long idInforme,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          Model model) {
        model.addAttribute("informe", revisionInformeService.obtenerInforme(usuario.getIdUsuario(), idInforme));
        return VISTA_REVISION;
    }

    @PostMapping("/{idInforme}/calificacion")
    public String guardarCalificacion(@PathVariable Long idInforme,
                                      @RequestParam(defaultValue = "") String calificacionFinal,
                                      @AuthenticationPrincipal UsuarioDetails usuario,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        Long idProfesor = usuario.getIdUsuario();
        try {
            revisionInformeService.guardarCalificacionFinal(idProfesor, idInforme, calificacionFinal);
        } catch (IllegalArgumentException e) {
            // Nota inválida: se vuelve al formulario conservando lo que escribió el profesor.
            model.addAttribute("informe", revisionInformeService.obtenerInforme(idProfesor, idInforme));
            model.addAttribute("calificacionIngresada", calificacionFinal);
            model.addAttribute("error", e.getMessage());
            return VISTA_REVISION;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE + "/" + idInforme;
        }
        redirectAttributes.addFlashAttribute("mensaje",
                "Calificación final guardada. Ya puedes publicar el informe.");
        return "redirect:" + BASE + "/" + idInforme;
    }

    @PostMapping("/{idInforme}/publicar")
    public String publicar(@PathVariable Long idInforme,
                           @AuthenticationPrincipal UsuarioDetails usuario,
                           RedirectAttributes redirectAttributes) {
        try {
            revisionInformeService.publicar(usuario.getIdUsuario(), idInforme);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE + "/" + idInforme;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Informe publicado: el estudiante ya puede verlo.");
        return "redirect:" + BASE;
    }
}
