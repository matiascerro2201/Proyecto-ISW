package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.LineaPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LineaPracticaRepository extends JpaRepository<LineaPractica, Long> {
    List<LineaPractica> findByInscripcion_Estudiante_IdUsuario(Long idEstudiante);
}