package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** De la más reciente a más antigua */
    List<Notificacion> findByUsuario_IdUsuarioOrderByFechaDescIdNotificacionDesc(Long idUsuario);

    long countByUsuario_IdUsuarioAndLeidaFalse(Long idUsuario);
}
