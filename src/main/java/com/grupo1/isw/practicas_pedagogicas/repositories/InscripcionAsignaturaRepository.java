package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;


public interface InscripcionAsignaturaRepository extends JpaRepository<InscripcionAsignatura, Long> {
    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE ins.estudiante.idUsuario = :idEstudiante
              AND EXISTS (
                    SELECT 1
                    FROM PautaEvaluacion p
                    WHERE p.asignatura.idAsignatura = sec.asignatura.idAsignatura
              )
            ORDER BY sec.anio DESC, sec.semestre DESC
            """)
    List<InscripcionAsignatura> findByEstudianteConPautas(@Param("idEstudiante") Long idEstudiante);

    @Query("""
            SELECT DISTINCT sec.asignatura
            FROM InscripcionAsignatura ins
            JOIN ins.seccion sec
            WHERE ins.estudiante.idUsuario = :idEstudiante
              AND sec.asignatura.idAsignatura = :idAsignatura
            """)
    Optional<Asignatura> findAsignaturaInscrita(@Param("idEstudiante") Long idEstudiante,
                                                @Param("idAsignatura") Long idAsignatura);

    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE ins.estudiante.idUsuario = :idEstudiante
            ORDER BY sec.anio DESC, sec.semestre DESC
            """)
    List<InscripcionAsignatura> findByEstudianteConAsignatura(@Param("idEstudiante") Long idEstudiante);
    
}
