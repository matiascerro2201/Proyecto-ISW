// Pauta del semestre (coordinador): cada cambio de contenido recarga la página, así que se recuerda qué dimensiones
// estaban abiertas y se vuelve a la que se estaba editando. Complementa a ver-pauta.js (expandir, plegar y navegación).
(function () {
    'use strict';

    var raiz = document.getElementById('ver-pauta');
    if (!raiz) { return; }

    var dimensiones = Array.prototype.slice.call(raiz.querySelectorAll('.ev-dim'));
    var claveAbiertas = 'pauta-abiertas:' + window.location.pathname;
    var claveUltima = 'pauta-ultima:' + window.location.pathname;

    function leer(clave) {
        try { return window.sessionStorage.getItem(clave); } catch (e) { return null; }
    }
    function escribir(clave, valor) {
        try {
            if (valor === null) { window.sessionStorage.removeItem(clave); } else { window.sessionStorage.setItem(clave, valor); }
        } catch (e) { /* sin almacenamiento: la pauta se muestra con la primera dimensión abierta */ }
    }
    function detalleDe(dimension) { return dimension.querySelector('details'); }

    // Restaura las dimensiones que estaban abiertas
    var guardadas = leer(claveAbiertas);
    if (guardadas) {
        try {
            var ids = JSON.parse(guardadas);
            dimensiones.forEach(function (dimension) {
                detalleDe(dimension).open = ids.indexOf(dimension.getAttribute('data-id')) !== -1;
            });
        } catch (e) { /* valor dañado: se ignora */ }
    }

    // Vuelve a la dimensión que se estaba editando (o a la recién creada)
    var ultima = leer(claveUltima);
    if (ultima && dimensiones.length) {
        var destino = ultima === 'nueva'
            ? dimensiones[dimensiones.length - 1]
            : dimensiones.filter(function (d) { return d.getAttribute('data-id') === ultima; })[0];
        if (destino) {
            detalleDe(destino).open = true;
            destino.scrollIntoView({ block: 'start' });
        }
    }
    escribir(claveUltima, null);

    function recordarAbiertas() {
        var abiertas = dimensiones.filter(function (d) { return detalleDe(d).open; })
            .map(function (d) { return d.getAttribute('data-id'); });
        escribir(claveAbiertas, JSON.stringify(abiertas));
    }
    // "toggle" no burbujea: se escucha en captura
    raiz.addEventListener('toggle', recordarAbiertas, true);

    raiz.addEventListener('submit', function (evento) {
        var formulario = evento.target;
        var dimension = formulario.closest ? formulario.closest('.ev-dim') : null;
        if (formulario.hasAttribute('data-nueva-dimension')) {
            escribir(claveUltima, 'nueva');
        } else if (dimension) {
            escribir(claveUltima, dimension.getAttribute('data-id'));
        }
        recordarAbiertas();
    });
})();
