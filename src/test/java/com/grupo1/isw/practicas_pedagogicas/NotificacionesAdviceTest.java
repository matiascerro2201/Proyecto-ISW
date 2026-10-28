package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.controllers.NotificacionesAdvice;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.security.UsuarioDetails;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** HU-22: el contador de la barra de inicio vale 0 sin sesión y el conteo del usuario con sesión. */
class NotificacionesAdviceTest {
    private static final Long ID_USUARIO = 4L;

    private final NotificacionesAdvice advice = new NotificacionesAdvice(
            new NotificacionService(null, null) {
                @Override
                public long contarNoLeidas(Long idUsuario) {
                    return ID_USUARIO.equals(idUsuario) ? 3 : -1;
                }
            });

    @Test
    @DisplayName("Sin usuario (por ejemplo en /login) el contador es 0")
    void sinUsuarioDevuelveCero() {
        assertEquals(0L, advice.notificacionesNoLeidas(null));
    }

    @Test
    @DisplayName("Con usuario devuelve las no leídas de ese usuario")
    void conUsuarioDevuelveSuConteo() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID_USUARIO);
        usuario.setRol(RolUsuario.COLABORADOR);

        assertEquals(3L, advice.notificacionesNoLeidas(new UsuarioDetails(usuario)));
    }
}
