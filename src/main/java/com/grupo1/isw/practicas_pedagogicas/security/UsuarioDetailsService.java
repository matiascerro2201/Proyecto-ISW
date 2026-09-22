package com.grupo1.isw.practicas_pedagogicas.security;

import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import com.grupo1.isw.practicas_pedagogicas.repositories.UsuarioRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Spring Security llama a este método con lo que el usuario escribe en el campo "username" (el email). */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return new UsuarioDetails(usuario);
    }
}
