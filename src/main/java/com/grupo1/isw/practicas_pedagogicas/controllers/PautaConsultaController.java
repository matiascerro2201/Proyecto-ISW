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

@RestController
@RequestMapping("/api/pautas")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class PautaConsultaController {
    private final PautaConsultaService pautaConsultaService;

    public PautaConsultaController(PautaConsultaService pautaConsultaService) {
        this.pautaConsultaService = pautaConsultaService;
    }

    @GetMapping("/asignatura/{idAsignatura}")
    public ResponseEntity<PautasAsignaturaResponse> consultarPautasDeAsignatura(
            @PathVariable Long idAsignatura,
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado) {
        return ResponseEntity.ok(
                pautaConsultaService.consultarPautasDeAsignatura(idAsignatura, usuarioLogueado.getIdUsuario()));
    }
}
