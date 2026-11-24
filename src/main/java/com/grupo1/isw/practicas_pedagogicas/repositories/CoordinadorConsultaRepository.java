package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.Estudiante;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Consultas de solo lectura del coordinador (HU-37): ve todos los cursos y todos los estudiantes, sin el filtro
 * por profesor, colaborador o tutor que usan los demás roles. Las listas con IN reciben ids ya reunidos para
 * traer los datos en bloque y no consultar estudiante por estudiante.
 */
public interface CoordinadorConsultaRepository extends Repository<SeccionAsignatura, Long> {

    /** Por cada semestre: [año, semestre, cantidad de cursos, cantidad de inscripciones]. */
    @Query("""
            SELECT s.anio, s.semestre, COUNT(DISTINCT s.idSeccion), COUNT(ins)
            FROM SeccionAsignatura s
            LEFT JOIN InscripcionAsignatura ins ON ins.seccion = s
            GROUP BY s.anio, s.semestre
            ORDER BY s.anio DESC, s.semestre DESC
            """)
    List<Object[]> resumenPorSemestre();

    @Query("""
            SELECT s
            FROM SeccionAsignatura s
            JOIN FETCH s.asignatura a
            JOIN FETCH s.profesor p
            WHERE s.anio = :anio AND s.semestre = :semestre
            ORDER BY a.nombre
            """)
    List<SeccionAsignatura> findDelSemestre(@Param("anio") int anio, @Param("semestre") int semestre);

    @Query("""
            SELECT s
            FROM SeccionAsignatura s
            JOIN FETCH s.asignatura
            JOIN FETCH s.profesor
            WHERE s.idSeccion = :idSeccion
            """)
    Optional<SeccionAsignatura> findConAsignaturaYProfesor(@Param("idSeccion") Long idSeccion);

    /** Por cada curso: [id de la sección, cantidad de inscripciones, cantidad de informes publicados]. */
    @Query("""
            SELECT ins.seccion.idSeccion, COUNT(ins),
                   SUM(CASE WHEN inf.fechaPublicacion IS NOT NULL THEN 1 ELSE 0 END)
            FROM InscripcionAsignatura ins
            LEFT JOIN ins.informeFinal inf
            WHERE ins.seccion.idSeccion IN :idsSeccion
            GROUP BY ins.seccion.idSeccion
            """)
    List<Object[]> contarPorSeccion(@Param("idsSeccion") Collection<Long> idsSeccion);

    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            JOIN FETCH ins.centro
            LEFT JOIN FETCH ins.informeFinal
            WHERE sec.idSeccion = :idSeccion
            ORDER BY est.nombre
            """)
    List<InscripcionAsignatura> findInscripcionesDeSeccion(@Param("idSeccion") Long idSeccion);

    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            JOIN FETCH ins.centro
            LEFT JOIN FETCH ins.informeFinal
            WHERE est.idUsuario = :idEstudiante
            ORDER BY sec.anio DESC, sec.semestre DESC
            """)
    List<InscripcionAsignatura> findInscripcionesDeEstudiante(@Param("idEstudiante") Long idEstudiante);

    @Query("""
            SELECT l
            FROM LineaPractica l
            LEFT JOIN FETCH l.colaborador
            LEFT JOIN FETCH l.tutor
            WHERE l.inscripcion.idInscripcion IN :idsInscripcion
            ORDER BY l.tipo
            """)
    List<LineaPractica> findLineas(@Param("idsInscripcion") Collection<Long> idsInscripcion);

    @Query("""
            SELECT e
            FROM EvaluacionAplicada e
            JOIN FETCH e.pautaEvaluacion
            WHERE e.linea.idLinea IN :idsLinea
            """)
    List<EvaluacionAplicada> findEvaluaciones(@Param("idsLinea") Collection<Long> idsLinea);

    @Query("""
            SELECT p
            FROM Planificacion p
            WHERE p.linea.idLinea IN :idsLinea
            ORDER BY p.fechaEnvio
            """)
    List<Planificacion> findPlanificaciones(@Param("idsLinea") Collection<Long> idsLinea);

    @Query("""
            SELECT d
            FROM DocumentoEstudiante d
            JOIN FETCH d.pautaDocumento
            WHERE d.inscripcion.idInscripcion IN :idsInscripcion
            """)
    List<DocumentoEstudiante> findDocumentos(@Param("idsInscripcion") Collection<Long> idsInscripcion);

    @Query("""
            SELECT p
            FROM PautaDocumento p
            WHERE p.asignatura.idAsignatura = :idAsignatura
            ORDER BY p.tipo, p.numero
            """)
    List<PautaDocumento> findPautasDocumento(@Param("idAsignatura") Long idAsignatura);

    /** Por cada pauta de evaluación: [id de la pauta, cantidad de dimensiones, cantidad de criterios]. */
    @Query("""
            SELECT d.pautaEvaluacion.idPautaEvaluacion, COUNT(DISTINCT d), COUNT(c)
            FROM Dimension d
            LEFT JOIN Criterio c ON c.dimension = d
            WHERE d.pautaEvaluacion.idPautaEvaluacion IN :idsPauta
            GROUP BY d.pautaEvaluacion.idPautaEvaluacion
            """)
    List<Object[]> contarEstructuraDePautas(@Param("idsPauta") Collection<Long> idsPauta);

    @Query("""
            SELECT p
            FROM PautaEvaluacion p
            JOIN FETCH p.asignatura
            WHERE p.idPautaEvaluacion = :idPauta
            """)
    Optional<PautaEvaluacion> findPautaEvaluacion(@Param("idPauta") Long idPauta);

    @Query("""
            SELECT p
            FROM PautaDocumento p
            JOIN FETCH p.asignatura
            WHERE p.idPautaDocumento = :idPauta
            """)
    Optional<PautaDocumento> findPautaDocumento(@Param("idPauta") Long idPauta);

    @Query("SELECT e FROM Estudiante e ORDER BY e.nombre")
    List<Estudiante> findEstudiantes();

    @Query("SELECT e FROM Estudiante e WHERE e.idUsuario = :idEstudiante")
    Optional<Estudiante> findEstudiante(@Param("idEstudiante") Long idEstudiante);

    /** Por cada estudiante con inscripciones: [id del estudiante, cantidad de cursos]. */
    @Query("""
            SELECT ins.estudiante.idUsuario, COUNT(ins)
            FROM InscripcionAsignatura ins
            GROUP BY ins.estudiante.idUsuario
            """)
    List<Object[]> contarInscripcionesPorEstudiante();
}
