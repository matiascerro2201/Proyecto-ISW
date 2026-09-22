package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluacionAplicadaRepository extends JpaRepository<EvaluacionAplicada, Long> {

    /** Todas las evaluaciones de una línea (de cualquier evaluador), con su pauta cargada. */
    @Query("""
            SELECT e
            FROM EvaluacionAplicada e
            JOIN FETCH e.pautaEvaluacion p
            WHERE e.linea.idLinea = :idLinea
            ORDER BY p.numeroAplicacion, e.idEvaluacion
            """)
    List<EvaluacionAplicada> findByLinea(@Param("idLinea") Long idLinea);

    /**
     * Solo las evaluaciones hechas por el COLABORADOR (no las del tutor). El filtro se hace en
     * memoria comparando el texto del enum: así no dependemos de dónde esté declarado ese enum.
     */
    default List<EvaluacionAplicada> findDelColaboradorByLinea(Long idLinea) {
        return findByLinea(idLinea).stream()
                .filter(e -> "COLABORADOR".equals(String.valueOf(e.getTipoEvaluador())))
                .toList();
    }
}
