package com.grupo1.isw.practicas_pedagogicas.repository;

import com.grupo1.isw.practicas_pedagogicas.models.PautaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Si Josué (HU-02) ya creó este repositorio, agrega este método al suyo en vez de duplicarlo.
 */
@Repository
public interface PautaEvaluacionRepository extends JpaRepository<PautaEvaluacion, Long> {

    /** Todas las pautas (plantillas) de una asignatura. El esquema no tiene campo de vigencia. */
    @Query("""
            SELECT p
            FROM PautaEvaluacion p
            WHERE p.asignatura.idAsignatura = :idAsignatura
            ORDER BY p.tipoLinea, p.numeroAplicacion
            """)
    List<PautaEvaluacion> findByAsignatura(@Param("idAsignatura") Long idAsignatura);
}
