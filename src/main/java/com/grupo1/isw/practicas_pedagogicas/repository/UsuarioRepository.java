package com.grupo1.isw.practicas_pedagogicas.repository;

import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado por UsuarioDetailsService para el login (el email es único en la tabla usuario). */
    Optional<Usuario> findByEmail(String email);
}
