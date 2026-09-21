package com.grupo1.isw.practicas_pedagogicas.repository;

import com.grupo1.isw.practicas_pedagogicas.models.DetalleCriterio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DetalleCriterioRepository extends JpaRepository<DetalleCriterio, Long> {

    /** Detalles de varias evaluaciones en una consulta, en el orden de la pauta (dimensión y luego criterio). */
    @Query("""
            SELECT d
            FROM DetalleCriterio d
            JOIN FETCH d.criterio c
            JOIN FETCH c.dimension dim
            WHERE d.evaluacion.idEvaluacion IN :idsEvaluacion
            ORDER BY dim.orden, c.orden
            """)
    List<DetalleCriterio> findByEvaluaciones(@Param("idsEvaluacion") Collection<Long> idsEvaluacion);
}
