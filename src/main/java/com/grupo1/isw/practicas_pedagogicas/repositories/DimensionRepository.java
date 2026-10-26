package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;


public interface DimensionRepository extends JpaRepository<Dimension, Long> {
    @Query("""
            SELECT d
            FROM Dimension d
            JOIN FETCH d.pautaEvaluacion p
            WHERE p.idPautaEvaluacion IN :idsPauta
            ORDER BY d.orden
            """)
    List<Dimension> findByPautas(@Param("idsPauta") Collection<Long> idsPauta);
}
