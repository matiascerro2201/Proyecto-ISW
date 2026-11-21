package com.grupo1.isw.practicas_pedagogicas.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interaccion_chatbot")
public class InteraccionChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_interaccion")
    private Long idInteraccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_planificacion", nullable = false)
    private Planificacion planificacion;

    @Column(name = "mensaje_estudiante", nullable = false, columnDefinition = "TEXT")
    private String mensajeEstudiante;

    @Column(name = "respuesta_bot", nullable = false, columnDefinition = "TEXT")
    private String respuestaBot;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    /** Revisión general o pregunta. Lo que existía antes de HU-30 quedó como pregunta. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoInteraccion tipo = TipoInteraccion.PREGUNTA;

    /** Lo que el asistente valoró al responder; se guarda junto con la interacción. */
    @OneToMany(mappedBy = "interaccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HallazgoChatbot> hallazgos = new ArrayList<>();

    public InteraccionChatbot() {
    }

    /** Una pregunta: el caso corriente. */
    public InteraccionChatbot(Planificacion planificacion, String mensajeEstudiante, String respuestaBot, LocalDateTime fecha) {
        this(planificacion, mensajeEstudiante, respuestaBot, fecha, TipoInteraccion.PREGUNTA);
    }

    public InteraccionChatbot(Planificacion planificacion, String mensajeEstudiante, String respuestaBot,
                              LocalDateTime fecha, TipoInteraccion tipo) {
        this.planificacion = planificacion;
        this.mensajeEstudiante = mensajeEstudiante;
        this.respuestaBot = respuestaBot;
        this.fecha = fecha;
        this.tipo = tipo;
    }

    public Long getIdInteraccion() {
        return idInteraccion;
    }

    public void setIdInteraccion(Long idInteraccion) {
        this.idInteraccion = idInteraccion;
    }

    public Planificacion getPlanificacion() {
        return planificacion;
    }

    public void setPlanificacion(Planificacion planificacion) {
        this.planificacion = planificacion;
    }

    public String getMensajeEstudiante() {
        return mensajeEstudiante;
    }

    public void setMensajeEstudiante(String mensajeEstudiante) {
        this.mensajeEstudiante = mensajeEstudiante;
    }

    public String getRespuestaBot() {
        return respuestaBot;
    }

    public void setRespuestaBot(String respuestaBot) {
        this.respuestaBot = respuestaBot;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public TipoInteraccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoInteraccion tipo) {
        this.tipo = tipo;
    }

    public List<HallazgoChatbot> getHallazgos() {
        return hallazgos;
    }

    public void setHallazgos(List<HallazgoChatbot> hallazgos) {
        this.hallazgos = hallazgos;
    }

    /** Agrega un hallazgo a esta interacción; se guarda al guardar la interacción. */
    public void agregarHallazgo(CategoriaPlanificacion categoria, NivelHallazgo nivel, String observacion) {
        hallazgos.add(new HallazgoChatbot(this, categoria, nivel, observacion));
    }
}
