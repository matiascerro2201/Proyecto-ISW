package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService.PlanificacionVista;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;

@Controller
@RequestMapping("/estudiante/planificaciones")
@PreAuthorize("hasRole('ESTUDIANTE')") // 2. Protege todas las rutas para que solo entren estudiantes
public class PlanificacionEstudianteController {

    private static final String BASE = "/estudiante/planificaciones";
    private static final String VISTA_LISTA = "estudiante/planificaciones/lista";
    private static final String VISTA_FORMULARIO = "estudiante/planificaciones/formulario";

    private final PlanificacionService planificacionService;

    public PlanificacionEstudianteController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) { // 3. Captura al usuario
        model.addAttribute("planificaciones",
                planificacionService.listarPlanificacionesDelEstudiante(usuarioLogueado.getIdUsuario())); // 4. Usa su ID
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        cargarFormularioNueva(model, usuarioLogueado.getIdUsuario(), null, "", null);
        return VISTA_FORMULARIO;
    }

    @PostMapping
    public String crear(@RequestParam(required = false) Long idLinea,
                        @RequestParam(defaultValue = "") String contenido,
                        @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            planificacionService.crearPlanificacion(usuarioLogueado.getIdUsuario(), idLinea, contenido);
        } catch (IllegalArgumentException e) {
            cargarFormularioNueva(model, usuarioLogueado.getIdUsuario(), idLinea, contenido, e.getMessage());
            return VISTA_FORMULARIO;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación enviada correctamente.");
        return "redirect:" + BASE;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, 
                         @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                         Model model, RedirectAttributes redirectAttributes) {
        PlanificacionVista planificacion =
                planificacionService.obtenerVistaDelEstudiante(usuarioLogueado.getIdUsuario(), id);

        if (!planificacion.puedeReenviar()) {
            redirectAttributes.addFlashAttribute("error",
                    "Solo se puede editar y reenviar una planificación rechazada.");
            return "redirect:" + BASE;
        }

        cargarFormularioReenvio(model, planificacion, planificacion.contenido(), null);
        return VISTA_FORMULARIO;
    }

    @PostMapping("/{id}/reenviar")
    public String reenviar(@PathVariable Long id,
                           @RequestParam(defaultValue = "") String contenido,
                           @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            planificacionService.reenviarPlanificacion(usuarioLogueado.getIdUsuario(), id, contenido);
        } catch (IllegalArgumentException e) {
            PlanificacionVista planificacion =
                    planificacionService.obtenerVistaDelEstudiante(usuarioLogueado.getIdUsuario(), id);
            cargarFormularioReenvio(model, planificacion, contenido, e.getMessage());
            return VISTA_FORMULARIO;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación reenviada al colaborador.");
        return "redirect:" + BASE;
    }

    private void cargarFormularioNueva(Model model, Long idEstudiante, Long idLineaSeleccionada, String contenido, String error) {
        model.addAttribute("titulo", "Nueva planificación de clase");
        model.addAttribute("accion", BASE);
        model.addAttribute("lineas", planificacionService.obtenerLineasDelEstudiante(idEstudiante));
        model.addAttribute("idLineaSeleccionada", idLineaSeleccionada);
        model.addAttribute("contenido", contenido);
        model.addAttribute("error", error);
    }

    private void cargarFormularioReenvio(Model model, PlanificacionVista planificacion, String contenido, String error) {
        model.addAttribute("titulo", "Editar y reenviar planificación");
        model.addAttribute("accion", BASE + "/" + planificacion.idPlanificacion() + "/reenviar");
        model.addAttribute("planificacion", planificacion);
        model.addAttribute("contenido", contenido);
        model.addAttribute("error", error);
    }
}