package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.PlanificacionArchivoService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PDF de una planificación enviada, para el colaborador de su línea. */
@RestController
@RequestMapping("/planificaciones")
@PreAuthorize("hasRole('COLABORADOR')")
public class PlanificacionArchivoColaboradorController {
    private final PlanificacionArchivoService archivoService;

    public PlanificacionArchivoColaboradorController(PlanificacionArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> ver(@PathVariable Long id, @AuthenticationPrincipal UsuarioDetails usuario) {
        return PlanificacionArchivoController.respuestaPdf(
                archivoService.obtenerParaColaborador(id, usuario.getIdUsuario()));
    }
}
