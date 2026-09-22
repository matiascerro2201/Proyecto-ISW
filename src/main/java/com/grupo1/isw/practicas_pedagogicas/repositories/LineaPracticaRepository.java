package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LineaPracticaRepository extends JpaRepository<LineaPractica, Long> {

    /** Líneas de un colaborador, con estudiante y asignatura cargados, de la más reciente a la más antigua. */
    @Query("""
            SELECT l
            FROM LineaPractica l
            JOIN FETCH l.inscripcion ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura asig
            WHERE l.colaborador.idUsuario = :idColaborador
            ORDER BY sec.anio DESC, sec.semestre DESC, est.nombre
            """)
    List<LineaPractica> findByColaborador(@Param("idColaborador") Long idColaborador);

    /** Una línea con todo lo necesario para validar propiedad y armar pantallas (sin consultas lazy). */
    @Query("""
            SELECT l
            FROM LineaPractica l
            JOIN FETCH l.colaborador col
            JOIN FETCH l.inscripcion ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura asig
            WHERE l.idLinea = :idLinea
            """)
    Optional<LineaPractica> findByIdConContexto(@Param("idLinea") Long idLinea);

    /** Líneas de práctica de un estudiante (todas las que tenga inscritas). */
    List<LineaPractica> findByInscripcion_Estudiante_IdUsuario(Long idEstudiante);
}