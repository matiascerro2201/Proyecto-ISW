package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * N-10 a N-15: lo que cada estudiante entregó en cada entrega del semestre y la nota que recibió
 * (tabla entrega_estudiante, creada en V15).
 */
@Repository
public class EntregaEstudianteRepository {

    /** Una entrega del semestre vista por el estudiante, con lo que él hizo en ella (si hizo algo). */
    public record EntregaDelEstudiante(Long idInscripcion, String estadoInscripcion, Long idSeccion,
                                       String asignatura, int anio, int semestre, Long idEntrega, String nombre,
                                       BigDecimal ponderacion, LocalDate fechaLimite, Long idRegistro,
                                       boolean tieneArchivo, LocalDateTime fechaSubida, BigDecimal nota,
                                       String comentario, boolean automatica, boolean publicada) {
        public String periodo() {
            return "Semestre " + semestre + " de " + anio;
        }
    }

    /** Todo lo necesario para decidir si una acción sobre (inscripción, entrega) está permitida. */
    public record ContextoEntrega(Long idInscripcion, Long idEstudiante, String estadoInscripcion,
                                  Long idSeccionInscripcion, boolean seccionIniciada, Long idSeccionEntrega,
                                  String nombreEntrega, boolean entregaHabilitada, LocalDate fechaLimite,
                                  Long idRegistro, String archivo, LocalDateTime fechaSubida, BigDecimal nota,
                                  String comentario, boolean automatica, boolean publicada) {
        public boolean tieneArchivo() {
            return archivo != null;
        }
    }

    /** Un estudiante inscrito y su estado en una entrega, para la pantalla del profesor. */
    public record FilaEstudianteEntrega(Long idInscripcion, String estudiante, String email, Long idRegistro,
                                        boolean tieneArchivo, LocalDateTime fechaSubida, BigDecimal nota,
                                        String comentario, boolean automatica, boolean publicada) {
        /** Si hay un registro (archivo o nota automática) el profesor ya puede ponerle nota. */
        public boolean puedeCalificar() {
            return idRegistro != null;
        }
    }

    public record ResumenEntrega(Long idEntrega, String nombre, BigDecimal ponderacion, LocalDate fechaLimite,
                                 int inscritos, int entregaron, int publicadas) {
        public int faltan() {
            return inscritos - entregaron;
        }
    }

    private static final String CAMPOS_CONTEXTO = """
            SELECT i.id_inscripcion, i.id_estudiante, i.estado, i.id_seccion, s.iniciada,
                   e.id_seccion, e.nombre, e.habilitada, e.fecha_limite,
                   x.id_entrega_estudiante, x.contenido_url, x.fecha_subida, x.nota, x.comentario,
                   x.nota_automatica, x.publicada
              FROM inscripcion_asignatura i
              JOIN seccion_asignatura s ON s.id_seccion = i.id_seccion
              JOIN entrega_seccion e ON e.id_entrega = ?
              LEFT JOIN entrega_estudiante x ON x.id_inscripcion = i.id_inscripcion AND x.id_entrega = e.id_entrega
             WHERE i.id_inscripcion = ?
            """;

    private final JdbcTemplate jdbc;

    public EntregaEstudianteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- estudiante

    /** Entregas habilitadas de los semestres ya iniciados en los que el estudiante está inscrito. */
    public List<EntregaDelEstudiante> delEstudiante(long idEstudiante) {
        return jdbc.query("""
                SELECT i.id_inscripcion, i.estado, s.id_seccion, a.nombre, s.anio, s.semestre,
                       e.id_entrega, e.nombre, e.ponderacion, e.fecha_limite,
                       x.id_entrega_estudiante, x.contenido_url, x.fecha_subida, x.nota, x.comentario,
                       x.nota_automatica, x.publicada
                  FROM inscripcion_asignatura i
                  JOIN seccion_asignatura s ON s.id_seccion = i.id_seccion AND s.iniciada = 1
                  JOIN asignatura a ON a.id_asignatura = s.id_asignatura
                  JOIN entrega_seccion e ON e.id_seccion = s.id_seccion AND e.habilitada = 1
                  LEFT JOIN entrega_estudiante x ON x.id_inscripcion = i.id_inscripcion AND x.id_entrega = e.id_entrega
                 WHERE i.id_estudiante = ?
                 ORDER BY s.anio DESC, s.semestre DESC, a.nombre, i.id_inscripcion, e.orden, e.id_entrega
                """, (rs, n) -> new EntregaDelEstudiante(rs.getLong(1), rs.getString(2), rs.getLong(3),
                rs.getString(4), rs.getInt(5), rs.getInt(6), rs.getLong(7), rs.getString(8), rs.getBigDecimal(9),
                rs.getObject(10, LocalDate.class), idONull(rs, 11), rs.getString(12) != null,
                rs.getObject(13, LocalDateTime.class), rs.getBigDecimal(14), rs.getString(15), rs.getBoolean(16),
                rs.getBoolean(17)), idEstudiante);
    }

    public Optional<ContextoEntrega> contexto(long idInscripcion, long idEntrega) {
        return jdbc.query(CAMPOS_CONTEXTO, (rs, n) -> new ContextoEntrega(rs.getLong(1), rs.getLong(2),
                rs.getString(3), rs.getLong(4), rs.getBoolean(5), rs.getLong(6), rs.getString(7), rs.getBoolean(8),
                rs.getObject(9, LocalDate.class), idONull(rs, 10), rs.getString(11),
                rs.getObject(12, LocalDateTime.class), rs.getBigDecimal(13), rs.getString(14), rs.getBoolean(15),
                rs.getBoolean(16)), idEntrega, idInscripcion).stream().findFirst();
    }

