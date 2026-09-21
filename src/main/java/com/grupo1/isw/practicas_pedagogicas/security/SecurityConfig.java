package com.grupo1.isw.practicas_pedagogicas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Mecanismo de autenticación: login por formulario + sesión (HttpSession).
 * Las reglas por rol NO se definen aquí: cada integrante protege sus propios
 * controladores con @PreAuthorize (habilitado con @EnableMethodSecurity).
 * Aquí solo se exige estar autenticado para todo lo que no sea público.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Público: login, recursos estáticos y la página de error (para no enmascarar 403/404)
                .requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                // Todo lo demás requiere sesión iniciada
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")          // vista Thymeleaf propia (login.html)
                .defaultSuccessUrl("/")       // si venía de una URL protegida, vuelve a ella
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        // CSRF se deja HABILITADO (por defecto). Los formularios Thymeleaf con th:action
        // incluyen el token automáticamente.
        return http.build();
    }

    /** Las contraseñas de usuario.password_hash deben estar almacenadas con BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
