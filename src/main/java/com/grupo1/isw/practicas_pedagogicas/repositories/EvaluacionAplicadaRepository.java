package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.EvaluacionAplicada;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface EvaluacionAplicadaRepository extends JpaRepository<EvaluacionAplicada, Long> {
    @Query("""
            SELECT e
            FROM EvaluacionAplicada e
            JOIN FETCH e.pautaEvaluacion p
            WHERE e.linea.idLinea = :idLinea
            ORDER BY p.numeroAplicacion, e.idEvaluacion
            """)
    List<EvaluacionAplicada> findByLinea(@Param("idLinea") Long idLinea);

    default List<EvaluacionAplicada> findByLineaYEvaluador(Long idLinea, TipoEvaluador tipoEvaluador) {
        return findByLinea(idLinea).stream()
                .filter(e -> tipoEvaluador == e.getTipoEvaluador())
                .toList();
    }

    default List<EvaluacionAplicada> findDelColaboradorByLinea(Long idLinea) {
        return findByLineaYEvaluador(idLinea, TipoEvaluador.COLABORADOR);
    }

    default List<EvaluacionAplicada> findDelTutorByLinea(Long idLinea) {
        return findByLineaYEvaluador(idLinea, TipoEvaluador.TUTOR);
    }

    /** HU-10: todas las evaluaciones de una inscripción (cualquier línea y evaluador), con su pauta. */
    @Query("""
            SELECT e
            FROM EvaluacionAplicada e
            JOIN FETCH e.pautaEvaluacion p
            WHERE e.linea.inscripcion.idInscripcion = :idInscripcion
            ORDER BY p.tipoLinea, p.numeroAplicacion, e.idEvaluacion
            """)
    List<EvaluacionAplicada> findByInscripcion(@Param("idInscripcion") Long idInscripcion);
    
    /** HU-01: ¿la pauta ya tiene evaluaciones aplicadas? Si es así, no se puede modificar. */
    @Query("""
            SELECT COUNT(e) > 0
            FROM EvaluacionAplicada e
            WHERE e.pautaEvaluacion.idPautaEvaluacion = :idPauta
            """)
    boolean existsByPauta(@Param("idPauta") Long idPauta);
}
