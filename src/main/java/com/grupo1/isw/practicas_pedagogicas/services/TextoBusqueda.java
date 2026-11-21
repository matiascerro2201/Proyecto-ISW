package com.grupo1.isw.practicas_pedagogicas.services;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza texto para buscar sin importar mayúsculas ni tildes (igual que el buscador de las demás listas). */
public final class TextoBusqueda {
    private TextoBusqueda() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public static boolean contiene(String texto, String normalizadoBuscado) {
        return normalizadoBuscado.isEmpty() || normalizar(texto).contains(normalizadoBuscado);
    }
}
