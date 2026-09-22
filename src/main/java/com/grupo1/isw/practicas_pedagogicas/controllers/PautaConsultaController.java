package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.PautasAsignaturaResponse;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PautaConsultaService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-07 (JSON). Solo lectura: únicamente hay un @GetMapping, no existe POST/PUT/DELETE aquí.
 * Comparte el prefijo /api/pautas con el controlador de escritura de HU-02 (Josué); no chocan
 * porque las rutas y los métodos HTTP son distintos.
 */
@RestController
@RequestMapping("/api/pautas")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PautaConsultaController {

    private final PautaConsultaService pautaConsultaService;

    public PautaConsultaController(PautaConsultaService pautaConsultaService) {
        this.pautaConsultaService = pautaConsultaService;
    }

    /**
     * GET /api/pautas/asignatura/{idAsignatura}
     *
     * Sin sesión                -> redirige a /login
     * Rol distinto a ESTUDIANTE -> 403
     * 200 -> el estudiante está inscrito en la asignatura (lista vacía si aún no hay pautas)
     * 403 -> no está inscrito en esa asignatura (o no existe)
     */
    @GetMapping("/asignatura/{idAsignatura}")
    public ResponseEntity<PautasAsignaturaResponse> consultarPautasDeAsignatura(
            @PathVariable Long idAsignatura,
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado) {

        return ResponseEntity.ok(
                pautaConsultaService.consultarPautasDeAsignatura(idAsignatura, usuarioLogueado.getIdUsuario()));
    }
}
