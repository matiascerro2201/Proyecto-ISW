package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Un aspecto de la planificación que el asistente valoró al responder una interacción.
 * La observación es una frase breve del asistente, sin datos personales ni texto copiado del estudiante.
 */
@Entity
@Table(name = "hallazgo_chatbot")
public class HallazgoChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hallazgo")
    private Long idHallazgo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_interaccion", nullable = false)
    private InteraccionChatbot interaccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaPlanificacion categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false)
    private NivelHallazgo nivel;

    @Column(name = "observacion", length = 300)
    private String observacion;

    public HallazgoChatbot() {
    }

    public HallazgoChatbot(InteraccionChatbot interaccion, CategoriaPlanificacion categoria,
                           NivelHallazgo nivel, String observacion) {
        this.interaccion = interaccion;
        this.categoria = categoria;
        this.nivel = nivel;
        this.observacion = observacion;
    }

    public Long getIdHallazgo() {
        return idHallazgo;
    }

    public void setIdHallazgo(Long idHallazgo) {
        this.idHallazgo = idHallazgo;
    }

    public InteraccionChatbot getInteraccion() {
        return interaccion;
    }

    public void setInteraccion(InteraccionChatbot interaccion) {
        this.interaccion = interaccion;
    }

    public CategoriaPlanificacion getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaPlanificacion categoria) {
        this.categoria = categoria;
    }

    public NivelHallazgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelHallazgo nivel) {
        this.nivel = nivel;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
