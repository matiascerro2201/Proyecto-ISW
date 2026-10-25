package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.PautaDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PautaDocumentoRepository extends JpaRepository<PautaDocumento, Long> {

    /** Entregas (informes, productos, proyecto de intervención) que exige una asignatura. */
    List<PautaDocumento> findByAsignatura_IdAsignaturaOrderByTipoAscNumeroAsc(Long idAsignatura);
}
