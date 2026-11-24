package com.grupo1.isw.practicas_pedagogicas.services;

import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.CentroOpcion;
import com.grupo1.isw.practicas_pedagogicas.dto.coordinador.UsuarioFormulario;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioAdminRepository;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoordinadorUsuarioServiceTest {
    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioAdminRepository adminRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks CoordinadorUsuarioService service;

    private static Usuario usuario(Long id, RolUsuario rol, boolean activo) {
        Usuario u = new Usuario("Nombre " + id, "u" + id + "@ubb.cl", "hash", rol);
        u.setIdUsuario(id);
        u.setActivo(activo);
        return u;
    }

    private static UsuarioFormulario formulario(RolUsuario rol) {
        UsuarioFormulario f = new UsuarioFormulario();
        f.setNombre("  Ana Pérez ");
        f.setEmail(" Ana@UBB.cl ");
        f.setRol(rol);
        f.setPassword("clave-segura");
        f.setPasswordConfirmacion("clave-segura");
        return f;
    }

    @Test
    void creaUnProfesorConCorreoEnMinusculasYClaveCifrada() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave-segura")).thenReturn("cifrada");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(7L);
            return u;
        });

        Long id = service.crear(formulario(RolUsuario.PROFESOR_ASIGNATURA));

        assertEquals(7L, id);
        verify(adminRepository).insertarSubtipo(7L, RolUsuario.PROFESOR_ASIGNATURA, null);
    }

    @Test
    void noCreaConUnCorreoYaUsado() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.of(usuario(1L, RolUsuario.PROFESOR_ASIGNATURA, true)));

        assertThrows(IllegalArgumentException.class, () -> service.crear(formulario(RolUsuario.PROFESOR_ASIGNATURA)));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void creaUnEstudianteSinPedirCarrera() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave-segura")).thenReturn("cifrada");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(8L);
            return u;
        });

        Long id = service.crear(formulario(RolUsuario.ESTUDIANTE));

        assertEquals(8L, id);
        verify(adminRepository).insertarSubtipo(8L, RolUsuario.ESTUDIANTE, null);
    }

    @Test
    void elColaboradorNecesitaUnCentroExistente() {
        UsuarioFormulario f = formulario(RolUsuario.COLABORADOR);
        f.setIdCentro(99L);
        when(adminRepository.centros()).thenReturn(List.of(new CentroOpcion(1L, "Liceo", "Chillán")));

        assertThrows(IllegalArgumentException.class, () -> service.crear(f));
    }

    @Test
    void laClaveInicialEsObligatoriaYTieneLargoMinimo() {
        UsuarioFormulario sinClave = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        sinClave.setPassword("");
        sinClave.setPasswordConfirmacion("");
        assertThrows(IllegalArgumentException.class, () -> service.crear(sinClave));

        UsuarioFormulario corta = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        corta.setPassword("1234567");
        corta.setPasswordConfirmacion("1234567");
        assertThrows(IllegalArgumentException.class, () -> service.crear(corta));
    }

    @Test
    void noCreaSiLaContrasenaRepetidaNoCoincide() {
        UsuarioFormulario distinta = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        distinta.setPasswordConfirmacion("clave-segurA");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.crear(distinta));

        assertEquals("Las contraseñas no coinciden.", error.getMessage());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void noCreaSiNoSeRepiteLaContrasena() {
        UsuarioFormulario sinRepetir = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        sinRepetir.setPasswordConfirmacion(null);

        assertThrows(IllegalArgumentException.class, () -> service.crear(sinRepetir));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void alEditarNoCambiaLaClaveSiNoSeEscribeUnaNueva() {
        Usuario u = usuario(4L, RolUsuario.PROFESOR_ASIGNATURA, true);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(u));
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());
        UsuarioFormulario f = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        f.setPassword("");
        f.setPasswordConfirmacion("");

        service.actualizar(1L, 4L, f);

        verify(passwordEncoder, never()).encode(any());
        assertEquals("hash", u.getPasswordHash());
    }

    @Test
    void alEditarCambiaLaClaveSoloSiLaRepeticionCoincide() {
        Usuario u = usuario(4L, RolUsuario.PROFESOR_ASIGNATURA, true);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(u));
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());
        UsuarioFormulario distinta = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        distinta.setPassword("otra-clave-1");
        distinta.setPasswordConfirmacion("otra-clave-2");

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1L, 4L, distinta));
        verify(passwordEncoder, never()).encode(any());

        UsuarioFormulario igual = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        igual.setPassword("otra-clave-1");
        igual.setPasswordConfirmacion("otra-clave-1");
        when(passwordEncoder.encode("otra-clave-1")).thenReturn("nueva-cifrada");

        service.actualizar(1L, 4L, igual);

        assertEquals("nueva-cifrada", u.getPasswordHash());
    }

    @Test
    void alEditarRechazaUnaRepeticionSinContrasenaNueva() {
        Usuario u = usuario(4L, RolUsuario.PROFESOR_ASIGNATURA, true);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(u));
        UsuarioFormulario f = formulario(RolUsuario.PROFESOR_ASIGNATURA);
        f.setPassword("");

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1L, 4L, f));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void noPuedeDesactivarSuPropiaCuenta() {
        assertThrows(IllegalArgumentException.class, () -> service.desactivar(1L, 1L));
    }

    @Test
    void noDesactivaAlUltimoCoordinadorActivo() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, RolUsuario.COORDINADOR, true)));
        when(usuarioRepository.countByRolAndActivoTrue(RolUsuario.COORDINADOR)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.desactivar(1L, 2L));
    }

    @Test
    void desactivaUnaCuentaCualquiera() {
        Usuario u = usuario(3L, RolUsuario.PROFESOR_ASIGNATURA, true);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(u));

        service.desactivar(1L, 3L);

        assertFalse(u.isActivo());
        verify(usuarioRepository).save(u);
    }

    @Test
    void activaUnaCuentaInactiva() {
        Usuario u = usuario(3L, RolUsuario.PROFESOR_ASIGNATURA, false);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(u));

        service.activar(3L);

        assertTrue(u.isActivo());
    }

    @Test
    void cambiaElRolSiNoTieneRegistrosAsociados() {
        Usuario u = usuario(4L, RolUsuario.PROFESOR_ASIGNATURA, true);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(u));
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());
        when(adminRepository.contarRegistrosAsociados(4L, RolUsuario.PROFESOR_ASIGNATURA)).thenReturn(0L);
        UsuarioFormulario f = formulario(RolUsuario.TUTOR);
        f.setPassword("");
        f.setPasswordConfirmacion("");

        service.actualizar(1L, 4L, f);

        verify(adminRepository).reemplazarSubtipo(4L, RolUsuario.PROFESOR_ASIGNATURA, RolUsuario.TUTOR, null);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void noCambiaElRolSiTieneRegistrosAsociados() {
        Usuario u = usuario(4L, RolUsuario.PROFESOR_ASIGNATURA, true);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(u));
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());
        when(adminRepository.contarRegistrosAsociados(4L, RolUsuario.PROFESOR_ASIGNATURA)).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1L, 4L, formulario(RolUsuario.TUTOR)));
        verify(adminRepository, never()).reemplazarSubtipo(anyLong(), any(), any(), any());
    }

    @Test
    void noPuedeCambiarSuPropioRol() {
        Usuario u = usuario(1L, RolUsuario.COORDINADOR, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.findByEmailIgnoreCase("ana@ubb.cl")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1L, 1L, formulario(RolUsuario.PROFESOR_ASIGNATURA)));
    }
}
