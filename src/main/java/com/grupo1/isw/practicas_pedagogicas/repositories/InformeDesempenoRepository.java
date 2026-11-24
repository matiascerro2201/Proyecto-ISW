package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** N-19: datos del informe de desempeño (tabla informe_final) y de las entregas calificadas de un estudiante. */
@Repository
public class InformeDesempenoRepository {

    public record DatosInscripcion(Long idInscripcion, Long idEstudiante, String estudiante, String email,
                                   Long idSeccion, String asignatura, int anio, int semestre, Long idProfesor,
                                   String estado, boolean iniciada) {
        public String periodo() {
            return "Semestre " + semestre + " de " + anio;
        }
    }

    public record EntregaNota(Long idEntrega, String nombre, BigDecimal ponderacion, LocalDate fechaLimite,
                              Long idRegistro, boolean tieneArchivo, BigDecimal nota, boolean automatica,
                              boolean publicada) {
    }

    public record InformeFila(Long idInforme, Long idInscripcion, BigDecimal sugerida, BigDecimal finalNota,
                              LocalDateTime generado, LocalDateTime validado, LocalDateTime publicado) {
        public String estado() {
            if (publicado != null) {
                return "PUBLICADO";
            }
            if (validado != null) {
                return "VALIDADO";
            }
            return "GENERADO";
        }
    }

    private static final String SELECT_INSCRIPCION = """
            SELECT i.id_inscripcion, i.id_estudiante, ue.nombre, ue.email, s.id_seccion, a.nombre, s.anio,
                   s.semestre, s.id_profesor, i.estado, s.iniciada
              FROM inscripcion_asignatura i
              JOIN usuario ue ON ue.id_usuario = i.id_estudiante
              JOIN seccion_asignatura s ON s.id_seccion = i.id_seccion
              JOIN asignatura a ON a.id_asignatura = s.id_asignatura
            """;

    private final JdbcTemplate jdbc;

    public InformeDesempenoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DatosInscripcion> inscripcion(long idInscripcion) {
        return jdbc.query(SELECT_INSCRIPCION + " WHERE i.id_inscripcion = ?", (rs, n) -> new DatosInscripcion(
                rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getLong(5), rs.getString(6),
                rs.getInt(7), rs.getInt(8), rs.getLong(9), rs.getString(10), rs.getBoolean(11)), idInscripcion)
                .stream().findFirst();
    }

    public List<DatosInscripcion> inscripcionesDeLaSeccion(long idSeccion) {
        return jdbc.query(SELECT_INSCRIPCION + " WHERE s.id_seccion = ? AND i.estado = 'EN_CURSO' ORDER BY ue.nombre",
                (rs, n) -> new DatosInscripcion(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getLong(5), rs.getString(6), rs.getInt(7), rs.getInt(8), rs.getLong(9), rs.getString(10),
                        rs.getBoolean(11)), idSeccion);
    }

    /** Cada entrega habilitada del semestre con lo que el estudiante hizo (registro null si todavía no hay nada). */
    public List<EntregaNota> entregas(long idInscripcion) {
        return jdbc.query("""
                SELECT e.id_entrega, e.nombre, e.ponderacion, e.fecha_limite, x.id_entrega_estudiante,
                       x.contenido_url IS NOT NULL, x.nota, COALESCE(x.nota_automatica, 0), COALESCE(x.publicada, 0)
                  FROM inscripcion_asignatura i
                  JOIN entrega_seccion e ON e.id_seccion = i.id_seccion AND e.habilitada = 1
                  LEFT JOIN entrega_estudiante x ON x.id_inscripcion = i.id_inscripcion AND x.id_entrega = e.id_entrega
                 WHERE i.id_inscripcion = ?
                 ORDER BY e.orden, e.id_entrega
                """, (rs, n) -> {
            java.sql.Date limite = rs.getDate(4);
            return new EntregaNota(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3),
                    limite == null ? null : limite.toLocalDate(), rs.getObject(5) == null ? null : rs.getLong(5),
                    rs.getBoolean(6), rs.getBigDecimal(7), rs.getBoolean(8), rs.getBoolean(9));
        }, idInscripcion);
    }

    public Optional<InformeFila> informe(long idInscripcion) {
        return jdbc.query(SELECT_INFORME + " WHERE id_inscripcion = ?", (rs, n) -> mapear(rs), idInscripcion)
                .stream().findFirst();
    }

    public List<InformeFila> informesDeLaSeccion(long idSeccion) {
        return jdbc.query(SELECT_INFORME + " WHERE id_inscripcion IN "
                + "(SELECT id_inscripcion FROM inscripcion_asignatura WHERE id_seccion = ?)",
                (rs, n) -> mapear(rs), idSeccion);
    }

    private static final String SELECT_INFORME = """
            SELECT id_informe, id_inscripcion, calificacion_sugerida, calificacion_final, fecha_generacion,
                   fecha_validacion, fecha_publicacion
              FROM informe_final
            """;

    private static InformeFila mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InformeFila(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3), rs.getBigDecimal(4),
                fecha(rs.getTimestamp(5)), fecha(rs.getTimestamp(6)), fecha(rs.getTimestamp(7)));
    }

    private static LocalDateTime fecha(java.sql.Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    /** Crea el informe o actualiza la nota sugerida del que ya existe (solo si todavía no se valida ni publica). */
    public void guardarSugerida(long idInscripcion, long idCorrector, BigDecimal sugerida, LocalDateTime ahora) {
        int filas = jdbc.update("""
                UPDATE informe_final SET calificacion_sugerida = ?, id_corrector = ?, fecha_generacion = ?
                 WHERE id_inscripcion = ?
                """, sugerida, idCorrector, ahora, idInscripcion);
        if (filas == 0) {
            jdbc.update("""
                    INSERT INTO informe_final (id_inscripcion, id_corrector, calificacion_sugerida, fecha_generacion)
                    VALUES (?, ?, ?, ?)
                    """, idInscripcion, idCorrector, sugerida, ahora);
        }
    }

    public void guardarFinal(long idInscripcion, BigDecimal nota, LocalDateTime ahora) {
        jdbc.update("UPDATE informe_final SET calificacion_final = ?, fecha_validacion = ? WHERE id_inscripcion = ?",
                nota, ahora, idInscripcion);
    }

    public void publicar(long idInscripcion, LocalDateTime ahora) {
        jdbc.update("UPDATE informe_final SET fecha_publicacion = ? WHERE id_inscripcion = ?", ahora, idInscripcion);
    }
}
