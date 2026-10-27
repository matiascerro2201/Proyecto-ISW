package com.grupo1.isw.practicas_pedagogicas;

import com.grupo1.isw.practicas_pedagogicas.dto.NotificacionResponse;
import com.grupo1.isw.practicas_pedagogicas.models.Notificacion;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.NotificacionRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;
import com.grupo1.isw.practicas_pedagogicas.services.NotificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU-22: crear, listar, contar y marcar como leída. repositorios en memoria, sin base de datos. */
class NotificacionServiceTest {
    private static final Long ID_USUARIO = 1L;
    private static final Long ID_OTRO_USUARIO = 2L;
    private static final String TIPO = NotificacionService.TIPO_PLANIFICACION;

    private final List<Notificacion> tabla = new ArrayList<>();
    private final Map<Long, Usuario> usuarios = new HashMap<>();
    private long siguienteId = 1;
    private int guardados = 0;
    private final NotificacionService servicio;

    NotificacionServiceTest() {
        usuarios.put(ID_USUARIO, usuario(ID_USUARIO));
        usuarios.put(ID_OTRO_USUARIO, usuario(ID_OTRO_USUARIO));

        NotificacionRepository notificaciones = repositorio(NotificacionRepository.class, Map.of(
                "save", a -> {
                    Notificacion n = (Notificacion) a[0];
                    guardados++;
                    if (n.getIdNotificacion() == null) {
                        n.setIdNotificacion(siguienteId++);
                        tabla.add(n);
                    }
                    return n;
                },
                "findById", a -> tabla.stream().filter(n -> n.getIdNotificacion().equals(a[0])).findFirst(),
                "findByUsuario_IdUsuarioOrderByFechaDescIdNotificacionDesc", a -> tabla.stream()
                        .filter(n -> n.getUsuario().getIdUsuario().equals(a[0]))
                        .sorted(Comparator.comparing(Notificacion::getFecha).reversed()
                                .thenComparing(Notificacion::getIdNotificacion, Comparator.reverseOrder()))
                        .toList(),
                "countByUsuario_IdUsuarioAndLeidaFalse", a -> tabla.stream()
                        .filter(n -> n.getUsuario().getIdUsuario().equals(a[0]) && !n.isLeida())
                        .count()));
        UsuarioRepository usuariosRepo = repositorio(UsuarioRepository.class, Map.of(
                "findById", a -> Optional.ofNullable(usuarios.get(a[0]))));
        servicio = new NotificacionService(notificaciones, usuariosRepo);
    }

    // ------------------------------------------------------------------ crear

    @Test
    @DisplayName("Crear guarda una notificación no leída, con fecha y para el usuario indicado")
    void creaUnaNotificacionNoLeida() {
        servicio.crear(ID_USUARIO, TIPO, "Camila envió una planificación", "/planificaciones/7/evaluar");

        assertEquals(1, tabla.size());
        Notificacion guardada = tabla.get(0);
        assertEquals(ID_USUARIO, guardada.getUsuario().getIdUsuario());
        assertEquals(TIPO, guardada.getTipo());
        assertEquals("Camila envió una planificación", guardada.getMensaje());
        assertEquals("/planificaciones/7/evaluar", guardada.getEnlace());
        assertFalse(guardada.isLeida());
        assertNotNull(guardada.getFecha());
    }

    @Test
    @DisplayName("Crear recorta espacios y deja el enlace en null si viene vacío")
    void recortaTextosYEnlaceVacio() {
        servicio.crear(ID_USUARIO, "  " + TIPO + " ", "  Hola  ", "   ");

        Notificacion guardada = tabla.get(0);
        assertEquals(TIPO, guardada.getTipo());
        assertEquals("Hola", guardada.getMensaje());
        assertNull(guardada.getEnlace());
    }

    @Test
    @DisplayName("Un mensaje de más de 500 caracteres se abrevia para no romper el guardado")
    void abreviaMensajesLargos() {
        servicio.crear(ID_USUARIO, TIPO, "a".repeat(800), null);

        String mensaje = tabla.get(0).getMensaje();
        assertEquals(500, mensaje.length());
        assertTrue(mensaje.endsWith("…"));
    }

