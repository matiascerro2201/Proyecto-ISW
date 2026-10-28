package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PlazoRevisionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * HU-12: el profesor de la asignatura define la fecha límite de revisión de una sección.
 * La fecha se visualiza en la lista GET /profesor/informes (GeneracionInformeViewController).
 */
@Controller
@RequestMapping("/profesor/informes")
@PreAuthorize("hasRole('PROFESOR_ASIGNATURA')")
public class PlazoRevisionController {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PlazoRevisionService plazoRevisionService;

    public PlazoRevisionController(PlazoRevisionService plazoRevisionService) {
        this.plazoRevisionService = plazoRevisionService;
    }

    @PostMapping("/secciones/{idSeccion}/fecha-limite")
    public String guardar(@PathVariable Long idSeccion,
                          @RequestParam(defaultValue = "") String fechaLimite,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          RedirectAttributes redirect) {
        try {
            LocalDate fecha = plazoRevisionService.guardarFechaLimite(usuario.getIdUsuario(), idSeccion, fechaLimite);
            redirect.addFlashAttribute("mensaje", fecha == null
                    ? "Se quitó la fecha límite de revisión."
                    : "Fecha límite de revisión guardada: " + fecha.format(FORMATO) + ".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorPlazo", e.getMessage());
        }
        return "redirect:/profesor/informes";
    }
}
