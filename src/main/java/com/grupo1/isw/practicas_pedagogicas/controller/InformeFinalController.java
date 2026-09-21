package com.grupo1.isw.practicas_pedagogicas.controller;

import com.grupo1.isw.practicas_pedagogicas.dto.InformeFinalResponse;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.service.InformeFinalService;
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

    /**
     * GET /api/informes-finales/{idInforme}
     *
     * Sin sesión           -> redirige a /login
     * Rol distinto a ESTUDIANTE -> 403
     * 200 -> el informe existe, es del estudiante autenticado y está publicado
     * 403 -> el informe pertenece a otro estudiante
     * 404 -> el informe no existe o aún no está publicado
     */
    @GetMapping("/{idInforme}")
    public ResponseEntity<InformeFinalResponse> obtenerInformeFinal(
            @PathVariable Long idInforme,
            @AuthenticationPrincipal UsuarioDetails usuarioLogueado) {

        InformeFinalResponse respuesta = informeFinalService
                .obtenerInformeParaEstudiante(idInforme, usuarioLogueado.getIdUsuario());

        return ResponseEntity.ok(respuesta);
    }
}