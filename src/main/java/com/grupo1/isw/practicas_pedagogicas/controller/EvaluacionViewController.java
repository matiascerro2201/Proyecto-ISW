package com.grupo1.isw.practicas_pedagogicas.controller;

import com.grupo1.isw.practicas_pedagogicas.dto.DetalleEvaluacionRequest;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.service.EvaluacionInvalidaException;
import com.grupo1.isw.practicas_pedagogicas.service.EvaluacionService;
import com.grupo1.isw.practicas_pedagogicas.service.LineaConsultaService;
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
 * HU-02 (solo Thymeleaf): las vistas llaman directo al service; no hay endpoints JSON.
 *
 *   /colaborador/lineas                            -> mis estudiantes (líneas)
 *   /colaborador/lineas/{idLinea}                  -> pautas de esa línea y su estado
 *   /colaborador/lineas/{idLinea}/pautas/{idPauta} -> formulario (GET) y guardado (POST)
 *
 * El formulario usa th:action, que agrega el token CSRF solo. Un mismo POST crea la evaluación
 * la primera vez y la actualiza las siguientes. Los errores 403/404 los renderiza templates/error/*.html.
 */
@Controller
@RequestMapping("/colaborador/lineas")
@PreAuthorize("hasRole('COLABORADOR')")
public class EvaluacionViewController {

    private static final Pattern CLAVE_PUNTAJE = Pattern.compile("^puntaje_(\\d{1,18})$");
    private static final Pattern CLAVE_COMENTARIO = Pattern.compile("^comentario_(\\d{1,18})$");

    private final LineaConsultaService lineaConsultaService;
    private final EvaluacionService evaluacionService;

    public EvaluacionViewController(LineaConsultaService lineaConsultaService,
                                    EvaluacionService evaluacionService) {
        this.lineaConsultaService = lineaConsultaService;
        this.evaluacionService = evaluacionService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("lineas", lineaConsultaService.listarLineasDelColaborador(usuario.getIdUsuario()));
        return "colaborador/lineas";                 // templates/colaborador/lineas.html
    }

    @GetMapping("/{idLinea}")
    public String detalleLinea(@PathVariable Long idLinea,
                               @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("linea", lineaConsultaService.detalleLinea(usuario.getIdUsuario(), idLinea));
        return "colaborador/linea";                  // templates/colaborador/linea.html
    }

    @GetMapping("/{idLinea}/pautas/{idPauta}")
    public String formulario(@PathVariable Long idLinea, @PathVariable Long idPauta,
                             @AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("form",
                evaluacionService.formulario(usuario.getIdUsuario(), idLinea, idPauta, null, null));
        return "colaborador/evaluacion-form";        // templates/colaborador/evaluacion-form.html
    }

    /**
     * Guarda la evaluación. Los campos del formulario se llaman puntaje_{idCriterio} y comentario_{idCriterio}.
     * Si hay errores de validación, vuelve al formulario con lo que el usuario escribió y la lista de errores.
     */
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

        Long idColaborador = usuario.getIdUsuario();
        try {
            evaluacionService.guardar(idColaborador, idLinea, idPauta, detalles);
        } catch (EvaluacionInvalidaException ex) {
            model.addAttribute("form",
                    evaluacionService.formulario(idColaborador, idLinea, idPauta, puntajes, comentarios));
            model.addAttribute("errores", ex.getErrores());
            return "colaborador/evaluacion-form";
        }

        redirect.addFlashAttribute("mensaje", "La evaluación se guardó correctamente.");
        return "redirect:/colaborador/lineas/" + idLinea;
    }

    /** Acepta "4.5" y "4,5". Vacío o ilegible -> null (el servicio informa "falta el puntaje"). */
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
