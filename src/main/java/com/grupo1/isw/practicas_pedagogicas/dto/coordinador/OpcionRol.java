package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;

/** Un rol para elegir en un selector. */
public record OpcionRol(RolUsuario valor, String etiqueta) {
}
