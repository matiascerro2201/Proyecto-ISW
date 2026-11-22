package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.TipoDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Toda la información de un curso para el coordinador: datos, pautas y estudiantes con su estado. */
public record CursoDetalle(
        Long idSeccion,
        Long idAsignatura,
        String nombre,
        Ciclo ciclo,
        int numeroLineas,
        String profesor,
        String profesorEmail,
        int anio,
        int semestre,
        LocalDate fechaLimiteRevision,
        List<PautaEvaluacionResumen> pautasEvaluacion,
        List<PautaDocumentoResumen> pautasDocumento,
        List<ActividadInscripcion> estudiantes) {

    public String periodo() {
        return "Semestre " + semestre + " de " + anio;
    }

    public String cicloEtiqueta() {
        return Etiquetas.ciclo(ciclo);
    }

    public String enlaceSemestre() {
        return "/coordinador/cursos/semestre/" + anio + "/" + semestre;
    }

    public record PautaEvaluacionResumen(Long id, TipoLinea tipoLinea, int numeroAplicacion, BigDecimal ponderacion,
                                         int dimensiones, int criterios, boolean habilitada) {
        public String tipoLineaEtiqueta() {
            return Etiquetas.tipoLinea(tipoLinea);
        }
    }

    public record PautaDocumentoResumen(Long id, TipoDocumento tipo, int numero, BigDecimal ponderacion,
                                        boolean habilitada) {
        public String tipoEtiqueta() {
            return Etiquetas.tipoDocumento(tipo);
        }
    }
}
