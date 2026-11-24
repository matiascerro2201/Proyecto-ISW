package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** N-02 a N-08: datos de una sección (semestre) y acciones sobre su estado. */
@Repository
public class SeccionEstructuraRepository {

    /** Una sección con lo que las pantallas de estructura necesitan saber de ella. */
    public record SeccionDatos(Long id, Long idAsignatura, String asignatura, int numeroLineas, int anio,
                               int semestre, Long idProfesor, String profesor, boolean iniciada,
                               LocalDateTime fechaInicio) {
        public String periodo() {
            return "Semestre " + semestre + " de " + anio;
        }

        public String titulo() {
            return asignatura + " · " + periodo();
        }
    }

    private static final String SELECT_SECCION = """
            SELECT s.id_seccion, s.id_asignatura, a.nombre, a.numero_lineas, s.anio, s.semestre,
                   s.id_profesor, u.nombre, s.iniciada, s.fecha_inicio
              FROM seccion_asignatura s
              JOIN asignatura a ON a.id_asignatura = s.id_asignatura
              JOIN usuario u ON u.id_usuario = s.id_profesor
            """;

    private final JdbcTemplate jdbc;

    public SeccionEstructuraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SeccionDatos> buscar(long idSeccion) {
        return jdbc.query(SELECT_SECCION + " WHERE s.id_seccion = ?", (rs, i) -> mapear(rs), idSeccion)
                .stream().findFirst();
    }

    public List<SeccionDatos> delProfesor(long idProfesor) {
        return jdbc.query(SELECT_SECCION + " WHERE s.id_profesor = ? ORDER BY s.anio DESC, s.semestre DESC, a.nombre",
                (rs, i) -> mapear(rs), idProfesor);
    }

    /** Otras secciones de la misma asignatura que ya tienen alguna entrega o pauta, la más reciente primero. */
    public List<SeccionDatos> conEstructuraDeLaAsignatura(long idAsignatura, long idSeccionExcluida) {
        return jdbc.query(SELECT_SECCION + """
                 WHERE s.id_asignatura = ? AND s.id_seccion <> ?
                   AND (EXISTS (SELECT 1 FROM entrega_seccion e WHERE e.id_seccion = s.id_seccion)
                     OR EXISTS (SELECT 1 FROM pauta_evaluacion p WHERE p.id_seccion = s.id_seccion))
                 ORDER BY s.anio DESC, s.semestre DESC
                """, (rs, i) -> mapear(rs), idAsignatura, idSeccionExcluida);
    }

    public void marcarIniciada(long idSeccion, LocalDateTime fecha) {
        jdbc.update("UPDATE seccion_asignatura SET iniciada = 1, fecha_inicio = ? WHERE id_seccion = ?",
                fecha, idSeccion);
    }

    private SeccionDatos mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SeccionDatos(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getInt(5),
                rs.getInt(6), rs.getLong(7), rs.getString(8), rs.getBoolean(9),
                rs.getObject(10, LocalDateTime.class));
    }
}
