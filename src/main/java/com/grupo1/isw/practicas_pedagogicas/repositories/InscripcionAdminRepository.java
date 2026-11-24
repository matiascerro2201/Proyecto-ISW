package com.grupo1.isw.practicas_pedagogicas.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** N-02: inscripciones de estudiantes en una sección y sus líneas de práctica (colaborador y tutor). */
@Repository
public class InscripcionAdminRepository {

    public record LineaFila(Long id, String tipo, Long idColaborador, String colaborador,
                            Long idTutor, String tutor) {
        public String etiquetaTipo() {
            return switch (tipo) {
                case "ESPECIALIDAD" -> "Especialidad";
                case "ORIENTACION" -> "Orientación";
                default -> "Línea única";
            };
        }
    }

    public record InscripcionFila(Long id, Long idSeccion, Long idEstudiante, String estudiante, String email,
                                  Long idCentro, String centro, String estado, List<LineaFila> lineas) {
    }

    /** Una persona o un centro para un menú desplegable. */
    public record Opcion(Long id, String etiqueta) {
    }

    private final JdbcTemplate jdbc;

    public InscripcionAdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<InscripcionFila> listar(long idSeccion) {
        return consultar("WHERE i.id_seccion = ?", idSeccion);
    }

    public Optional<InscripcionFila> buscar(long idInscripcion) {
        return consultar("WHERE i.id_inscripcion = ?", idInscripcion).stream().findFirst();
    }

    private List<InscripcionFila> consultar(String filtro, Object parametro) {
        List<Object[]> base = jdbc.query("""
                SELECT i.id_inscripcion, i.id_seccion, i.id_estudiante, ue.nombre, ue.email,
                       i.id_centro, c.nombre, i.estado
                  FROM inscripcion_asignatura i
                  JOIN usuario ue ON ue.id_usuario = i.id_estudiante
                  JOIN centro_practica c ON c.id_centro = i.id_centro
                """ + filtro + " ORDER BY ue.nombre",
                (rs, n) -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getString(7), rs.getString(8)}, parametro);

        Map<Long, List<LineaFila>> lineasPorInscripcion = new LinkedHashMap<>();
        for (Object[] fila : base) {
            lineasPorInscripcion.put((Long) fila[0], new ArrayList<>());
        }
        if (!base.isEmpty()) {
            String marcas = String.join(",", base.stream().map(f -> "?").toList());
            jdbc.query("""
                    SELECT l.id_inscripcion, l.id_linea, l.tipo, l.id_colaborador, uc.nombre, l.id_tutor, ut.nombre
                      FROM linea_practica l
                      LEFT JOIN usuario uc ON uc.id_usuario = l.id_colaborador
                      LEFT JOIN usuario ut ON ut.id_usuario = l.id_tutor
                     WHERE l.id_inscripcion IN (""" + marcas + ") ORDER BY l.tipo",
                    rs -> {
                        long idInscripcion = rs.getLong(1);
                        long idColaborador = rs.getLong(4);
                        Long colaborador = rs.wasNull() ? null : idColaborador;
                        long idTutor = rs.getLong(6);
                        Long tutor = rs.wasNull() ? null : idTutor;
                        lineasPorInscripcion.get(idInscripcion).add(new LineaFila(rs.getLong(2), rs.getString(3),
                                colaborador, rs.getString(5), tutor, rs.getString(7)));
                    },
                    base.stream().map(f -> f[0]).toArray());
        }

