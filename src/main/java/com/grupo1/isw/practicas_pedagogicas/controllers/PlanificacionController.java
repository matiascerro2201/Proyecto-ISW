package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/planificaciones")
@PreAuthorize("hasRole('COLABORADOR')")
public class PlanificacionController {
    private static final String VISTA_PENDIENTES = "colaborador/pendientes";
    private static final String VISTA_EVALUAR = "colaborador/evaluar_planificacion";

    private final PlanificacionService planificacionService;

    public PlanificacionController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @GetMapping("/pendientes")
    public String listarPendientes(@AuthenticationPrincipal UsuarioDetails usuarioLogueado, Model model) {
        model.addAttribute("planificaciones",
                planificacionService.obtenerPendientesDelColaborador(usuarioLogueado.getIdUsuario()));
        return VISTA_PENDIENTES;
    }

    @GetMapping("/{id}/evaluar")
    public String mostrarFormularioEvaluacion(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                                              Model model) {
        Planificacion planificacion = planificacionService.obtenerPlanificacion(id);
        if (!planificacion.getLinea().getColaborador().getIdUsuario().equals(usuarioLogueado.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver esta planificación");
        }
        model.addAttribute("planificacion", planificacion);
        return VISTA_EVALUAR;
    }

    @PostMapping("/{id}/evaluar")
    public String procesarEvaluacion(@PathVariable Long id,
                                     @RequestParam(defaultValue = "") String retroalimentacion,
                                     @RequestParam EstadoPlanificacion estado,
                                     @AuthenticationPrincipal UsuarioDetails usuarioLogueado,
                                     Model model) {
        try {
            planificacionService.evaluarPlanificacion(id, usuarioLogueado.getIdUsuario(), retroalimentacion, estado);
            return "redirect:/planificaciones/" + id + "/evaluar?exito=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("planificacion", planificacionService.obtenerPlanificacion(id));
            return VISTA_EVALUAR;
        }
    }
}
