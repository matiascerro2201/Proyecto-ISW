package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.InteraccionChatbot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InteraccionChatbotRepository extends JpaRepository<InteraccionChatbot, Long> {

    /**
     * La conversación de una planificación desde cierta fecha, en orden cronológico. Sirve para mostrarla
     * y para armar lo que se le envía al asistente: en una planificación rechazada solo cuenta lo
     * conversado desde el rechazo actual (el texto anterior ya no existe).
     */
    List<InteraccionChatbot> findByPlanificacion_IdPlanificacionAndFechaGreaterThanEqualOrderByFechaAscIdInteraccionAsc(
            Long idPlanificacion, LocalDateTime desde);

    /** Mensajes que un estudiante envió al asistente desde cierta fecha (para el límite diario). */
    long countByPlanificacion_Linea_Inscripcion_Estudiante_IdUsuarioAndFechaGreaterThanEqual(
            Long idEstudiante, LocalDateTime desde);
}