        List<InscripcionFila> resultado = new ArrayList<>();
        for (Object[] f : base) {
            resultado.add(new InscripcionFila((Long) f[0], (Long) f[1], (Long) f[2], (String) f[3], (String) f[4],
                    (Long) f[5], (String) f[6], (String) f[7], lineasPorInscripcion.get((Long) f[0])));
        }
        return resultado;
    }

    public List<Opcion> estudiantesSinInscribir(long idSeccion) {
        return jdbc.query("""
                SELECT u.id_usuario, CONCAT(u.nombre, ' (', u.email, ')') FROM usuario u
                 WHERE u.rol = 'ESTUDIANTE' AND u.activo = 1
                   AND NOT EXISTS (SELECT 1 FROM inscripcion_asignatura i
                                    WHERE i.id_estudiante = u.id_usuario AND i.id_seccion = ?)
                 ORDER BY u.nombre
                """, (rs, i) -> new Opcion(rs.getLong(1), rs.getString(2)), idSeccion);
    }

    public List<Opcion> personasActivas(String rol) {
        return jdbc.query("""
                SELECT id_usuario, CONCAT(nombre, ' (', email, ')') FROM usuario
                 WHERE rol = ? AND activo = 1 ORDER BY nombre
                """, (rs, i) -> new Opcion(rs.getLong(1), rs.getString(2)), rol);
    }

    public List<Opcion> centros() {
        return jdbc.query("""
                SELECT id_centro, CASE WHEN comuna IS NULL OR comuna = '' THEN nombre
                                       ELSE CONCAT(nombre, ' · ', comuna) END
                  FROM centro_practica ORDER BY nombre
                """, (rs, i) -> new Opcion(rs.getLong(1), rs.getString(2)));
    }

    public boolean existePersonaActiva(long idUsuario, String rol) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE id_usuario = ? AND rol = ? AND activo = 1",
                Integer.class, idUsuario, rol);
        return n != null && n > 0;
    }

    public boolean existeCentro(long idCentro) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM centro_practica WHERE id_centro = ?", Integer.class, idCentro);
        return n != null && n > 0;
    }

    public boolean estaInscrito(long idEstudiante, long idSeccion) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inscripcion_asignatura WHERE id_estudiante = ? AND id_seccion = ?",
                Integer.class, idEstudiante, idSeccion);
        return n != null && n > 0;
    }

    public long insertarInscripcion(long idEstudiante, long idSeccion, long idCentro) {
        return Claves.insertar(jdbc, """
                INSERT INTO inscripcion_asignatura (id_estudiante, id_seccion, id_centro, estado)
                VALUES (?, ?, ?, 'EN_CURSO')
                """, idEstudiante, idSeccion, idCentro);
    }

    public void cambiarCentro(long idInscripcion, long idCentro) {
        jdbc.update("UPDATE inscripcion_asignatura SET id_centro = ? WHERE id_inscripcion = ?", idCentro, idInscripcion);
    }

    /**
     * Crea la línea o, si el estudiante ya tiene una de ese tipo, le cambia colaborador y tutor. Ambos son
     * opcionales: hay asignaturas sin profesor colaborador y líneas sin tutor.
     */
    public void guardarLinea(long idInscripcion, String tipo, Long idColaborador, Long idTutor) {
        int actualizadas = jdbc.update("""
                UPDATE linea_practica SET id_colaborador = ?, id_tutor = ?
                 WHERE id_inscripcion = ? AND tipo = ?
                """, idColaborador, idTutor, idInscripcion, tipo);
        if (actualizadas == 0) {
            jdbc.update("""
                    INSERT INTO linea_practica (id_inscripcion, id_colaborador, id_tutor, tipo)
                    VALUES (?, ?, ?, ?)
                    """, idInscripcion, idColaborador, idTutor, tipo);
        }
    }

    /** Si el estudiante ya tiene notas, evaluaciones, entregas o informe, no se puede quitar de la sección. */
    public boolean tieneDatosEvaluativos(long idInscripcion) {
        Integer n = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM documento_estudiante WHERE id_inscripcion = ?)
                     + (SELECT COUNT(*) FROM entrega_estudiante WHERE id_inscripcion = ?)
                     + (SELECT COUNT(*) FROM informe_final WHERE id_inscripcion = ?)
                     + (SELECT COUNT(*) FROM evaluacion_aplicada ea
                          JOIN linea_practica l ON l.id_linea = ea.id_linea WHERE l.id_inscripcion = ?)
                     + (SELECT COUNT(*) FROM planificacion p
                          JOIN linea_practica l ON l.id_linea = p.id_linea WHERE l.id_inscripcion = ?)
                """, Integer.class, idInscripcion, idInscripcion, idInscripcion, idInscripcion, idInscripcion);
        return n != null && n > 0;
    }

    public void eliminar(long idInscripcion) {
        jdbc.update("DELETE FROM inscripcion_asignatura WHERE id_inscripcion = ?", idInscripcion);
    }
}
