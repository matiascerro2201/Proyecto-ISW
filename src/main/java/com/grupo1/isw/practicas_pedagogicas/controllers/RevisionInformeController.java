package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.RevisionInformeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * HU-11: el profesor de la asignatura confirma o ajusta la calificación sugerida y publica el informe final.
 *
 * La revisión se hace en la página del informe (GET /profesor/informes/{id}, de GeneracionInformeViewController).
 * Esta clase recibe las acciones:
 *   POST /profesor/informes/{id}/calificacion
 *   POST /profesor/informes/{id}/publicar
 * y mantiene GET /profesor/informes/{id}/revision, que lleva a esa página con la revisión abierta.
 */
@Controller
@RequestMapping("/profesor/informes")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class RevisionInformeController {
    private final RevisionInformeService revisionInformeService;

    public RevisionInformeController(RevisionInformeService revisionInformeService) {
        this.revisionInformeService = revisionInformeService;
    }

    @GetMapping("/{idInforme}/revision")
    public String revisar(@PathVariable Long idInforme) {
        return "redirect:" + rutaRevision(idInforme);
    }

    @PostMapping("/{idInforme}/calificacion")
    public String guardarCalificacion(@PathVariable Long idInforme,
                                      @RequestParam(defaultValue = "") String calificacionFinal,
                                      @AuthenticationPrincipal UsuarioDetails usuario,
                                      RedirectAttributes redirectAttributes) {
        try {
            revisionInformeService.guardarCalificacionFinal(usuario.getIdUsuario(), idInforme, calificacionFinal);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("calificacionIngresada", calificacionFinal);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + rutaRevision(idInforme);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + rutaRevision(idInforme);
        }
        redirectAttributes.addFlashAttribute("mensaje",
                "Calificación final guardada. Ya puedes publicar el informe.");
        return "redirect:" + rutaRevision(idInforme);
    }

    @PostMapping("/{idInforme}/publicar")
    public String publicar(@PathVariable Long idInforme,
                           @AuthenticationPrincipal UsuarioDetails usuario,
                           RedirectAttributes redirectAttributes) {
        try {
            revisionInformeService.publicar(usuario.getIdUsuario(), idInforme);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + rutaRevision(idInforme);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Informe publicado: el estudiante ya puede verlo.");
        return "redirect:" + rutaInforme(idInforme);
    }

    private static String rutaInforme(Long idInforme) {
        return "/profesor/informes/" + idInforme;
    }

    private static String rutaRevision(Long idInforme) {
        return rutaInforme(idInforme) + "#revisar";
    }
}
