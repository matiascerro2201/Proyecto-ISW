package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Las pautas del semestre que se esperan de colaboradores y tutores: una fila por estudiante, línea de práctica y
 * pauta habilitada, con la evaluación aplicada si ya existe. La pauta del tutor solo se espera si la línea tiene tutor.
 * Solo cuenta lo de semestres iniciados.
 */
@Repository
public class EvaluacionesEsperadasRepository {

    public record EvaluacionEsperada(Long idInscripcion, Long idEstudiante, String estudiante, Long idSeccion,
                                     String asignatura, int anio, int semestre, Long idLinea, String tipoLinea,
                                     Long idPauta, String pauta, String evaluador, int aplicacion,
                                     BigDecimal ponderacion, LocalDate fechaLimite, BigDecimal puntajeMaximo,
                                     Long idEvaluacion, BigDecimal puntaje, LocalDateTime fechaEvaluacion,
                                     boolean publicada, Long idEvaluador, String nombreEvaluador) {
        /** Una evaluación cuenta como aplicada cuando tiene puntaje total (igual que en el resto del sistema). */
        public boolean aplicada() {
            return idEvaluacion != null && puntaje != null;
        }

        public String periodo() {
            return "Semestre " + semestre + " de " + anio;
        }

        public String etiquetaEvaluador() {
            return "TUTOR".equals(evaluador) ? "Tutor" : "Profesor colaborador";
        }

        public String etiquetaLinea() {
            return switch (tipoLinea == null ? "" : tipoLinea) {
                case "ESPECIALIDAD" -> "Especialidad";
                case "ORIENTACION" -> "Orientación";
                default -> "Línea única";
            };
        }

        public String etiquetaAplicacion() {
            return aplicacion == 2 ? "B" : "A";
        }
    }

    public record CriterioEvaluado(String dimension, String criterio, BigDecimal maximo, BigDecimal obtenido,
                                   String comentario) {
    }

    public record ContextoEvaluacion(Long idEvaluacion, Long idInscripcion, Long idEstudiante, Long idSeccion,
                                     Long idProfesor, String pauta, BigDecimal puntaje, BigDecimal puntajeMaximo,
                                     boolean publicada) {
    }

    private static final String SELECT = """
            SELECT i.id_inscripcion, i.id_estudiante, ue.nombre, s.id_seccion, a.nombre, s.anio, s.semestre,
                   l.id_linea, l.tipo, p.id_pauta_evaluacion, p.nombre, p.evaluador, p.numero_aplicacion,
                   p.ponderacion, p.fecha_limite,
                   (SELECT COALESCE(SUM(c.puntaje_max), 0) FROM criterio c
                      JOIN dimension d ON d.id_dimension = c.id_dimension
                     WHERE d.id_pauta_evaluacion = p.id_pauta_evaluacion),
                   ev.id_evaluacion, ev.puntaje_total, ev.fecha, COALESCE(ev.publicada, 0),
                   ur.id_usuario, ur.nombre
              FROM inscripcion_asignatura i
              JOIN usuario ue ON ue.id_usuario = i.id_estudiante
              JOIN seccion_asignatura s ON s.id_seccion = i.id_seccion AND s.iniciada = 1
              JOIN asignatura a ON a.id_asignatura = s.id_asignatura
              JOIN linea_practica l ON l.id_inscripcion = i.id_inscripcion
              JOIN pauta_evaluacion p ON p.id_seccion = s.id_seccion AND p.habilitada = 1
                                     AND p.tipo_linea = l.tipo
                                     AND ((p.evaluador = 'COLABORADOR' AND l.id_colaborador IS NOT NULL)
                                          OR (p.evaluador = 'TUTOR' AND l.id_tutor IS NOT NULL))
              LEFT JOIN usuario ur ON ur.id_usuario =
                        CASE p.evaluador WHEN 'COLABORADOR' THEN l.id_colaborador ELSE l.id_tutor END
              LEFT JOIN evaluacion_aplicada ev ON ev.id_linea = l.id_linea
                                              AND ev.id_pauta_evaluacion = p.id_pauta_evaluacion
                                              AND ev.tipo_evaluador = p.evaluador
            """;
    private static final String ORDEN = " ORDER BY ue.nombre, i.id_inscripcion, p.evaluador, p.tipo_linea, "
            + "p.numero_aplicacion";

    private final JdbcTemplate jdbc;

    public EvaluacionesEsperadasRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EvaluacionEsperada> deLaSeccion(long idSeccion) {
        return jdbc.query(SELECT + " WHERE s.id_seccion = ? AND i.estado = 'EN_CURSO'" + ORDEN,
                (rs, n) -> mapear(rs), idSeccion);
    }

    public List<EvaluacionEsperada> deLaInscripcion(long idInscripcion) {
        return jdbc.query(SELECT + " WHERE i.id_inscripcion = ?" + ORDEN, (rs, n) -> mapear(rs), idInscripcion);
    }

