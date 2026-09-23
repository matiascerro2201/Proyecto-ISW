package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionAplicadaRepository extends JpaRepository<EvaluacionAplicada, Long> {
    @Query("""
            SELECT e
            FROM EvaluacionAplicada e
            JOIN FETCH e.pautaEvaluacion p
            WHERE e.linea.idLinea = :idLinea
            ORDER BY p.numeroAplicacion, e.idEvaluacion
            """)
    List<EvaluacionAplicada> findByLinea(@Param("idLinea") Long idLinea);

    default List<EvaluacionAplicada> findDelColaboradorByLinea(Long idLinea) {
        return findByLinea(idLinea).stream()
                .filter(e -> "COLABORADOR".equals(String.valueOf(e.getTipoEvaluador())))
                .toList();
    }
}
