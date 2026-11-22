package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Planificacion;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDate;
import java.util.List;


public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {
    List<Planificacion> findByEstado(EstadoPlanificacion estado);
    List<Planificacion> findByEstadoAndLinea_Colaborador_IdUsuarioOrderByFechaEnvioAsc(
            EstadoPlanificacion estado, Long idColaborador);

    List<Planificacion> findByLinea_Inscripcion_Estudiante_IdUsuarioOrderByFechaEnvioDesc(Long idEstudiante);

    /** Enviadas (aún sin evaluar) cuya fecha de revisión cae en el rango y que todavía no generaron su aviso. */
    List<Planificacion> findByEstadoAndRecordatorioRevisionEnviadoFalseAndFechaRevisionSolicitadaBetween(
            EstadoPlanificacion estado, LocalDate desde, LocalDate hasta);
}
