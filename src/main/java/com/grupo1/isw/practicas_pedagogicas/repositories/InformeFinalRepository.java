package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;


public interface InformeFinalRepository extends JpaRepository<InformeFinal, Long> {
    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE i.idInforme = :idInforme
            """)
    Optional<InformeFinal> findByIdConPropietario(@Param("idInforme") Long idInforme);

    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE ins.estudiante.idUsuario = :idEstudiante
              AND i.fechaPublicacion IS NOT NULL
            ORDER BY i.fechaPublicacion DESC
            """)
    List<InformeFinal> findPublicadosByEstudiante(@Param("idEstudiante") Long idEstudiante);

    /** HU-10: un informe con el profesor de su sección, para que solo ese profesor pueda verlo. */
    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            JOIN FETCH sec.profesor
            WHERE i.idInforme = :idInforme
            """)
    Optional<InformeFinal> findByIdParaProfesor(@Param("idInforme") Long idInforme);

    /** HU-11: informe con todo lo necesario para decidir si el profesor puede revisarlo. */
    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            JOIN FETCH sec.profesor
            JOIN FETCH i.corrector
            WHERE i.idInforme = :idInforme
            """)
    Optional<InformeFinal> findByIdParaRevision(@Param("idInforme") Long idInforme);

    /** HU-11: informes de las secciones del profesor (o que tiene asignados como corrector). */
    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE sec.profesor.idUsuario = :idProfesor
               OR i.corrector.idUsuario = :idProfesor
            ORDER BY i.fechaGeneracion DESC, i.idInforme DESC
            """)
    List<InformeFinal> findParaRevisionDelProfesor(@Param("idProfesor") Long idProfesor);

    /**
     * HU-14: informes publicados de los estudiantes que supervisa un tutor, es decir, de las inscripciones
     * donde él figura como tutor en alguna línea. DISTINCT: una inscripción con dos líneas del mismo tutor
     * (especialidad y orientación) no debe repetir su informe.
     */
    @Query("""
            SELECT DISTINCT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            JOIN ins.lineas lin
            WHERE lin.tutor.idUsuario = :idTutor
              AND i.fechaPublicacion IS NOT NULL
            ORDER BY i.fechaPublicacion DESC, i.idInforme DESC
            """)
    List<InformeFinal> findPublicadosDelTutor(@Param("idTutor") Long idTutor);
}
