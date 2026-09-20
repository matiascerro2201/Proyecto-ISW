package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {
    
    List<Planificacion> findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
            EstadoPlanificacion estado, Long idColaborador);
}