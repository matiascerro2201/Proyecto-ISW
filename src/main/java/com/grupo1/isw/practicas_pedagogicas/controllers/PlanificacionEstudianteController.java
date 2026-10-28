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

    @GetMapping("/nueva")
    public String nueva(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        cargarFormularioNueva(model, usuario.getIdUsuario(), null, "", null);
        return VISTA_FORMULARIO;
    }

    /** "Enviar al profesor" desde el formulario de una planificación nueva: se crea y se envía de una vez. */
    @PostMapping
    public String crear(@RequestParam(required = false) Long idLinea,
                        @RequestParam(defaultValue = "") String contenido,
                        @AuthenticationPrincipal UsuarioDetails usuario,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            planificacionService.crearPlanificacion(idEstudiante, idLinea, contenido);
        } catch (IllegalArgumentException e) {
            cargarFormularioNueva(model, idEstudiante, idLinea, contenido, e.getMessage());
            return VISTA_FORMULARIO;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación enviada correctamente.");
        return "redirect:" + BASE;
    }

    /** "Guardar borrador" desde el formulario de una planificación nueva: solo la ve el estudiante. */
    @PostMapping("/borrador")
    public String guardarComoBorrador(@RequestParam(required = false) Long idLinea,
                                      @RequestParam(defaultValue = "") String contenido,
                                      @AuthenticationPrincipal UsuarioDetails usuario,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        Long idBorrador;
        try {
            idBorrador = planificacionService.crearBorrador(idEstudiante, idLinea, contenido);
        } catch (IllegalArgumentException e) {
            cargarFormularioNueva(model, idEstudiante, idLinea, contenido, e.getMessage());
            return VISTA_FORMULARIO;
        }
        redirectAttributes.addFlashAttribute("mensaje",
                "Borrador guardado. Solo tú puedes verlo hasta que lo envíes.");
        return "redirect:" + BASE + "/" + idBorrador + "/editar";
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

        cargarFormularioEdicion(model, planificacion, planificacion.contenido(), null);
        return VISTA_FORMULARIO;
    }

    /** Guarda el texto sin cambiar el estado (borrador o rechazada). */
    @PostMapping("/{id}/guardar")
    public String guardar(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String contenido,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            planificacionService.guardarCambios(idEstudiante, id, contenido);
        } catch (IllegalArgumentException e) {
            return volverAlFormulario(model, idEstudiante, id, contenido, e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Cambios guardados.");
        return "redirect:" + BASE + "/" + id + "/editar";
    }

    /** Envía un borrador al colaborador. No depende del asistente. */
    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String contenido,
                         @AuthenticationPrincipal UsuarioDetails usuario,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            planificacionService.enviarBorrador(idEstudiante, id, contenido);
        } catch (IllegalArgumentException e) {
            return volverAlFormulario(model, idEstudiante, id, contenido, e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación enviada al colaborador.");
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/reenviar")
    public String reenviar(@PathVariable Long id,
                           @RequestParam(defaultValue = "") String contenido,
                           @AuthenticationPrincipal UsuarioDetails usuario,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Long idEstudiante = usuario.getIdUsuario();
        try {
            planificacionService.reenviarPlanificacion(idEstudiante, id, contenido);
        } catch (IllegalArgumentException e) {
            return volverAlFormulario(model, idEstudiante, id, contenido, e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + BASE;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Planificación reenviada al colaborador.");
        return "redirect:" + BASE;
    }

    private String volverAlFormulario(Model model, Long idEstudiante, Long id, String contenido, String error) {
        PlanificacionVista planificacion = planificacionService.obtenerVistaDelEstudiante(idEstudiante, id);
        cargarFormularioEdicion(model, planificacion, contenido, error);
        return VISTA_FORMULARIO;
    }

    private void cargarFormularioNueva(Model model, Long idEstudiante, Long idLineaSeleccionada,
                                       String contenido, String error) {
        model.addAttribute("titulo", "Nueva planificación de clase");
        model.addAttribute("accionGuardar", BASE + "/borrador");
        model.addAttribute("accionEnviar", BASE);
        model.addAttribute("etiquetaGuardar", "Guardar borrador");
        model.addAttribute("etiquetaEnviar", "Enviar al profesor");
        model.addAttribute("lineas", planificacionService.obtenerLineasDelEstudiante(idEstudiante));
        model.addAttribute("idLineaSeleccionada", idLineaSeleccionada);
        model.addAttribute("contenido", contenido);
        model.addAttribute("error", error);
    }

    /** Formulario de una planificación ya guardada: un borrador (se envía) o una rechazada (se reenvía). */
    private void cargarFormularioEdicion(Model model, PlanificacionVista planificacion, String contenido, String error) {
        String base = BASE + "/" + planificacion.idPlanificacion();
        boolean borrador = planificacion.esBorrador();

        model.addAttribute("titulo", borrador ? "Editar borrador" : "Editar y reenviar planificación");
        model.addAttribute("accionGuardar", base + "/guardar");
        model.addAttribute("accionEnviar", base + (borrador ? "/enviar" : "/reenviar"));
        model.addAttribute("etiquetaGuardar", "Guardar cambios");
        model.addAttribute("etiquetaEnviar", borrador ? "Enviar al profesor" : "Reenviar al profesor");
        model.addAttribute("planificacion", planificacion);
        model.addAttribute("contenido", contenido);
        model.addAttribute("error", error);
    }
}
