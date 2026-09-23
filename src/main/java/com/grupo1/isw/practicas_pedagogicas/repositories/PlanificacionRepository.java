package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {
    List<Planificacion> findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
            EstadoPlanificacion estado, Long idColaborador);

    List<Planificacion> findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(Long idEstudiante);
}
