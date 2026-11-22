package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.Asignatura;
import com.grupo1.isw.practicas_pedagogicas.models.ProfesorAsignatura;
import com.grupo1.isw.practicas_pedagogicas.models.SeccionAsignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** N-01: consultas del coordinador para administrar asignaturas y secciones. */
public interface AsignaturaAdminRepository extends JpaRepository<Asignatura, Long> {

    List<Asignatura> findAllByOrderByNombreAsc();

    /** Para detectar nombres repetidos (la colación de la base tampoco distingue mayúsculas ni tildes). */
    Optional<Asignatura> findByNombreIgnoreCase(String nombre);

    /** Por asignatura: [id, cantidad de secciones]. */
    @Query("""
            SELECT s.asignatura.idAsignatura, COUNT(s)
            FROM SeccionAsignatura s
            GROUP BY s.asignatura.idAsignatura
            """)
    List<Object[]> contarSeccionesPorAsignatura();

    /** Por asignatura: [id, cantidad de inscripciones de todas sus secciones]. */
    @Query("""
            SELECT ins.seccion.asignatura.idAsignatura, COUNT(ins)
            FROM InscripcionAsignatura ins
            GROUP BY ins.seccion.asignatura.idAsignatura
            """)
    List<Object[]> contarInscripcionesPorAsignatura();

    /** Por sección: [id, cantidad de inscripciones]. */
    @Query("""
            SELECT ins.seccion.idSeccion, COUNT(ins)
            FROM InscripcionAsignatura ins
            GROUP BY ins.seccion.idSeccion
            """)
    List<Object[]> contarInscripcionesPorSeccion();

    @Query("SELECT COUNT(s) FROM SeccionAsignatura s WHERE s.asignatura.idAsignatura = :idAsignatura")
    long contarSecciones(@Param("idAsignatura") Long idAsignatura);

    @Query("""
            SELECT COUNT(ins)
            FROM InscripcionAsignatura ins
            WHERE ins.seccion.asignatura.idAsignatura = :idAsignatura
            """)
    long contarInscripcionesDeAsignatura(@Param("idAsignatura") Long idAsignatura);

    @Query("SELECT COUNT(ins) FROM InscripcionAsignatura ins WHERE ins.seccion.idSeccion = :idSeccion")
    long contarInscripcionesDeSeccion(@Param("idSeccion") Long idSeccion);

    /** ¿Existe otra sección con la misma asignatura, año y semestre? Pasar -1 en idExcluida si es una sección nueva. */
    @Query("""
            SELECT COUNT(s) > 0
            FROM SeccionAsignatura s
            WHERE s.asignatura.idAsignatura = :idAsignatura
              AND s.anio = :anio
              AND s.semestre = :semestre
              AND s.idSeccion <> :idExcluida
            """)
    boolean existeSeccion(@Param("idAsignatura") Long idAsignatura,
                          @Param("anio") Integer anio,
                          @Param("semestre") Integer semestre,
                          @Param("idExcluida") Long idExcluida);

    @Query("""
            SELECT s
            FROM SeccionAsignatura s
            JOIN FETCH s.asignatura a
            JOIN FETCH s.profesor
            ORDER BY s.anio DESC, s.semestre DESC, a.nombre
            """)
    List<SeccionAsignatura> findSeccionesConDetalle();

    @Query("""
            SELECT s
            FROM SeccionAsignatura s
            JOIN FETCH s.asignatura
            JOIN FETCH s.profesor
            WHERE s.idSeccion = :idSeccion
            """)
    Optional<SeccionAsignatura> findSeccionConDetalle(@Param("idSeccion") Long idSeccion);

    /** Profesores que se pueden asignar a una sección: solo cuentas activas. */
    @Query("SELECT p FROM ProfesorAsignatura p WHERE p.activo = true ORDER BY p.nombre")
    List<ProfesorAsignatura> findProfesoresActivos();
}
