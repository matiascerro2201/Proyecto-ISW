package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "informe_final")
public class InformeFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_informe")
    private Long idInforme;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false, unique = true)
    private InscripcionAsignatura inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_corrector", nullable = false)
    private ProfesorAsignatura corrector;

    @Column(name = "calificacion_sugerida", precision = 3, scale = 1)
    private BigDecimal calificacionSugerida;

    @Column(name = "calificacion_final", precision = 3, scale = 1)
    private BigDecimal calificacionFinal;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    public InformeFinal() {
    }

    public InformeFinal(InscripcionAsignatura inscripcion, ProfesorAsignatura corrector) {
        this.inscripcion = inscripcion;
        this.corrector = corrector;
    }

    public Long getIdInforme() {
        return idInforme;
    }

    public void setIdInforme(Long idInforme) {
        this.idInforme = idInforme;
    }

    public InscripcionAsignatura getInscripcion() {
        return inscripcion;
    }

    public void setInscripcion(InscripcionAsignatura inscripcion) {
        this.inscripcion = inscripcion;
    }

    public ProfesorAsignatura getCorrector() {
        return corrector;
    }

    public void setCorrector(ProfesorAsignatura corrector) {
        this.corrector = corrector;
    }

    public BigDecimal getCalificacionSugerida() {
        return calificacionSugerida;
    }

    public void setCalificacionSugerida(BigDecimal calificacionSugerida) {
        this.calificacionSugerida = calificacionSugerida;
    }

    public BigDecimal getCalificacionFinal() {
        return calificacionFinal;
    }

    public void setCalificacionFinal(BigDecimal calificacionFinal) {
        this.calificacionFinal = calificacionFinal;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }
}
