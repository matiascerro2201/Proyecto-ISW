package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeccionAsignaturaRepository extends JpaRepository<SeccionAsignatura, Long> {

    /** HU-01: asignaturas en las que el profesor tiene al menos una sección, sin repetir. */
    @Query("""
            SELECT DISTINCT a
            FROM SeccionAsignatura s
            JOIN s.asignatura a
            WHERE s.profesor.idUsuario = :idProfesor
            ORDER BY a.nombre
            """)
    List<Asignatura> findAsignaturasDelProfesor(@Param("idProfesor") Long idProfesor);

    /** HU-01: ¿el profesor imparte esta asignatura? Define quién puede gestionar sus pautas. */
    @Query("""
            SELECT COUNT(s) > 0
            FROM SeccionAsignatura s
            WHERE s.profesor.idUsuario = :idProfesor
              AND s.asignatura.idAsignatura = :idAsignatura
            """)
    boolean profesorImparte(@Param("idProfesor") Long idProfesor,
                            @Param("idAsignatura") Long idAsignatura);
}
