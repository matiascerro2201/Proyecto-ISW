package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.EntregaEstudianteService;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionesEstudianteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** N-20 a N-23: la tabla de evaluaciones del estudiante, con entrega desde la tabla y detalle de cada nota. */
@Controller
@RequestMapping("/estudiante/evaluaciones")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class EvaluacionesEstudianteController {
    private static final String TABLA = "redirect:/estudiante/evaluaciones";

    private final EvaluacionesEstudianteService servicio;
    private final EntregaEstudianteService entregaService;

    public EvaluacionesEstudianteController(EvaluacionesEstudianteService servicio,
                                            EntregaEstudianteService entregaService) {
        this.servicio = servicio;
        this.entregaService = entregaService;
    }

    @GetMapping
    public String tabla(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("grupos", servicio.tabla(usuario.getIdUsuario()));
        return "estudiante/evaluaciones/tabla";
    }

    /** N-21: entregar (o reemplazar) el PDF desde la misma tabla; las reglas de plazo son las de N-10. */
    @PostMapping("/entregas/{idInscripcion}/{idEntrega}")
    public String entregar(@PathVariable Long idInscripcion, @PathVariable Long idEntrega,
                           @RequestParam(required = false) MultipartFile archivo,
                           @AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        try {
            boolean reemplazo = entregaService.subir(usuario.getIdUsuario(), idInscripcion, idEntrega, archivo);
            redirect.addFlashAttribute("mensaje",
                    reemplazo ? "Entrega reemplazada correctamente." : "Entrega subida correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return TABLA;
    }

    @GetMapping("/entregas/{idInscripcion}/{idEntrega}")
    public String detalleEntrega(@PathVariable Long idInscripcion, @PathVariable Long idEntrega,
                                 @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("detalle", servicio.detalleEntrega(usuario.getIdUsuario(), idInscripcion, idEntrega));
        return "estudiante/evaluaciones/entrega";
    }

    @GetMapping("/pautas/{idEvaluacion}")
    public String detallePauta(@PathVariable Long idEvaluacion, @AuthenticationPrincipal UsuarioDetails usuario,
                               Model model) {
        model.addAttribute("detalle", servicio.detallePauta(usuario.getIdUsuario(), idEvaluacion));
        return "estudiante/evaluaciones/pauta";
    }
}
