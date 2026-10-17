package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.Criterio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


public interface CriterioRepository extends JpaRepository<Criterio, Long> {
    @Query("""
            SELECT c
            FROM Criterio c
            JOIN FETCH c.dimension d
            WHERE d.idDimension IN :idsDimension
            ORDER BY c.orden
            """)
    List<Criterio> findByDimensiones(@Param("idsDimension") Collection<Long> idsDimension);
}
