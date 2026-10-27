package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.CategoriaPlanificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaPlanificacionRepository extends JpaRepository<CategoriaPlanificacion, Long> {

    /** Los aspectos vigentes, en el orden en que se presentan (son los que se le piden al asistente). */
    List<CategoriaPlanificacion> findByActivaTrueOrderByOrdenAsc();
}
