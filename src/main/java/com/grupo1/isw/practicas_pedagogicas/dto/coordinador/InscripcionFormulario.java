package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import java.util.ArrayList;
import java.util.List;

/** N-02: inscripción de un estudiante en una sección, con sus líneas de práctica. */
public class InscripcionFormulario {
    private Long idEstudiante;
    private Long idCentro;
    private List<LineaFormulario> lineas = new ArrayList<>();

    public Long getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(Long idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public Long getIdCentro() {
        return idCentro;
    }

    public void setIdCentro(Long idCentro) {
        this.idCentro = idCentro;
    }

    public List<LineaFormulario> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaFormulario> lineas) {
        this.lineas = lineas;
    }
}
