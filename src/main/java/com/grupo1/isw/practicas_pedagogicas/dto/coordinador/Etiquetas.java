package com.grupo1.isw.practicas_pedagogicas.dto.coordinador;

import com.grupo1.isw.practicas_pedagogicas.models.Ciclo;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoInscripcion;
import com.grupo1.isw.practicas_pedagogicas.models.EstadoPlanificacion;
import com.grupo1.isw.practicas_pedagogicas.models.RolUsuario;
import com.grupo1.isw.practicas_pedagogicas.models.TipoCentro;
import com.grupo1.isw.practicas_pedagogicas.models.TipoDocumento;
import com.grupo1.isw.practicas_pedagogicas.models.TipoEvaluador;
import com.grupo1.isw.practicas_pedagogicas.models.TipoLinea;

/** Textos en español de los valores de cada enum, para las vistas del coordinador. */
public final class Etiquetas {
    private static final String SIN_DATO = "—";

    private Etiquetas() {
    }

    public static String ciclo(Ciclo ciclo) {
        if (ciclo == null) {
            return SIN_DATO;
        }
        return switch (ciclo) {
            case INICIAL -> "Inicial";
            case INTERMEDIO -> "Intermedio";
            case FINAL -> "Final";
        };
    }

    public static String tipoLinea(TipoLinea tipo) {
        if (tipo == null) {
            return SIN_DATO;
        }
        return switch (tipo) {
            case UNICA -> "Única";
            case ESPECIALIDAD -> "Especialidad";
            case ORIENTACION -> "Orientación";
        };
    }

    public static String tipoDocumento(TipoDocumento tipo) {
        if (tipo == null) {
            return SIN_DATO;
        }
        return switch (tipo) {
            case INFORME -> "Informe";
            case PRODUCTO -> "Producto";
            case PROYECTO_INTERVENCION -> "Proyecto de intervención";
        };
    }

    public static String tipoEvaluador(TipoEvaluador tipo) {
        if (tipo == null) {
            return SIN_DATO;
        }
        return switch (tipo) {
            case COLABORADOR -> "Colaborador";
            case TUTOR -> "Supervisor";
        };
    }

    public static String estadoInscripcion(EstadoInscripcion estado) {
        if (estado == null) {
            return SIN_DATO;
        }
        return switch (estado) {
            case EN_CURSO -> "En curso";
            case APROBADO -> "Aprobado";
            case REPROBADO -> "Reprobado";
            case RETIRADO -> "Retirado";
        };
    }

    public static String estadoPlanificacion(EstadoPlanificacion estado) {
        if (estado == null) {
            return SIN_DATO;
        }
        return switch (estado) {
            case BORRADOR -> "Borrador";
            case ENVIADA -> "Por revisar";
            case APROBADA -> "Aprobada";
            case RECHAZADA -> "Rechazada";
        };
    }

    public static String tipoCentro(TipoCentro tipo) {
        if (tipo == null) {
            return SIN_DATO;
        }
        return switch (tipo) {
            case MUNICIPAL -> "Municipal";
            case SUBVENCIONADO -> "Particular subvencionado";
            case PARTICULAR -> "Particular pagado";
            case EDUCACION_ADULTOS -> "Educación de adultos";
        };
    }

    public static String rol(RolUsuario rol) {
        if (rol == null) {
            return SIN_DATO;
        }
        return switch (rol) {
            case ESTUDIANTE -> "Estudiante";
            case COLABORADOR -> "Profesor colaborador";
            case TUTOR -> "Profesor supervisor";
            case PROFESOR_ASIGNATURA -> "Profesor de asignatura";
            case COORDINADOR -> "Coordinador de prácticas";
        };
    }
}
