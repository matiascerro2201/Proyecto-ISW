package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CursoDetalle;
import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.services.CoordinadorCursoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * HU-37 y HU-38: el botón "Cursos" del coordinador. Primero la lista de semestres (año y semestre), después los
 * cursos del semestre con buscador y filtros, y por último el curso con sus pautas y estudiantes.
 */
@Controller
@RequestMapping("/coordinador/cursos")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorCursoController {
    private static final String BASE = "/coordinador/cursos";

    private final CoordinadorCursoService cursoService;

    public CoordinadorCursoController(CoordinadorCursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public String semestres(Model model) {
        model.addAttribute("semestres", cursoService.semestres());
        return "coordinador/cursos/semestres";
    }

    @GetMapping("/semestre/{anio}/{semestre}")
    public String cursos(@PathVariable int anio,
                         @PathVariable int semestre,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) Ciclo ciclo,
                         Model model) {
        model.addAttribute("anio", anio);
        model.addAttribute("semestre", semestre);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("ciclo", ciclo);
        model.addAttribute("ciclos", Ciclo.values());
        model.addAttribute("cursos", cursoService.cursosDelSemestre(anio, semestre, q, ciclo));
        return "coordinador/cursos/lista";
    }

    @GetMapping("/seccion/{idSeccion}")
    public String detalle(@PathVariable Long idSeccion, Model model) {
        CursoDetalle curso = cursoService.detalle(idSeccion);
        model.addAttribute("curso", curso);
        return "coordinador/cursos/detalle";
    }

    @PostMapping("/seccion/{idSeccion}/pautas-evaluacion/{idPauta}/habilitada")
    public String habilitarPautaEvaluacion(@PathVariable Long idSeccion,
                                           @PathVariable Long idPauta,
                                           @RequestParam boolean habilitada,
                                           RedirectAttributes redirect) {
        try {
            cursoService.cambiarHabilitacionPautaEvaluacion(idSeccion, idPauta, habilitada);
            redirect.addFlashAttribute("mensaje", habilitada ? "Pauta habilitada." : "Pauta deshabilitada.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "/seccion/" + idSeccion + "#pautas";
    }

    @PostMapping("/seccion/{idSeccion}/pautas-documento/{idPauta}/habilitada")
    public String habilitarPautaDocumento(@PathVariable Long idSeccion,
                                          @PathVariable Long idPauta,
                                          @RequestParam boolean habilitada,
                                          RedirectAttributes redirect) {
        try {
            cursoService.cambiarHabilitacionPautaDocumento(idSeccion, idPauta, habilitada);
            redirect.addFlashAttribute("mensaje", habilitada ? "Entrega habilitada." : "Entrega deshabilitada.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "/seccion/" + idSeccion + "#entregas";
    }
}
