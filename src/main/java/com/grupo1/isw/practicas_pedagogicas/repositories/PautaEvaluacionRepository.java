package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface PautaEvaluacionRepository extends JpaRepository<PautaEvaluacion, Long> {
    @Query("""
            SELECT p
            FROM PautaEvaluacion p
            WHERE p.asignatura.idAsignatura = :idAsignatura
            ORDER BY p.tipoLinea, p.numeroAplicacion
            """)
    List<PautaEvaluacion> findByAsignatura(@Param("idAsignatura") Long idAsignatura);
}
