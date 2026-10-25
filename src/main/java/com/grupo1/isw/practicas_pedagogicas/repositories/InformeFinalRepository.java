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
}
