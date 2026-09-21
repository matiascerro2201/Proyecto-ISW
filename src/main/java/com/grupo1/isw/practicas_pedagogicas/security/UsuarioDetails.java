package com.grupo1.isw.practicas_pedagogicas.security;

import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Representa al usuario autenticado dentro de Spring Security.
 * Se guarda en la sesión, por eso copia solo datos simples (no la entidad JPA,
 * que arrastraría relaciones lazy y no debe vivir en la sesión).
 *
 * Desde cualquier controlador se obtiene con @AuthenticationPrincipal UsuarioDetails.
 */
public class UsuarioDetails implements UserDetails {

    private final Long idUsuario;
    private final String nombre;
    private final String email;
    private final String passwordHash;
    private final String rol;

    public UsuarioDetails(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.nombre = usuario.getNombre();
        this.email = usuario.getEmail();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol().name();   // si tu campo rol es String, quita .name()
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring exige el prefijo ROLE_ para usar hasRole('ESTUDIANTE')
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;   // el "username" del login es el email
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
