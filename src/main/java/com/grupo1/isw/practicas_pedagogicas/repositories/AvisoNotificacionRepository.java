package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consulta si un aviso ya existe, para no repetirlo. */
@Repository
public class AvisoNotificacionRepository {
    private final JdbcTemplate jdbc;

    public AvisoNotificacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existe(long idUsuario, String tipo, String mensaje) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notificacion WHERE id_usuario = ? AND tipo = ? AND mensaje = ?",
                Integer.class, idUsuario, tipo, mensaje);
        return cantidad != null && cantidad > 0;
    }
}
