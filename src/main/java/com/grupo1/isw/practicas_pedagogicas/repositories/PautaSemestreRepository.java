package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** N-04: pautas de un semestre (filas de pauta_evaluacion con id_seccion) con sus dimensiones y criterios. */
@Repository
public class PautaSemestreRepository {

    public record PautaFila(Long id, Long idSeccion, String nombre, String evaluador, String tipoLinea,
                            int aplicacion, BigDecimal ponderacion, boolean habilitada, LocalDate fechaLimite,
                            long criterios, BigDecimal puntajeMaximo) {

        public String etiquetaAplicacion() {
            return aplicacion == 2 ? "B" : "A";
        }

        public String etiquetaEvaluador() {
            return "TUTOR".equals(evaluador) ? "Tutor" : "Profesor colaborador";
        }

        public String etiquetaLinea() {
            return switch (tipoLinea) {
                case "ESPECIALIDAD" -> "Especialidad";
                case "ORIENTACION" -> "Orientación";
                default -> "Línea única";
            };
        }
    }

    public record CriterioFila(Long id, String descripcion, BigDecimal puntajeMaximo, int orden) {
    }

    public record DimensionFila(Long id, String nombre, int orden, List<CriterioFila> criterios) {
    }

    private static final String SELECT = """
            SELECT p.id_pauta_evaluacion, p.id_seccion, p.nombre, p.evaluador, p.tipo_linea, p.numero_aplicacion,
                   p.ponderacion, p.habilitada, p.fecha_limite,
                   (SELECT COUNT(*) FROM criterio c JOIN dimension d ON d.id_dimension = c.id_dimension
                     WHERE d.id_pauta_evaluacion = p.id_pauta_evaluacion),
                   (SELECT COALESCE(SUM(c.puntaje_max), 0) FROM criterio c JOIN dimension d ON d.id_dimension = c.id_dimension
                     WHERE d.id_pauta_evaluacion = p.id_pauta_evaluacion)
              FROM pauta_evaluacion p
            """;

    private final JdbcTemplate jdbc;

    public PautaSemestreRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PautaFila> listar(long idSeccion) {
        return jdbc.query(SELECT + """
                 WHERE p.id_seccion = ?
                 ORDER BY p.evaluador, p.tipo_linea, p.numero_aplicacion
                """, (rs, i) -> mapear(rs), idSeccion);
    }

    public Optional<PautaFila> buscar(long idPauta) {
        return jdbc.query(SELECT + " WHERE p.id_pauta_evaluacion = ? AND p.id_seccion IS NOT NULL",
                (rs, i) -> mapear(rs), idPauta).stream().findFirst();
    }

