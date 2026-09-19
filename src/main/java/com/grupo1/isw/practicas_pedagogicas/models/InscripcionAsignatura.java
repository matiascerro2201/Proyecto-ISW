package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscripcion_asignatura",
       uniqueConstraints = @UniqueConstraint(name = "uq_inscripcion", columnNames = {"id_estudiante", "id_seccion"}))
public class InscripcionAsignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Long idInscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_seccion", nullable = false)
    private SeccionAsignatura seccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_centro", nullable = false)
    private CentroPractica centro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoInscripcion estado = EstadoInscripcion.EN_CURSO;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaPractica> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoEstudiante> documentos = new ArrayList<>();

    @OneToOne(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private InformeFinal informeFinal;

    public InscripcionAsignatura() {
    }

    public InscripcionAsignatura(Estudiante estudiante, SeccionAsignatura seccion, CentroPractica centro) {
        this.estudiante = estudiante;
        this.seccion = seccion;
        this.centro = centro;
    }

    public Long getIdInscripcion() {
        return idInscripcion;
    }

    public void setIdInscripcion(Long idInscripcion) {
        this.idInscripcion = idInscripcion;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public SeccionAsignatura getSeccion() {
        return seccion;
    }

    public void setSeccion(SeccionAsignatura seccion) {
        this.seccion = seccion;
    }

    public CentroPractica getCentro() {
        return centro;
    }

    public void setCentro(CentroPractica centro) {
        this.centro = centro;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public List<LineaPractica> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaPractica> lineas) {
        this.lineas = lineas;
    }

    public List<DocumentoEstudiante> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<DocumentoEstudiante> documentos) {
        this.documentos = documentos;
    }

    public InformeFinal getInformeFinal() {
        return informeFinal;
    }

    public void setInformeFinal(InformeFinal informeFinal) {
        this.informeFinal = informeFinal;
    }
}
