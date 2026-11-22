package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.FechaLimiteService;
import jakarta.servlet.http.HttpServletRequest;
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

/** N-09: el coordinador o el profesor de la asignatura fijan las fechas límite del semestre. */
@Controller
@RequestMapping("/semestre/{idSeccion}/fechas")
@PreAuthorize("hasAnyRole('COORDINADOR','PROFESOR_ASIGNATURA')")
public class FechasLimiteController {
    private final FechaLimiteService servicio;

    public FechasLimiteController(FechaLimiteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String ver(@PathVariable Long idSeccion, @AuthenticationPrincipal UsuarioDetails usuario,
                      HttpServletRequest peticion, Model model) {
        boolean coordinador = peticion.isUserInRole("COORDINADOR");
        model.addAttribute("fechas", servicio.vista(usuario.getIdUsuario(), coordinador, idSeccion));
        model.addAttribute("esCoordinador", coordinador);
        return "semestre/fechas";
    }

    @PostMapping("/entregas/{idEntrega}")
    public String fijarEntrega(@PathVariable Long idSeccion, @PathVariable Long idEntrega,
                               @RequestParam(required = false) String fecha,
                               @AuthenticationPrincipal UsuarioDetails usuario, HttpServletRequest peticion,
                               RedirectAttributes redirect) {
        try {
            servicio.fijarFechaEntrega(usuario.getIdUsuario(), peticion.isUserInRole("COORDINADOR"), idSeccion,
                    idEntrega, fecha);
            redirect.addFlashAttribute("mensaje", "Fecha límite de la entrega guardada.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    @PostMapping("/pautas/{idPauta}")
    public String fijarPauta(@PathVariable Long idSeccion, @PathVariable Long idPauta,
                             @RequestParam(required = false) String fecha,
                             @AuthenticationPrincipal UsuarioDetails usuario, HttpServletRequest peticion,
                             RedirectAttributes redirect) {
        try {
            servicio.fijarFechaPauta(usuario.getIdUsuario(), peticion.isUserInRole("COORDINADOR"), idSeccion,
                    idPauta, fecha);
            redirect.addFlashAttribute("mensaje", "Fecha límite de la pauta guardada.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return volver(idSeccion);
    }

    private String volver(Long idSeccion) {
        return "redirect:/semestre/" + idSeccion + "/fechas";
    }
}
