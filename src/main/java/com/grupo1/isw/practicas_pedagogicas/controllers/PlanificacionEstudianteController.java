package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService.PlanificacionVista;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Páginas de las planificaciones del estudiante. El PDF se sube aparte (PlanificacionArchivoController);
 * subirlo ya deja guardado el borrador, así que aquí solo queda enviar o reenviar.
 */
@Controller
@RequestMapping("/estudiante/planificaciones")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PlanificacionEstudianteController {
    private static final String BASE = "/estudiante/planificaciones";
    private static final String VISTA_LISTA = "estudiante/planificaciones/lista";
    private static final String VISTA_FORMULARIO = "estudiante/planificaciones/formulario";

    private final PlanificacionService planificacionService;

    public PlanificacionEstudianteController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("planificaciones",
                planificacionService.listarPlanificacionesDelEstudiante(usuario.getIdUsuario()));
        return VISTA_LISTA;
    }

    /** Elige la línea y sube el PDF; al subirlo se crea el borrador y la página pasa a su edición. */
    @GetMapping("/nueva")
    public String nueva(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("titulo", "Nueva planificación de clase");
        model.addAttribute("urlSubida", BASE + "/archivo");
        model.addAttribute("lineas", planificacionService.obtenerLineasDelEstudiante(usuario.getIdUsuario()));
        return VISTA_FORMULARIO;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioDetails usuario,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        PlanificacionVista planificacion =
                planificacionService.obtenerVistaDelEstudiante(usuario.getIdUsuario(), id);

        if (!planificacion.puedeEditar()) {
            redirectAttributes.addFlashAttribute("error",
                    "Solo se puede editar una planificación en borrador o rechazada.");
            return "redirect:" + BASE;
        }

        cargarFormularioEdicion(model, planificacion, null);
        return VISTA_FORMULARIO;
    }

    /** Envía un borrador al colaborador. No depende del asistente. */
    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id,
                         @AuthenticationPrincipal UsuarioDetails usuario,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            planificacionService.enviarBorrador(idEstudiante, id);
        } catch (IllegalArgumentException e) {
            return volverAlFormulario(model, idEstudiante, id, e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación enviada al colaborador.");
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/reenviar")
    public String reenviar(@PathVariable Long id,
                           @AuthenticationPrincipal UsuarioDetails usuario,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            planificacionService.reenviarPlanificacion(idEstudiante, id);
        } catch (IllegalArgumentException e) {
            return volverAlFormulario(model, idEstudiante, id, e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación reenviada al colaborador.");
        return "redirect:" + BASE;
    }

    private String volverAlFormulario(Model model, Long idEstudiante, Long id, String error) {
        PlanificacionVista planificacion = planificacionService.obtenerVistaDelEstudiante(idEstudiante, id);
        cargarFormularioEdicion(model, planificacion, error);
        return VISTA_FORMULARIO;
    }

    /** Formulario de una planificación ya guardada: un borrador (se envía) o una rechazada (se reenvía). */
    private void cargarFormularioEdicion(Model model, PlanificacionVista planificacion, String error) {
        String base = BASE + "/" + planificacion.idPlanificacion();
        boolean borrador = planificacion.esBorrador();

        model.addAttribute("titulo", borrador ? "Editar borrador" : "Editar y reenviar planificación");
        model.addAttribute("urlSubida", BASE + "/archivo");
        model.addAttribute("urlArchivo", base + "/archivo");
        model.addAttribute("accionEnviar", base + (borrador ? "/enviar" : "/reenviar"));
        model.addAttribute("etiquetaEnviar", borrador ? "Enviar al profesor" : "Reenviar al profesor");
        model.addAttribute("planificacion", planificacion);
        model.addAttribute("error", error);
    }
}
