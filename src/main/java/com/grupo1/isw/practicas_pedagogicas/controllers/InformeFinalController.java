package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeFinalResponse;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.InformeFinalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/informes-finales")
@PreAuthorize("hasRole('ESTUDIANTE')")
public class InformeFinalController {
    private final InformeFinalService informeFinalService;

    public InformeFinalController(InformeFinalService informeFinalService) {
        this.informeFinalService = informeFinalService;
    }

    @GetMapping("/{idInforme}")
    public ResponseEntity<InformeFinalResponse> obtenerInformeFinal(
            @PathVariable Long idInforme,
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado) {
        InformeFinalResponse respuesta = informeFinalService
                .obtenerInformeParaEstudiante(idInforme, usuarioLogueado.getIdUsuario());

        return ResponseEntity.ok(respuesta);
    }
}
