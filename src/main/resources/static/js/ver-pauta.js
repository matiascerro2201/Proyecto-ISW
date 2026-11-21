// Vista de una pauta (solo lectura): lista de dimensiones a la izquierda, expandir y plegar, y seguimiento de la lectura.
(function () {
    'use strict';

    var raiz = document.getElementById('ver-pauta');
    if (!raiz) { return; }

    var lista = document.getElementById('ev-nav-lista');
    var dimensiones = Array.prototype.slice.call(raiz.querySelectorAll('.ev-dim'));
    var itemsNav = Array.prototype.slice.call(raiz.querySelectorAll('.ev-nav__dim'));
    var lateral = raiz.querySelector('.editor__lateral');
    var movil = window.matchMedia('(max-width: 56rem)');

    function todos(contenedor, selector) {
        return Array.prototype.slice.call(contenedor.querySelectorAll(selector));
    }

    function abrirDimension(dimension) {
        dimension.querySelector('details').open = true;
    }

    raiz.addEventListener('click', function (evento) {
        var resumen = evento.target.closest('.ev-nav__cab');
        if (resumen) {
            var detalle = resumen.parentNode;
            // si estaba cerrado se va a abrir: además de expandir la lista, lleva a esa dimensión
            if (!detalle.open) {
                var dimension = dimensiones[itemsNav.indexOf(detalle.parentNode)];
                abrirDimension(dimension);
                dimension.scrollIntoView({ block: 'start' });
            }
            return;
        }

        var boton = evento.target.closest('button');
        if (!boton) { return; }
        if (boton.hasAttribute('data-ir')) {
            var criterio = document.getElementById('criterio-' + boton.getAttribute('data-ir'));
            if (criterio) {
                abrirDimension(criterio.closest('.ev-dim'));
                criterio.scrollIntoView({ block: 'start' });
            }
        } else if (boton.id === 'ev-expandir' || boton.id === 'ev-plegar') {
            dimensiones.forEach(function (dimension) { dimension.querySelector('details').open = boton.id === 'ev-expandir'; });
        } else if (boton.id === 'ev-nav-expandir' || boton.id === 'ev-nav-plegar') {
            todos(lista, 'details').forEach(function (detalle) { detalle.open = boton.id === 'ev-nav-expandir'; });
        }
    });

    var pendiente = false;
    function seguirLectura() {
        pendiente = false;
        Navegador.ajustarAlto(raiz, lateral, movil);
        Navegador.resaltarActual(dimensiones, itemsNav, lista);
    }
    window.addEventListener('scroll', function () {
        if (!pendiente) { pendiente = true; window.requestAnimationFrame(seguirLectura); }
    }, { passive: true });
    window.addEventListener('resize', seguirLectura);

    seguirLectura();
})();
