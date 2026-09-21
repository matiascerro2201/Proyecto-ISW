package com.grupo1.isw.practicas_pedagogicas.repository;

import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DimensionRepository extends JpaRepository<Dimension, Long> {

    /** Dimensiones de varias pautas en una sola consulta, ordenadas por el campo orden. */
    @Query("""
            SELECT d
            FROM Dimension d
            JOIN FETCH d.pautaEvaluacion p
            WHERE p.idPautaEvaluacion IN :idsPauta
            ORDER BY d.orden
            """)
    List<Dimension> findByPautas(@Param("idsPauta") Collection<Long> idsPauta);
}
