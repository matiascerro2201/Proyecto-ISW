package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.CentroPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** El coordinador administra los centros de práctica (alta, edición y baja). */
public interface CentroPracticaRepository extends JpaRepository<CentroPractica, Long> {

    List<CentroPractica> findAllByOrderByNombreAsc();

    /** Profesores colaboradores que trabajan en el centro. */
    @Query("SELECT COUNT(c) FROM ProfesorColaborador c WHERE c.centro.idCentro = :idCentro")
    long contarColaboradores(@Param("idCentro") Long idCentro);

    /** Inscripciones de estudiantes que hacen su práctica en el centro. */
    @Query("SELECT COUNT(i) FROM InscripcionAsignatura i WHERE i.centro.idCentro = :idCentro")
    long contarInscripciones(@Param("idCentro") Long idCentro);
}
