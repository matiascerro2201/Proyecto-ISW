package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroOpcion;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.Etiquetas;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.UsuarioFormulario;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.UsuarioResumen;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioAdminRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * HU-36 y HU-39: el coordinador crea, edita y desactiva cuentas y asigna el rol de cada usuario.
 *
 * <p>Las reglas que protegen al sistema: no puede desactivarse ni cambiarse el rol a sí mismo, siempre debe quedar
 * al menos un coordinador activo, y no se cambia el rol de quien ya tiene registros propios de su rol actual
 * (inscripciones, líneas, cursos o informes), porque quedarían huérfanos. En ese caso se desactiva la cuenta y se
 * crea otra. Los errores de validación salen como {@link IllegalArgumentException} con un mensaje para el usuario.
 */
@Service
public class CoordinadorUsuarioService {
    static final int CLAVE_MINIMA = 8;
    private static final int LARGO_NOMBRE = 150;
    private static final int LARGO_CORREO = 150;
    private static final int LARGO_CARRERA = 150;
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public CoordinadorUsuarioService(UsuarioRepository usuarioRepository,
                                     UsuarioAdminRepository adminRepository,
                                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumen> listar(String texto, RolUsuario rol, Boolean activo) {
        String buscado = TextoBusqueda.normalizar(texto);
        return usuarioRepository.findAllByOrderByNombreAsc().stream()
                .filter(u -> rol == null || u.getRol() == rol)
                .filter(u -> activo == null || u.isActivo() == activo)
                .filter(u -> TextoBusqueda.contiene(u.getNombre(), buscado) || TextoBusqueda.contiene(u.getEmail(), buscado))
                .map(u -> new UsuarioResumen(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CentroOpcion> centros() {
        return adminRepository.centros();
    }

    /** El formulario de edición con los datos actuales (sin contraseña). */
    @Transactional(readOnly = true)
    public UsuarioFormulario formularioDe(Long idUsuario) {
        Usuario usuario = obtener(idUsuario);
        UsuarioFormulario formulario = new UsuarioFormulario();
        formulario.setNombre(usuario.getNombre());
        formulario.setEmail(usuario.getEmail());
        formulario.setRol(usuario.getRol());
        if (usuario.getRol() == RolUsuario.ESTUDIANTE) {
            formulario.setCarrera(adminRepository.carreraDe(idUsuario));
        }
        if (usuario.getRol() == RolUsuario.COLABORADOR) {
            formulario.setIdCentro(adminRepository.centroDe(idUsuario));
        }
        return formulario;
    }

    @Transactional
    public Long crear(UsuarioFormulario formulario) {
        String nombre = nombreValido(formulario.getNombre());
        String email = correoValido(formulario.getEmail());
        RolUsuario rol = rolValido(formulario.getRol());
        String carrera = formulario.getCarrera() == null ? null : formulario.getCarrera().strip();
        validarDatosDelRol(rol, carrera, formulario.getIdCentro());
        String clave = claveValida(formulario.getPassword(), true);
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }

        Usuario usuario = new Usuario(nombre, email, passwordEncoder.encode(clave), rol);
        usuario.setActivo(true);
        usuarioRepository.saveAndFlush(usuario);
        adminRepository.insertarSubtipo(usuario.getIdUsuario(), rol, carrera, formulario.getIdCentro());
        return usuario.getIdUsuario();
    }

    /**
     * Edita nombre, correo, datos del rol y, si se escribe una, la contraseña. Si el rol cambia (HU-39) se aplican las
     * protecciones descritas en la clase. {@code idCoordinador} es quien hace el cambio.
     */
    @Transactional
    public void actualizar(Long idCoordinador, Long idUsuario, UsuarioFormulario formulario) {
        Usuario usuario = obtener(idUsuario);
        String nombre = nombreValido(formulario.getNombre());
        String email = correoValido(formulario.getEmail());
        RolUsuario nuevo = rolValido(formulario.getRol());
        RolUsuario actual = usuario.getRol();
        String carrera = formulario.getCarrera() == null ? null : formulario.getCarrera().strip();
        validarDatosDelRol(nuevo, carrera, formulario.getIdCentro());
        String clave = claveValida(formulario.getPassword(), false);

        if (usuarioRepository.findByEmailIgnoreCase(email)
                .filter(otro -> !otro.getIdUsuario().equals(idUsuario)).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }
        if (nuevo != actual) {
            validarCambioDeRol(idCoordinador, usuario, actual);
        }

        usuario.setNombre(nombre);
        usuario.setEmail(email);
        if (clave != null) {
            usuario.setPasswordHash(passwordEncoder.encode(clave));
        }
        usuarioRepository.saveAndFlush(usuario);

        if (nuevo != actual) {
            adminRepository.reemplazarSubtipo(idUsuario, actual, nuevo, carrera, formulario.getIdCentro());
        } else {
            adminRepository.actualizarSubtipo(idUsuario, actual, carrera, formulario.getIdCentro());
        }
    }

    @Transactional
    public void desactivar(Long idCoordinador, Long idUsuario) {
        if (idCoordinador.equals(idUsuario)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
        }
        Usuario usuario = obtener(idUsuario);
        if (!usuario.isActivo()) {
            return;
        }
        if (usuario.getRol() == RolUsuario.COORDINADOR && esElUltimoCoordinadorActivo()) {
            throw new IllegalArgumentException("Debe quedar al menos un coordinador activo.");
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void activar(Long idUsuario) {
        Usuario usuario = obtener(idUsuario);
        if (!usuario.isActivo()) {
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
        }
    }

    private void validarCambioDeRol(Long idCoordinador, Usuario usuario, RolUsuario actual) {
        if (idCoordinador.equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("No puedes cambiar tu propio rol.");
        }
        if (actual == RolUsuario.COORDINADOR && usuario.isActivo() && esElUltimoCoordinadorActivo()) {
            throw new IllegalArgumentException("Debe quedar al menos un coordinador activo.");
        }
        long asociados = adminRepository.contarRegistrosAsociados(usuario.getIdUsuario(), actual);
        if (asociados > 0) {
            throw new IllegalArgumentException("No se puede cambiar el rol: la cuenta ya tiene " + asociados
                    + (asociados == 1 ? " registro asociado" : " registros asociados")
                    + " como " + Etiquetas.rol(actual).toLowerCase()
                    + ". Desactiva esta cuenta y crea una nueva con el rol correcto.");
        }
    }

    private boolean esElUltimoCoordinadorActivo() {
        return usuarioRepository.countByRolAndActivoTrue(RolUsuario.COORDINADOR) <= 1;
    }

    private Usuario obtener(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Escribe el nombre del usuario.");
        }
        String limpio = nombre.strip();
        if (limpio.length() > LARGO_NOMBRE) {
            throw new IllegalArgumentException("El nombre no puede pasar de " + LARGO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    private static String correoValido(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Escribe el correo del usuario.");
        }
        String limpio = email.strip().toLowerCase();
        if (limpio.length() > LARGO_CORREO || !CORREO.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El correo no es válido.");
        }
        return limpio;
    }

    private static RolUsuario rolValido(RolUsuario rol) {
        if (rol == null) {
            throw new IllegalArgumentException("Elige el rol del usuario.");
        }
        return rol;
    }

    /** Devuelve la contraseña, o null si es opcional y no se escribió. */
    private static String claveValida(String clave, boolean obligatoria) {
        if (clave == null || clave.isEmpty()) {
            if (obligatoria) {
                throw new IllegalArgumentException("Escribe una contraseña inicial para la cuenta.");
            }
            return null;
        }
        if (clave.length() < CLAVE_MINIMA) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + CLAVE_MINIMA + " caracteres.");
        }
        return clave;
    }

    private void validarDatosDelRol(RolUsuario rol, String carrera, Long idCentro) {
        if (rol == RolUsuario.ESTUDIANTE) {
            if (carrera == null || carrera.isBlank()) {
                throw new IllegalArgumentException("Escribe la carrera del estudiante.");
            }
            if (carrera.length() > LARGO_CARRERA) {
                throw new IllegalArgumentException("La carrera no puede pasar de " + LARGO_CARRERA + " caracteres.");
            }
        }
        if (rol == RolUsuario.COLABORADOR) {
            if (idCentro == null) {
                throw new IllegalArgumentException("Elige el centro de práctica del profesor colaborador.");
            }
            if (adminRepository.centros().stream().noneMatch(c -> c.id().equals(idCentro))) {
                throw new IllegalArgumentException("El centro de práctica elegido no existe.");
            }
        }
    }
}
