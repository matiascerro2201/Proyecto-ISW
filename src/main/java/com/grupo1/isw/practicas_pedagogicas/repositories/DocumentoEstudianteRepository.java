package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.DocumentoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoEstudianteRepository extends JpaRepository<DocumentoEstudiante, Long> {

    /** Todo lo que el estudiante ha subido en una inscripción. */
    List<DocumentoEstudiante> findByInscripcion_IdInscripcion(Long idInscripcion);

    /**
     * La entrega de una pauta concreta dentro de una inscripción.
     * Con la regla "volver a subir reemplaza", debe existir como máximo una por par.
     */
    Optional<DocumentoEstudiante> findByInscripcion_IdInscripcionAndPautaDocumento_IdPautaDocumento(
            Long idInscripcion, Long idPautaDocumento);
}
