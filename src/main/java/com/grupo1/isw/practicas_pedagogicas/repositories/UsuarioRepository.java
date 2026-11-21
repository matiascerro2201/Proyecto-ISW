package com.grupo1.isw.practicas_pedagogicas.repositories;

import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    /** HU-36: el correo es único sin importar mayúsculas. */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findAllByOrderByNombreAsc();

    /** HU-36/39: para no dejar al sistema sin ningún coordinador activo. */
    long countByRolAndActivoTrue(RolUsuario rol);
}