    public void insertarArchivo(long idInscripcion, long idEntrega, String archivo, LocalDateTime fecha) {
        jdbc.update("""
                INSERT INTO entrega_estudiante (id_inscripcion, id_entrega, contenido_url, fecha_subida)
                VALUES (?, ?, ?, ?)
                """, idInscripcion, idEntrega, archivo, fecha);
    }

    public void reemplazarArchivo(long idRegistro, String archivo, LocalDateTime fecha) {
        jdbc.update("UPDATE entrega_estudiante SET contenido_url = ?, fecha_subida = ? WHERE id_entrega_estudiante = ?",
                archivo, fecha, idRegistro);
    }

    // ---- N-11 nota automática

    /**
     * Pone 1,0 (sin publicar) a quien no entregó una entrega cuyo plazo ya venció, en semestres iniciados.
     * Con {@code idSeccion} null revisa todos los semestres. Devuelve cuántas notas creó.
     */
    public int crearNotasAutomaticas(LocalDate hoy, LocalDateTime ahora, Long idSeccion) {
        return jdbc.update("""
                INSERT INTO entrega_estudiante (id_inscripcion, id_entrega, nota, nota_automatica, publicada, fecha_nota)
                SELECT i.id_inscripcion, e.id_entrega, 1.0, 1, 0, ?
                  FROM entrega_seccion e
                  JOIN seccion_asignatura s ON s.id_seccion = e.id_seccion AND s.iniciada = 1
                  JOIN inscripcion_asignatura i ON i.id_seccion = s.id_seccion AND i.estado = 'EN_CURSO'
                 WHERE e.habilitada = 1 AND e.fecha_limite IS NOT NULL AND e.fecha_limite < ?
                   AND (? IS NULL OR s.id_seccion = ?)
                   AND NOT EXISTS (SELECT 1 FROM entrega_estudiante x
                                    WHERE x.id_inscripcion = i.id_inscripcion AND x.id_entrega = e.id_entrega)
                """, ahora, hoy, idSeccion, idSeccion);
    }

    /** Si se extiende o se quita un plazo, las notas automáticas por no entregar dejan de corresponder. */
    public int eliminarNotasAutomaticas(long idEntrega) {
        return jdbc.update("""
                DELETE FROM entrega_estudiante
                 WHERE id_entrega = ? AND nota_automatica = 1 AND contenido_url IS NULL
                """, idEntrega);
    }

    // ---- profesor

    public List<ResumenEntrega> resumenDeLaSeccion(long idSeccion) {
        return jdbc.query("""
                SELECT e.id_entrega, e.nombre, e.ponderacion, e.fecha_limite,
                       (SELECT COUNT(*) FROM inscripcion_asignatura i
                         WHERE i.id_seccion = e.id_seccion AND i.estado = 'EN_CURSO'),
                       (SELECT COUNT(*) FROM entrega_estudiante x
                          JOIN inscripcion_asignatura i ON i.id_inscripcion = x.id_inscripcion
                         WHERE x.id_entrega = e.id_entrega AND i.estado = 'EN_CURSO' AND x.contenido_url IS NOT NULL),
                       (SELECT COUNT(*) FROM entrega_estudiante x
                          JOIN inscripcion_asignatura i ON i.id_inscripcion = x.id_inscripcion
                         WHERE x.id_entrega = e.id_entrega AND i.estado = 'EN_CURSO' AND x.publicada = 1)
                  FROM entrega_seccion e
                 WHERE e.id_seccion = ? AND e.habilitada = 1
                 ORDER BY e.orden, e.id_entrega
                """, (rs, n) -> new ResumenEntrega(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getObject(4, LocalDate.class), rs.getInt(5), rs.getInt(6), rs.getInt(7)), idSeccion);
    }

    public List<FilaEstudianteEntrega> estudiantesDeLaEntrega(long idSeccion, long idEntrega) {
        return jdbc.query("""
                SELECT i.id_inscripcion, u.nombre, u.email, x.id_entrega_estudiante, x.contenido_url, x.fecha_subida,
                       x.nota, x.comentario, x.nota_automatica, x.publicada
                  FROM inscripcion_asignatura i
                  JOIN usuario u ON u.id_usuario = i.id_estudiante
                  LEFT JOIN entrega_estudiante x ON x.id_inscripcion = i.id_inscripcion AND x.id_entrega = ?
                 WHERE i.id_seccion = ? AND i.estado = 'EN_CURSO'
                 ORDER BY u.nombre
                """, (rs, n) -> new FilaEstudianteEntrega(rs.getLong(1), rs.getString(2), rs.getString(3),
                idONull(rs, 4), rs.getString(5) != null, rs.getObject(6, LocalDateTime.class), rs.getBigDecimal(7),
                rs.getString(8), rs.getBoolean(9), rs.getBoolean(10)), idEntrega, idSeccion);
    }

    /** Guarda la nota y el comentario. Una nota puesta por el profesor ya no es "automática". */
    public void guardarNota(long idRegistro, BigDecimal nota, String comentario, LocalDateTime fecha) {
        jdbc.update("""
                UPDATE entrega_estudiante
                   SET nota = ?, comentario = ?, nota_automatica = 0, fecha_nota = ?
                 WHERE id_entrega_estudiante = ?
                """, nota, comentario, fecha, idRegistro);
    }

    public void cambiarPublicada(long idRegistro, boolean publicada) {
        jdbc.update("UPDATE entrega_estudiante SET publicada = ? WHERE id_entrega_estudiante = ?", publicada, idRegistro);
    }

    private static Long idONull(java.sql.ResultSet rs, int columna) throws java.sql.SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }
}
