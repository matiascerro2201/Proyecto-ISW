package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.HallazgoChatbot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Los hallazgos se guardan junto con su interacción (cascada desde InteraccionChatbot). Las consultas
 * de agregación para el informe del profesor se agregan aquí cuando se construya esa parte.
 */
public interface HallazgoChatbotRepository extends JpaRepository<HallazgoChatbot, Long> {
}
