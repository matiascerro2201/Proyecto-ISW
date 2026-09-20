package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/planificaciones")
public class PlanificacionController {

    private final PlanificacionService planificacionService;
    //reemplazar por el usuario autenticado cuando exista el login
    private static final Long ID_COLABORADOR_SIMULADO = 1L;

    public PlanificacionController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @GetMapping("/pendientes")
    public String listarPendientes(Model model) {
        model.addAttribute("planificaciones",
                planificacionService.obtenerPendientesDelColaborador(ID_COLABORADOR_SIMULADO));
        return "pendientes";
    }

    @GetMapping("/{id}/evaluar")
    public String mostrarFormularioEvaluacion(@PathVariable Long id, Model model) {
        try {
            Planificacion planificacion = planificacionService.obtenerPlanificacion(id);
            
            // Parche de seguridad propuesto por Claude para el GET
            if (!planificacion.getLinea().getColaborador().getIdUsuario().equals(ID_COLABORADOR_SIMULADO)) {
                return "redirect:/planificaciones/pendientes"; // Lo expulsa si no es suyo
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
            Model model) {
        try {
            planificacionService.evaluarPlanificacion(id, ID_COLABORADOR_SIMULADO, retroalimentacion, estado);
            return "redirect:/planificaciones/" + id + "/evaluar?exito=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("planificacion", planificacionService.obtenerPlanificacion(id));
            return "evaluar_planificacion";
        }
    }
}