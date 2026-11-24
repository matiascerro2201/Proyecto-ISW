package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

/** N-02: colaborador y tutor de una línea de práctica de un estudiante. El tutor es opcional. */
public class LineaFormulario {
    private String tipo;
    private Long idColaborador;
    private Long idTutor;

    public LineaFormulario() {
    }

    public LineaFormulario(String tipo, Long idColaborador, Long idTutor) {
        this.tipo = tipo;
        this.idColaborador = idColaborador;
        this.idTutor = idTutor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getIdColaborador() {
        return idColaborador;
    }

    public void setIdColaborador(Long idColaborador) {
        this.idColaborador = idColaborador;
    }

    public Long getIdTutor() {
        return idTutor;
    }

    public void setIdTutor(Long idTutor) {
        this.idTutor = idTutor;
    }

    public String etiquetaTipo() {
        return switch (tipo == null ? "" : tipo) {
            case "ESPECIALIDAD" -> "Especialidad";
            case "ORIENTACION" -> "Orientación";
            default -> "Línea única";
        };
    }
}