    @Test
    @DisplayName("Crear rechaza usuario, tipo o mensaje vacíos y no guarda nada")
    void crearRechazaDatosVacios() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(null, TIPO, "Hola", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(ID_USUARIO, "  ", "Hola", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(ID_USUARIO, TIPO, null, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(ID_USUARIO, TIPO, "   ", null));
        assertTrue(tabla.isEmpty());
    }

    @Test
    @DisplayName("Crear para un usuario que no existe da 404")
    void crearUsuarioInexistente() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(999L, TIPO, "Hola", null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(tabla.isEmpty());
    }

    // ------------------------------------------------------------------ listar y contar

    @Test
    @DisplayName("Listar devuelve solo las del usuario, de la más reciente a la más antigua")
    void listaLasDelUsuarioMasRecientePrimero() {
        agregar(ID_USUARIO, "Antigua", LocalDateTime.of(2026, 10, 1, 9, 0), true);
        agregar(ID_USUARIO, "Reciente", LocalDateTime.of(2026, 10, 3, 9, 0), false);
        agregar(ID_OTRO_USUARIO, "De otro usuario", LocalDateTime.of(2026, 10, 2, 9, 0), false);
        agregar(ID_USUARIO, "Intermedia", LocalDateTime.of(2026, 10, 2, 9, 0), false);

        List<NotificacionResponse> lista = servicio.listar(ID_USUARIO);

        assertEquals(List.of("Reciente", "Intermedia", "Antigua"),
                lista.stream().map(NotificacionResponse::mensaje).toList());
        assertTrue(lista.get(2).leida());
        assertFalse(lista.get(0).leida());
    }

    @Test
    @DisplayName("Contar no leídas ignora las leídas y las de otros usuarios")
    void cuentaSoloLasNoLeidasDelUsuario() {
        agregar(ID_USUARIO, "Nueva 1", LocalDateTime.of(2026, 10, 1, 9, 0), false);
        agregar(ID_USUARIO, "Nueva 2", LocalDateTime.of(2026, 10, 2, 9, 0), false);
        agregar(ID_USUARIO, "Leída", LocalDateTime.of(2026, 10, 3, 9, 0), true);
        agregar(ID_OTRO_USUARIO, "De otro", LocalDateTime.of(2026, 10, 4, 9, 0), false);

        assertEquals(2L, servicio.contarNoLeidas(ID_USUARIO));
        assertEquals(1L, servicio.contarNoLeidas(ID_OTRO_USUARIO));
        assertEquals(0L, servicio.contarNoLeidas(999L));
    }

    // ------------------------------------------------------------------ marcar como leída

    @Test
    @DisplayName("Marcar como leída la deja leída y baja el contador")
    void marcaComoLeida() {
        Notificacion nueva = agregar(ID_USUARIO, "Nueva", LocalDateTime.of(2026, 10, 1, 9, 0), false);
        assertEquals(1L, servicio.contarNoLeidas(ID_USUARIO));

        servicio.marcarLeida(ID_USUARIO, nueva.getIdNotificacion());

        assertTrue(nueva.isLeida());
        assertEquals(0L, servicio.contarNoLeidas(ID_USUARIO));
    }

    @Test
    @DisplayName("Marcar una notificación de otro usuario da 403 y no la modifica")
    void marcarNotificacionAjena() {
        Notificacion ajena = agregar(ID_OTRO_USUARIO, "De otro", LocalDateTime.of(2026, 10, 1, 9, 0), false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.marcarLeida(ID_USUARIO, ajena.getIdNotificacion()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertFalse(ajena.isLeida());
    }

    @Test
    @DisplayName("Marcar una notificación que no existe da 404")
    void marcarInexistente() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.marcarLeida(ID_USUARIO, 12345L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Marcar una notificación ya leída no vuelve a guardarla")
    void marcarYaLeidaNoHaceNada() {
        Notificacion leida = agregar(ID_USUARIO, "Leída", LocalDateTime.of(2026, 10, 1, 9, 0), true);
        guardados = 0;

        servicio.marcarLeida(ID_USUARIO, leida.getIdNotificacion());

        assertEquals(0, guardados);
        assertTrue(leida.isLeida());
    }

    // ------------------------------------------------------------------ apoyo

    private Notificacion agregar(Long idUsuario, String mensaje, LocalDateTime fecha, boolean leida) {
        Notificacion n = new Notificacion(usuarios.get(idUsuario), TIPO, mensaje, null, fecha);
        n.setIdNotificacion(siguienteId++);
        n.setLeida(leida);
        tabla.add(n);
        return n;
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        return usuario;
    }

    /** Repositorio falso: responde los métodos listados y lanza UnsupportedOperationException con los demás. */
    @SuppressWarnings("unchecked")
    private static <T> T repositorio(Class<T> tipo, Map<String, Function<Object[], Object>> metodos) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (proxy, metodo, args) -> {
            Function<Object[], Object> respuesta = metodos.get(metodo.getName());
            if (respuesta == null) {
                throw new UnsupportedOperationException(tipo.getSimpleName() + "." + metodo.getName() + " no simulado");
            }
            return respuesta.apply(args);
        });
    }
}
