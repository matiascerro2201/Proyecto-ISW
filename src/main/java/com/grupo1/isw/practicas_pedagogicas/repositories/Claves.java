package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;

/** Inserta una fila con JdbcTemplate y devuelve el id que generó la base de datos. */
final class Claves {
    private Claves() {
    }

    static long insertar(JdbcTemplate jdbc, String sql, Object... argumentos) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < argumentos.length; i++) {
                ps.setObject(i + 1, argumentos[i]);
            }
            return ps;
        }, claves);
        Number clave = claves.getKey();
        if (clave == null) {
            throw new IllegalStateException("La base de datos no devolvió el id de la fila creada");
        }
        return clave.longValue();
    }
}