    public boolean existe(long idSeccion, String evaluador, String tipoLinea, int aplicacion, Long idExcluida) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pauta_evaluacion
                 WHERE id_seccion = ? AND evaluador = ? AND tipo_linea = ? AND numero_aplicacion = ?
                   AND id_pauta_evaluacion <> ?
                """, Integer.class, idSeccion, evaluador, tipoLinea, aplicacion, idExcluida == null ? -1L : idExcluida);
        return n != null && n > 0;
    }

    public long insertar(long idAsignatura, long idSeccion, String nombre, String evaluador, String tipoLinea,
                         int aplicacion, BigDecimal ponderacion, boolean habilitada, LocalDate fechaLimite) {
        return Claves.insertar(jdbc, """
                INSERT INTO pauta_evaluacion (id_asignatura, id_seccion, nombre, evaluador, tipo_linea,
                                              numero_aplicacion, ponderacion, habilitada, fecha_limite)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, idAsignatura, idSeccion, nombre, evaluador, tipoLinea, aplicacion, ponderacion, habilitada,
                fechaLimite);
    }

    public void actualizar(long idPauta, String nombre, String evaluador, String tipoLinea, int aplicacion,
                           BigDecimal ponderacion) {
        jdbc.update("""
                UPDATE pauta_evaluacion
                   SET nombre = ?, evaluador = ?, tipo_linea = ?, numero_aplicacion = ?, ponderacion = ?
                 WHERE id_pauta_evaluacion = ?
                """, nombre, evaluador, tipoLinea, aplicacion, ponderacion, idPauta);
    }

    public void cambiarHabilitada(long idPauta, boolean habilitada) {
        jdbc.update("UPDATE pauta_evaluacion SET habilitada = ? WHERE id_pauta_evaluacion = ?", habilitada, idPauta);
    }

    /** N-09: fecha límite de la pauta (null la quita). */
    public void cambiarFechaLimite(long idPauta, LocalDate fecha) {
        jdbc.update("UPDATE pauta_evaluacion SET fecha_limite = ? WHERE id_pauta_evaluacion = ?", fecha, idPauta);
    }

    public void eliminar(long idPauta) {
        jdbc.update("DELETE FROM pauta_evaluacion WHERE id_pauta_evaluacion = ?", idPauta);
    }

    public boolean tieneEvaluacionesAplicadas(long idPauta) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM evaluacion_aplicada WHERE id_pauta_evaluacion = ?", Integer.class, idPauta);
        return n != null && n > 0;
    }

    /** Evaluaciones ya aplicadas sobre cualquier pauta del semestre. */
    public long contarEvaluacionesAplicadas(long idSeccion) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM evaluacion_aplicada ea
                  JOIN pauta_evaluacion p ON p.id_pauta_evaluacion = ea.id_pauta_evaluacion
                 WHERE p.id_seccion = ?
                """, Long.class, idSeccion);
        return n == null ? 0 : n;
    }

    // ---- dimensiones y criterios

    public List<DimensionFila> dimensiones(long idPauta) {
        List<DimensionFila> resultado = new ArrayList<>();
        List<Object[]> filas = jdbc.query(
                "SELECT id_dimension, nombre, orden FROM dimension WHERE id_pauta_evaluacion = ? ORDER BY orden, id_dimension",
                (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getInt(3)}, idPauta);
        for (Object[] fila : filas) {
            Long idDimension = (Long) fila[0];
            List<CriterioFila> criterios = jdbc.query(
                    "SELECT id_criterio, descripcion, puntaje_max, orden FROM criterio WHERE id_dimension = ? ORDER BY orden, id_criterio",
                    (rs, i) -> new CriterioFila(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getInt(4)),
                    idDimension);
            resultado.add(new DimensionFila(idDimension, (String) fila[1], (Integer) fila[2], criterios));
        }
        return resultado;
    }

    /** Id de la pauta a la que pertenece una dimensión, si existe. */
    public Optional<Long> pautaDeDimension(long idDimension) {
        return jdbc.query("SELECT id_pauta_evaluacion FROM dimension WHERE id_dimension = ?",
                (rs, i) -> rs.getLong(1), idDimension).stream().findFirst();
    }

    /** Id de la dimensión a la que pertenece un criterio, si existe. */
    public Optional<Long> dimensionDeCriterio(long idCriterio) {
        return jdbc.query("SELECT id_dimension FROM criterio WHERE id_criterio = ?",
                (rs, i) -> rs.getLong(1), idCriterio).stream().findFirst();
    }

    public long insertarDimension(long idPauta, String nombre) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(orden), 0) FROM dimension WHERE id_pauta_evaluacion = ?", Integer.class, idPauta);
        return Claves.insertar(jdbc,
                "INSERT INTO dimension (id_pauta_evaluacion, nombre, orden) VALUES (?, ?, ?)",
                idPauta, nombre, (max == null ? 0 : max) + 1);
    }

    public void eliminarDimension(long idDimension) {
        jdbc.update("DELETE FROM dimension WHERE id_dimension = ?", idDimension);
    }

    public long insertarCriterio(long idDimension, String descripcion, BigDecimal puntajeMaximo) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(orden), 0) FROM criterio WHERE id_dimension = ?", Integer.class, idDimension);
        return Claves.insertar(jdbc,
                "INSERT INTO criterio (id_dimension, descripcion, puntaje_max, orden) VALUES (?, ?, ?, ?)",
                idDimension, descripcion, puntajeMaximo, (max == null ? 0 : max) + 1);
    }

    public void eliminarCriterio(long idCriterio) {
        jdbc.update("DELETE FROM criterio WHERE id_criterio = ?", idCriterio);
    }

    /** N-07: copia pautas, dimensiones y criterios de otra sección (sin fechas límite). Devuelve cuántas pautas copió. */
    public int copiarDe(long idSeccionOrigen, long idSeccionDestino, long idAsignaturaDestino) {
        int copiadas = 0;
        for (PautaFila origen : listar(idSeccionOrigen)) {
            long idNueva = insertar(idAsignaturaDestino, idSeccionDestino, origen.nombre(), origen.evaluador(),
                    origen.tipoLinea(), origen.aplicacion(), origen.ponderacion(), origen.habilitada(), null);
            for (DimensionFila dimension : dimensiones(origen.id())) {
                long idDimension = insertarDimension(idNueva, dimension.nombre());
                for (CriterioFila criterio : dimension.criterios()) {
                    insertarCriterio(idDimension, criterio.descripcion(), criterio.puntajeMaximo());
                }
            }
            copiadas++;
        }
        return copiadas;
    }

    private PautaFila mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PautaFila(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getInt(6), rs.getBigDecimal(7), rs.getBoolean(8), rs.getObject(9, LocalDate.class),
                rs.getLong(10), rs.getBigDecimal(11));
    }
}
