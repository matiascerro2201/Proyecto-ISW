package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// 1. Nuevos imports para la seguridad de Benjamín
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;

@Controller
@RequestMapping("/planificaciones")
@PreAuthorize("hasRole('COLABORADOR')") // 2. Protege todas las rutas de este controlador
public class PlanificacionController {

    private final PlanificacionService planificacionService;
    // 3. Eliminamos el ID_COLABORADOR_SIMULADO

    public PlanificacionController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @GetMapping("/pendientes")
    public String listarPendientes(
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado, // 4. Captura al usuario real
            Model model) {
        model.addAttribute("planificaciones",
                planificacionService.obtenerPendientesDelColaborador(usuarioLogueado.getIdUsuario())); // 5. Usa el ID real
        return "pendientes";
    }

    @GetMapping("/{id}/evaluar")
    public String mostrarFormularioEvaluacion(
            @PathVariable Long id, 
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado, // Inyecta el usuario real
            Model model) {
        try {
            Planificacion planificacion = planificacionService.obtenerPlanificacion(id);
            
            // Validación de propiedad con el ID del usuario autenticado
            if (!planificacion.getLinea().getColaborador().getIdUsuario().equals(usuarioLogueado.getIdUsuario())) {
                return "redirect:/planificaciones/pendientes"; 
            }
            
            model.addAttribute("planificacion", planificacion);
            return "evaluar_planificacion"; 
        } catch (ResponseStatusException e) {
            return "redirect:/planificaciones/pendientes";
        }
    }

    @PostMapping("/{id}/evaluar")
    public String procesarEvaluacion(
            @PathVariable Long id,
            @RequestParam String retroalimentacion,
            @RequestParam EstadoPlanificacion estado,
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado, // Inyecta el usuario real
            Model model) {
        try {
            // Pasamos el ID real al servicio
            planificacionService.evaluarPlanificacion(id, usuarioLogueado.getIdUsuario(), retroalimentacion, estado);
            return "redirect:/planificaciones/" + id + "/evaluar?exito=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("planificacion", planificacionService.obtenerPlanificacion(id));
            return "evaluar_planificacion";
        }
    }
}