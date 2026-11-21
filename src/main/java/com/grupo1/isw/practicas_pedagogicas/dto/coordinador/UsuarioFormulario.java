package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;

/** Datos del formulario de crear/editar una cuenta (HU-36 y HU-39). La contraseña nunca se devuelve a la vista. */
public class UsuarioFormulario {
    private String nombre;
    private String email;
    private RolUsuario rol;
    private String password;
    /** Solo para el rol ESTUDIANTE. */
    private String carrera;
    /** Solo para el rol COLABORADOR. */
    private Long idCentro;

    public static UsuarioFormulario vacio() {
        return new UsuarioFormulario();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public Long getIdCentro() {
        return idCentro;
    }

    public void setIdCentro(Long idCentro) {
        this.idCentro = idCentro;
    }
}
