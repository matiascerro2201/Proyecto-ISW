package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.DetalleEvaluacionRequest;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.services.EvaluacionService;
import com.grupo1.isw.practicas_pedagogicas.services.LineaConsultaService;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HU-03: el profesor supervisor (rol TUTOR) llena las pautas de evaluación
 * directamente en el sistema, sin papel ni escaneo por parte del estudiante.
 *
 * Replica el flujo de {@link EvaluacionViewController} (HU-02), pero acotado a las
 * líneas de práctica en las que la persona autenticada figura como tutor.
 */
@Controller
@RequestMapping("/tutor/lineas")
@PreAuthorize("hasRole('TUTOR')")
public class TutorEvaluacionViewController {
    private static final Pattern CLAVE_PUNTAJE = Pattern.compile("^puntaje_(\\d{1,18})$");
    private static final Pattern CLAVE_COMENTARIO = Pattern.compile("^comentario_(\\d{1,18})$");

    private final LineaConsultaService lineaConsultaService;
    private final EvaluacionService evaluacionService;

    public TutorEvaluacionViewController(LineaConsultaService lineaConsultaService,
                                         EvaluacionService evaluacionService) {
        this.lineaConsultaService = lineaConsultaService;
        this.evaluacionService = evaluacionService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("lineas", lineaConsultaService.listarLineasDelTutor(usuario.getIdUsuario()));
        return "tutor/lineas";
    }

    @GetMapping("/{idLinea}")
    public String detalleLinea(@PathVariable Long idLinea,
                               @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("linea", lineaConsultaService.detalleLineaDelTutor(usuario.getIdUsuario(), idLinea));
        return "tutor/linea";
    }

    @GetMapping("/{idLinea}/pautas/{idPauta}")
    public String formulario(@PathVariable Long idLinea, @PathVariable Long idPauta,
                             @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("form",
                evaluacionService.formularioComoTutor(usuario.getIdUsuario(), idLinea, idPauta, null, null));
        return "tutor/evaluacion-form";
    }

    @PostMapping("/{idLinea}/pautas/{idPauta}")
    public String guardar(@PathVariable Long idLinea, @PathVariable Long idPauta,
                          @RequestParam Map<String, String> params,
                          @AuthenticationPrincipal UsuarioDetails usuario,
                          Model model, RedirectAttributes redirect) {
        Map<Long, String> puntajes = new LinkedHashMap<>();
        Map<Long, String> comentarios = new LinkedHashMap<>();
        for (Map.Entry<String, String> campo : params.entrySet()) {
            Matcher puntaje = CLAVE_PUNTAJE.matcher(campo.getKey());
            if (puntaje.matches()) {
                puntajes.put(Long.valueOf(puntaje.group(1)), campo.getValue());
                continue;
            }
            Matcher comentario = CLAVE_COMENTARIO.matcher(campo.getKey());
            if (comentario.matches()) {
                comentarios.put(Long.valueOf(comentario.group(1)), campo.getValue());
            }
        }

        List<DetalleEvaluacionRequest> detalles = puntajes.entrySet().stream()
                .map(e -> new DetalleEvaluacionRequest(
                        e.getKey(), parsearPuntaje(e.getValue()), comentarios.get(e.getKey())))
                .toList();

        Long idTutor = usuario.getIdUsuario();
        try {
            evaluacionService.guardarComoTutor(idTutor, idLinea, idPauta, detalles);
        } catch (EvaluacionInvalidaException ex) {
            model.addAttribute("form",
                    evaluacionService.formularioComoTutor(idTutor, idLinea, idPauta, puntajes, comentarios));
            model.addAttribute("errores", ex.getErrores());
            return "tutor/evaluacion-form";
        }

        redirect.addFlashAttribute("mensaje", "La evaluación se guardó correctamente.");
        return "redirect:/tutor/lineas/" + idLinea;
    }

    private static BigDecimal parsearPuntaje(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
