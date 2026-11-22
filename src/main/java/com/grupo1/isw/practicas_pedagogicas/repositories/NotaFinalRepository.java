package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** N-23: la nota final de cada inscripción del estudiante, solo si el profesor ya publicó el informe de desempeño. */
@Repository
public class NotaFinalRepository {

    public record NotaFinal(BigDecimal nota, LocalDateTime publicadaEl) {
    }

    private final JdbcTemplate jdbc;

    public NotaFinalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Clave: id de la inscripción. */
    public Map<Long, NotaFinal> publicadas(long idEstudiante) {
        Map<Long, NotaFinal> notas = new HashMap<>();
        jdbc.query("""
                SELECT f.id_inscripcion, f.calificacion_final, f.fecha_publicacion
                  FROM informe_final f
                  JOIN inscripcion_asignatura i ON i.id_inscripcion = f.id_inscripcion
                 WHERE i.id_estudiante = ? AND f.fecha_publicacion IS NOT NULL AND f.calificacion_final IS NOT NULL
                """, rs -> {
            java.sql.Timestamp fecha = rs.getTimestamp(3);
            notas.put(rs.getLong(1), new NotaFinal(rs.getBigDecimal(2), fecha == null ? null : fecha.toLocalDateTime()));
        }, idEstudiante);
        return notas;
    }
}
