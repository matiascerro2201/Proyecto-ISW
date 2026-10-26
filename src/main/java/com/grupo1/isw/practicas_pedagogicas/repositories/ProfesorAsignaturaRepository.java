package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Consultas del profesor sobre inscripciones e informes de sus secciones (HU-10; la reutiliza HU-11).
 *  Todas traen cargado el profesor de la sección, para verificar que el recurso es suyo. */
public interface ProfesorAsignaturaRepository extends JpaRepository<ProfesorAsignatura, Long> {

    /** Inscripciones de las secciones que dicta el profesor, con su informe si ya existe. */
    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            LEFT JOIN FETCH ins.informeFinal
            WHERE sec.profesor.idUsuario = :idProfesor
            ORDER BY sec.anio DESC, sec.semestre DESC, est.nombre
            """)
    List<InscripcionAsignatura> findInscripcionesDelProfesor(@Param("idProfesor") Long idProfesor);

    /** Una inscripción con el profesor de su sección y su informe (si existe), para generarlo. */
    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            JOIN FETCH sec.profesor
            LEFT JOIN FETCH ins.informeFinal
            WHERE ins.idInscripcion = :idInscripcion
            """)
    Optional<InscripcionAsignatura> findInscripcionConProfesor(@Param("idInscripcion") Long idInscripcion);

    /** Un informe con el profesor de su sección, para que solo ese profesor pueda verlo o modificarlo. */
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
    Optional<InformeFinal> findInformeConProfesor(@Param("idInforme") Long idInforme);
}
