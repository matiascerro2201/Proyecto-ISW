package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asignatura")
public class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignatura")
    private Long idAsignatura;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ciclo", nullable = false)
    private Ciclo ciclo;

    @Column(name = "numero_lineas", nullable = false)
    private Integer numeroLineas = 1;

    @OneToMany(mappedBy = "asignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeccionAsignatura> secciones = new ArrayList<>();

    @OneToMany(mappedBy = "asignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PautaEvaluacion> pautasEvaluacion = new ArrayList<>();

    @OneToMany(mappedBy = "asignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PautaDocumento> pautasDocumento = new ArrayList<>();

    public Asignatura() {
    }

    public Asignatura(String nombre, Ciclo ciclo, Integer numeroLineas) {
        this.nombre = nombre;
        this.ciclo = ciclo;
        this.numeroLineas = numeroLineas;
    }

    public Long getIdAsignatura() {
        return idAsignatura;
    }

    public void setIdAsignatura(Long idAsignatura) {
        this.idAsignatura = idAsignatura;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public void setCiclo(Ciclo ciclo) {
        this.ciclo = ciclo;
    }

    public Integer getNumeroLineas() {
        return numeroLineas;
    }

    public void setNumeroLineas(Integer numeroLineas) {
        this.numeroLineas = numeroLineas;
    }

    public List<SeccionAsignatura> getSecciones() {
        return secciones;
    }

    public void setSecciones(List<SeccionAsignatura> secciones) {
        this.secciones = secciones;
    }

    public List<PautaEvaluacion> getPautasEvaluacion() {
        return pautasEvaluacion;
    }

    public void setPautasEvaluacion(List<PautaEvaluacion> pautasEvaluacion) {
        this.pautasEvaluacion = pautasEvaluacion;
    }

    public List<PautaDocumento> getPautasDocumento() {
        return pautasDocumento;
    }

    public void setPautasDocumento(List<PautaDocumento> pautasDocumento) {
        this.pautasDocumento = pautasDocumento;
    }
}
