package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;

public record UsuarioResumen(Long id, String nombre, String email, RolUsuario rol, boolean activo) {
    public String rolEtiqueta() {
        return Etiquetas.rol(rol);
    }
}