    public List<EvaluacionEsperada> delEstudiante(long idEstudiante) {
        return jdbc.query(SELECT + " WHERE i.id_estudiante = ?" + ORDEN, (rs, n) -> mapear(rs), idEstudiante);
    }

    /** Lo que le toca aplicar a un colaborador (evaluador = COLABORADOR) o a un tutor (TUTOR). */
    public List<EvaluacionEsperada> delEvaluador(long idUsuario, String evaluador) {
        String columna = "TUTOR".equals(evaluador) ? "l.id_tutor" : "l.id_colaborador";
        String rol = "TUTOR".equals(evaluador) ? "TUTOR" : "COLABORADOR";
        return jdbc.query(SELECT + " WHERE p.evaluador = ? AND " + columna + " = ? AND i.estado = 'EN_CURSO'"
                + ORDEN, (rs, n) -> mapear(rs), rol, idUsuario);
    }

    /** Todas las pautas esperadas con plazo, de cualquier evaluador, para los avisos. */
    public List<EvaluacionEsperada> conPlazo() {
        return jdbc.query(SELECT + " WHERE p.fecha_limite IS NOT NULL AND i.estado = 'EN_CURSO'" + ORDEN,
                (rs, n) -> mapear(rs));
    }

    /** El detalle criterio por criterio de una evaluación; los criterios sin puntaje salen con obtenido null. */
    public List<CriterioEvaluado> criterios(long idEvaluacion) {
        return jdbc.query("""
                SELECT d.nombre, c.descripcion, c.puntaje_max, dc.puntaje_obtenido, dc.transcripcion_comentario
                  FROM evaluacion_aplicada ev
                  JOIN dimension d ON d.id_pauta_evaluacion = ev.id_pauta_evaluacion
                  JOIN criterio c ON c.id_dimension = d.id_dimension
                  LEFT JOIN detalle_criterio dc ON dc.id_evaluacion = ev.id_evaluacion
                                               AND dc.id_criterio = c.id_criterio
                 WHERE ev.id_evaluacion = ?
                 ORDER BY d.orden, d.id_dimension, c.orden, c.id_criterio
                """, (rs, n) -> new CriterioEvaluado(rs.getString(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getBigDecimal(4), rs.getString(5)), idEvaluacion);
    }

    public Optional<ContextoEvaluacion> contexto(long idEvaluacion) {
        return jdbc.query("""
                SELECT ev.id_evaluacion, i.id_inscripcion, i.id_estudiante, s.id_seccion, s.id_profesor,
                       p.nombre, ev.puntaje_total,
                       (SELECT COALESCE(SUM(c.puntaje_max), 0) FROM criterio c
                          JOIN dimension d ON d.id_dimension = c.id_dimension
                         WHERE d.id_pauta_evaluacion = p.id_pauta_evaluacion),
                       COALESCE(ev.publicada, 0)
                  FROM evaluacion_aplicada ev
                  JOIN linea_practica l ON l.id_linea = ev.id_linea
                  JOIN inscripcion_asignatura i ON i.id_inscripcion = l.id_inscripcion
                  JOIN seccion_asignatura s ON s.id_seccion = i.id_seccion
                  JOIN pauta_evaluacion p ON p.id_pauta_evaluacion = ev.id_pauta_evaluacion
                 WHERE ev.id_evaluacion = ?
                """, (rs, n) -> new ContextoEvaluacion(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                rs.getLong(5), rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBoolean(9)),
                idEvaluacion).stream().findFirst();
    }

    public void cambiarPublicada(long idEvaluacion, boolean publicada, LocalDateTime ahora) {
        jdbc.update("UPDATE evaluacion_aplicada SET publicada = ?, fecha_publicacion = ? WHERE id_evaluacion = ?",
                publicada, publicada ? ahora : null, idEvaluacion);
    }

    private static EvaluacionEsperada mapear(ResultSet rs) throws SQLException {
        java.sql.Date limite = rs.getDate(15);
        java.sql.Timestamp fecha = rs.getTimestamp(19);
        Long idEvaluacion = rs.getObject(17) == null ? null : rs.getLong(17);
        Long idEvaluador = rs.getObject(21) == null ? null : rs.getLong(21);
        return new EvaluacionEsperada(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getString(5),
                rs.getInt(6), rs.getInt(7), rs.getLong(8), rs.getString(9), rs.getLong(10), rs.getString(11),
                rs.getString(12), rs.getInt(13), rs.getBigDecimal(14), limite == null ? null : limite.toLocalDate(),
                rs.getBigDecimal(16), idEvaluacion, rs.getBigDecimal(18),
                fecha == null ? null : fecha.toLocalDateTime(), rs.getBoolean(20), idEvaluador, rs.getString(22));
    }
}
