package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.NotificacionResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Notificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Notificaciones dentro del sistema (HU-22); otras historias solo llaman a crear con su propio tipo. */
@Service
public class NotificacionService {
    public static final String TIPO_PLANIFICACION = "PLANIFICACION";
    /** HU-17: el informe final del estudiante fue publicado. */
    public static final String TIPO_INFORME_FINAL = "INFORME_FINAL";
    private static final int MAX_MENSAJE = 500;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Se une a la transacción de quien la llama: si esa falla, la notificación tampoco queda. */
    @Transactional
    public Notificacion crear(Long idUsuario, String tipo, String mensaje, String enlace) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("Debe indicar el usuario a notificar");
        }
        String tipoLimpio = limpiar(tipo);
        if (tipoLimpio == null) {
            throw new IllegalArgumentException("El tipo de la notificación no puede estar vacío");
        }
        String texto = limpiar(mensaje);
        if (texto == null) {
            throw new IllegalArgumentException("El mensaje de la notificación no puede estar vacío");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return notificacionRepository.save(
                new Notificacion(usuario, tipoLimpio, abreviar(texto), limpiar(enlace), LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(Long idUsuario) {
        return notificacionRepository.findByUsuario_IdUsuarioOrderByFechaDescIdNotificacionDesc(idUsuario).stream()
                .map(NotificacionResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long idUsuario) {
        return notificacionRepository.countByUsuario_IdUsuarioAndLeidaFalse(idUsuario);
    }

    /** 404 si no existe y 403 si es de otro usuario; marcar una ya leída no hace nada. */
    @Transactional
    public void marcarLeida(Long idUsuario, Long idNotificacion) {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));

        if (!Objects.equals(notificacion.getUsuario().getIdUsuario(), idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para modificar esta notificación");
        }

        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private static String abreviar(String texto) {
        return texto.length() <= MAX_MENSAJE ? texto : texto.substring(0, MAX_MENSAJE - 1) + "…";
    }
}
