package com.grupo1.isw.practicas_pedagogicas.controllers;

import com.grupo1.isw.practicas_pedagogicas.dto.inicio.FuncionInicio;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.FuncionesDelSistema;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class FuncionesAdvice {
    @ModelAttribute("funcionesDelRol")
    public List<FuncionInicio> funcionesDelRol(@AuthenticationPrincipal UsuarioDetails usuario) {
        return usuario == null ? List.of() : FuncionesDelSistema.paraRol(usuario.getRol());
    }
}
