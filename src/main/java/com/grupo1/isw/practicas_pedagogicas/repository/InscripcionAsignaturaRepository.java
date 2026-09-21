package com.grupo1.isw.practicas_pedagogicas.repository;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.InscripcionAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Si un compañero ya creó este repositorio, no lo dupliques: agrega estos dos métodos al suyo.
 */
@Repository
public interface InscripcionAsignaturaRepository extends JpaRepository<InscripcionAsignatura, Long> {

    /**
     * Inscripciones del estudiante cuya asignatura ya tiene al menos una pauta registrada,
     * con la asignatura cargada, de la más reciente a la más antigua.
     * Alimenta el listado /mis-pautas.
     */
    @Query("""
            SELECT ins
            FROM InscripcionAsignatura ins
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE ins.estudiante.idUsuario = :idEstudiante
              AND EXISTS (
                    SELECT 1
                    FROM PautaEvaluacion p
                    WHERE p.asignatura.idAsignatura = sec.asignatura.idAsignatura
              )
            ORDER BY sec.anio DESC, sec.semestre DESC
            """)
    List<InscripcionAsignatura> findByEstudianteConPautas(@Param("idEstudiante") Long idEstudiante);

    /**
     * Devuelve la asignatura solo si el estudiante está (o estuvo) inscrito en ella.
     * Vacío = no inscrito o asignatura inexistente. Sirve como control de acceso de HU-07.
     * DISTINCT porque el mismo estudiante podría haberla cursado en más de un período.
     */
    @Query("""
            SELECT DISTINCT sec.asignatura
            FROM InscripcionAsignatura ins
            JOIN ins.seccion sec
            WHERE ins.estudiante.idUsuario = :idEstudiante
              AND sec.asignatura.idAsignatura = :idAsignatura
            """)
    Optional<Asignatura> findAsignaturaInscrita(@Param("idEstudiante") Long idEstudiante,
                                                @Param("idAsignatura") Long idAsignatura);
}
