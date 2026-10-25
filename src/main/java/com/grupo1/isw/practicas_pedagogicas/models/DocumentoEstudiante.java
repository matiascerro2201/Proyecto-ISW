package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_estudiante")
public class DocumentoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long idDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private InscripcionAsignatura inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pauta_documento", nullable = false)
    private PautaDocumento pautaDocumento;

    @Column(name = "contenido_url", nullable = false, length = 500)
    private String contenidoUrl;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "puntaje_obtenido", precision = 6, scale = 2)
    private BigDecimal puntajeObtenido;

    @Column(name = "comentarios_evaluador", columnDefinition = "TEXT")
    private String comentariosEvaluador;

    public DocumentoEstudiante() {
    }

    public DocumentoEstudiante(InscripcionAsignatura inscripcion, PautaDocumento pautaDocumento, String contenidoUrl, LocalDateTime fechaSubida) {
        this.inscripcion = inscripcion;
        this.pautaDocumento = pautaDocumento;
        this.contenidoUrl = contenidoUrl;
        this.fechaSubida = fechaSubida;
    }

    public Long getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(Long idDocumento) {
        this.idDocumento = idDocumento;
    }

    public InscripcionAsignatura getInscripcion() {
        return inscripcion;
    }

    public void setInscripcion(InscripcionAsignatura inscripcion) {
        this.inscripcion = inscripcion;
    }

    public PautaDocumento getPautaDocumento() {
        return pautaDocumento;
    }

    public void setPautaDocumento(PautaDocumento pautaDocumento) {
        this.pautaDocumento = pautaDocumento;
    }

    public String getContenidoUrl() {
        return contenidoUrl;
    }

    public void setContenidoUrl(String contenidoUrl) {
        this.contenidoUrl = contenidoUrl;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public BigDecimal getPuntajeObtenido() {
        return puntajeObtenido;
    }

    public void setPuntajeObtenido(BigDecimal puntajeObtenido) {
        this.puntajeObtenido = puntajeObtenido;
    }

    public String getComentariosEvaluador() {
        return comentariosEvaluador;
    }

    public void setComentariosEvaluador(String comentariosEvaluador) {
        this.comentariosEvaluador = comentariosEvaluador;
    }
}
