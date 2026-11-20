// Tarjeta fija y lista de dimensiones: lo comparten la evaluación y el editor de pautas.
window.Navegador = (function () {
    'use strict';

    var BARRA = 64;
    var ESPACIO = 16;
    var UMBRAL = BARRA + ESPACIO + 96;

    // La tarjeta cabe en la pantalla desde que se abre la página y crece cuando queda fija bajo la barra
    function ajustarAlto(contenedor, tarjeta, movil) {
        if (movil.matches) { tarjeta.style.maxHeight = ''; return; }
        var tope = Math.max(BARRA + ESPACIO, contenedor.getBoundingClientRect().top);
        tarjeta.style.maxHeight = Math.max(320, window.innerHeight - tope - ESPACIO) + 'px';
    }

    // Resalta en la lista la dimensión que se está viendo y desplaza la lista para mostrarla
    function resaltarActual(dimensiones, itemsNav, lista, desplazar) {
        if (!dimensiones.length || dimensiones.length !== itemsNav.length) { return; }
        var actual = 0;
        dimensiones.forEach(function (dimension, i) {
            if (dimension.getBoundingClientRect().top <= UMBRAL) { actual = i; }
        });
        if (window.scrollY > 0 && window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2) {
            actual = dimensiones.length - 1;
        }
        itemsNav.forEach(function (item, i) {
            if (i === actual) { item.setAttribute('aria-current', 'true'); } else { item.removeAttribute('aria-current'); }
        });
        if (desplazar === false) { return; }
        var caja = lista.getBoundingClientRect();
        var pos = itemsNav[actual].getBoundingClientRect();
        if (pos.top < caja.top) { lista.scrollTop -= caja.top - pos.top; }
        else if (pos.bottom > caja.bottom) { lista.scrollTop += pos.bottom - caja.bottom; }
    }

    return { ajustarAlto: ajustarAlto, resaltarActual: resaltarActual };
})();
