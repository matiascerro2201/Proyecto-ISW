package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaEvaluacion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaHallazgo;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaInteraccion;
import com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.InformeAsistente;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Los análisis guardados y los datos de los que sale el informe del profesor. Las consultas devuelven filas
 * simples de toda una sección; las cifras se calculan en Java (CalculadorInforme), donde se pueden probar.
 */
public interface InformeAsistenteRepository extends JpaRepository<InformeAsistente, Long> {

    /** El análisis más reciente de una sección. */
    Optional<InformeAsistente> findFirstBySeccion_IdSeccionOrderByFechaGeneracionDescIdInformeDesc(Long idSeccion);

    long countBySeccion_IdSeccion(Long idSeccion);

    /** Análisis generados desde cierta fecha (para el límite diario). */
    long countBySeccion_IdSeccionAndFechaGeneracionGreaterThanEqual(Long idSeccion, LocalDateTime desde);

    /** Las secciones que dicta el profesor, las más recientes primero. */
    @Query("""
            SELECT s
            FROM SeccionAsignatura s
            JOIN FETCH s.asignatura a
            WHERE s.profesor.idUsuario = :idProfesor
            ORDER BY s.anio DESC, s.semestre DESC, a.nombre
            """)
    List<SeccionAsignatura> findSeccionesDelProfesor(@Param("idProfesor") Long idProfesor);

    @Query("""
            SELECT COUNT(ins)
            FROM InscripcionAsignatura ins
            WHERE ins.seccion.idSeccion = :idSeccion
            """)
    long contarEstudiantesDeLaSeccion(@Param("idSeccion") Long idSeccion);

    @Query("""
            SELECT new com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaPlanificacion(
                p.idPlanificacion, ins.estudiante.idUsuario, p.estado)
            FROM Planificacion p
            JOIN p.linea l
            JOIN l.inscripcion ins
            WHERE ins.seccion.idSeccion = :idSeccion
            """)
    List<FilaPlanificacion> filasPlanificaciones(@Param("idSeccion") Long idSeccion);

    @Query("""
            SELECT new com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaInteraccion(
                p.idPlanificacion, ins.estudiante.idUsuario, i.idInteraccion, i.fecha)
            FROM InteraccionChatbot i
            JOIN i.planificacion p
            JOIN p.linea l
            JOIN l.inscripcion ins
            WHERE ins.seccion.idSeccion = :idSeccion
            """)
    List<FilaInteraccion> filasInteracciones(@Param("idSeccion") Long idSeccion);

    @Query("""
            SELECT new com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaHallazgo(
                p.idPlanificacion, i.idInteraccion, i.fecha, c.codigo, h.nivel, h.observacion)
            FROM HallazgoChatbot h
            JOIN h.interaccion i
            JOIN h.categoria c
            JOIN i.planificacion p
            JOIN p.linea l
            JOIN l.inscripcion ins
            WHERE ins.seccion.idSeccion = :idSeccion
            """)
    List<FilaHallazgo> filasHallazgos(@Param("idSeccion") Long idSeccion);

    @Query("""
            SELECT new com.grupo1.isw.practicas_pedagogicas.dto.informe.FilaEvaluacion(
                e.idEvaluacion, p.idPlanificacion, e.decision, e.fecha)
            FROM EvaluacionPlanificacion e
            JOIN e.planificacion p
            JOIN p.linea l
            JOIN l.inscripcion ins
            WHERE ins.seccion.idSeccion = :idSeccion
            """)
    List<FilaEvaluacion> filasEvaluaciones(@Param("idSeccion") Long idSeccion);
}
