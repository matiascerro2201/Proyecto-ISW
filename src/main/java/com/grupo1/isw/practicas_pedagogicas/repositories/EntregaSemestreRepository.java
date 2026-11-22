package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** N-03: entregas de un semestre (tabla entrega_seccion). */
@Repository
public class EntregaSemestreRepository {

    public record EntregaFila(Long id, Long idSeccion, String nombre, BigDecimal ponderacion,
                              boolean habilitada, LocalDate fechaLimite, int orden) {
    }

    private static final String SELECT = """
            SELECT id_entrega, id_seccion, nombre, ponderacion, habilitada, fecha_limite, orden
              FROM entrega_seccion
            """;

    private final JdbcTemplate jdbc;

    public EntregaSemestreRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EntregaFila> listar(long idSeccion) {
        return jdbc.query(SELECT + " WHERE id_seccion = ? ORDER BY orden, id_entrega", (rs, i) -> mapear(rs), idSeccion);
    }

    public Optional<EntregaFila> buscar(long idEntrega) {
        return jdbc.query(SELECT + " WHERE id_entrega = ?", (rs, i) -> mapear(rs), idEntrega).stream().findFirst();
    }

    public boolean existeNombre(long idSeccion, String nombre, Long idExcluida) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM entrega_seccion
                 WHERE id_seccion = ? AND LOWER(nombre) = LOWER(?) AND id_entrega <> ?
                """, Integer.class, idSeccion, nombre, idExcluida == null ? -1L : idExcluida);
        return n != null && n > 0;
    }

    public int siguienteOrden(long idSeccion) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(orden), 0) FROM entrega_seccion WHERE id_seccion = ?", Integer.class, idSeccion);
        return (max == null ? 0 : max) + 1;
    }

    public long insertar(long idSeccion, String nombre, BigDecimal ponderacion, boolean habilitada, int orden) {
        return Claves.insertar(jdbc, """
                INSERT INTO entrega_seccion (id_seccion, nombre, ponderacion, habilitada, orden)
                VALUES (?, ?, ?, ?, ?)
                """, idSeccion, nombre, ponderacion, habilitada, orden);
    }

    public void actualizar(long idEntrega, String nombre, BigDecimal ponderacion) {
        jdbc.update("UPDATE entrega_seccion SET nombre = ?, ponderacion = ? WHERE id_entrega = ?",
                nombre, ponderacion, idEntrega);
    }

    public void cambiarHabilitada(long idEntrega, boolean habilitada) {
        jdbc.update("UPDATE entrega_seccion SET habilitada = ? WHERE id_entrega = ?", habilitada, idEntrega);
    }

    /** N-09: fecha límite de la entrega (null la quita). */
    public void cambiarFechaLimite(long idEntrega, LocalDate fecha) {
        jdbc.update("UPDATE entrega_seccion SET fecha_limite = ? WHERE id_entrega = ?", fecha, idEntrega);
    }

    public void eliminar(long idEntrega) {
        jdbc.update("DELETE FROM entrega_seccion WHERE id_entrega = ?", idEntrega);
    }

    public boolean tieneEntregasDeEstudiantes(long idEntrega) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM entrega_estudiante WHERE id_entrega = ?", Integer.class, idEntrega);
        return n != null && n > 0;
    }

    /** Cuántas entregas de estudiantes hay en todo el semestre (para saber si ya se aplicó algo). */
    public long contarEntregasDeEstudiantes(long idSeccion) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM entrega_estudiante ee
                  JOIN entrega_seccion e ON e.id_entrega = ee.id_entrega
                 WHERE e.id_seccion = ?
                """, Long.class, idSeccion);
        return n == null ? 0 : n;
    }

    /** N-07: copia las entregas de otra sección (sin fechas límite). Devuelve cuántas copió. */
    public int copiarDe(long idSeccionOrigen, long idSeccionDestino) {
        return jdbc.update("""
                INSERT INTO entrega_seccion (id_seccion, nombre, ponderacion, habilitada, orden)
                SELECT ?, nombre, ponderacion, habilitada, orden FROM entrega_seccion WHERE id_seccion = ?
                """, idSeccionDestino, idSeccionOrigen);
    }

    private EntregaFila mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new EntregaFila(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getBigDecimal(4),
                rs.getBoolean(5), rs.getObject(6, LocalDate.class), rs.getInt(7));
    }
}
