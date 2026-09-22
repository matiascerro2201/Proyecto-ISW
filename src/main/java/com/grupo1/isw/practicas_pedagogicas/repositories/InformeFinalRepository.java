package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.InformeFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InformeFinalRepository extends JpaRepository<InformeFinal, Long> {

    /**
     * Busca el informe cargando en la misma consulta el camino hacia su dueño
     * (InformeFinal -> InscripcionAsignatura -> Estudiante) y hacia la asignatura
     * que se muestra en pantalla (Inscripcion -> Seccion -> Asignatura).
     * Así el service valida la propiedad y arma el DTO sin consultas adicionales (lazy loading).
     */
    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.estudiante
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE i.idInforme = :idInforme
            """)
    Optional<InformeFinal> findByIdConPropietario(@Param("idInforme") Long idInforme);

    /**
     * Informes PUBLICADOS (fecha_publicacion no nula) de un estudiante, del más reciente al más antiguo.
     * El filtro por estudiante y por publicación se hace en la consulta, no en memoria.
     */
    @Query("""
            SELECT i
            FROM InformeFinal i
            JOIN FETCH i.inscripcion ins
            JOIN FETCH ins.seccion sec
            JOIN FETCH sec.asignatura
            WHERE ins.estudiante.idUsuario = :idEstudiante
              AND i.fechaPublicacion IS NOT NULL
            ORDER BY i.fechaPublicacion DESC
            """)
    List<InformeFinal> findPublicadosByEstudiante(@Param("idEstudiante") Long idEstudiante);
}
