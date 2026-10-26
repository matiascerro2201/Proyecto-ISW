package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;


public interface LineaPracticaRepository extends JpaRepository<LineaPractica, Long> {
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

    @Query("""
            SELECT l
            FROM LineaPractica l
            JOIN FETCH l.inscripcion ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura asig
            WHERE l.tutor.idUsuario = :idTutor
            ORDER BY sec.anio DESC, sec.semestre DESC, est.nombre
            """)
    List<LineaPractica> findByTutor(@Param("idTutor") Long idTutor);

    @Query("""
            SELECT l
            FROM LineaPractica l
            JOIN FETCH l.colaborador col
            LEFT JOIN FETCH l.tutor tut
            JOIN FETCH l.inscripcion ins
            JOIN FETCH ins.estudiante est
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura asig
            WHERE l.idLinea = :idLinea
            """)
    Optional<LineaPractica> findByIdConContexto(@Param("idLinea") Long idLinea);

    List<LineaPractica> findByInscripcion_Estudiante_IdUsuario(Long idEstudiante);
}
