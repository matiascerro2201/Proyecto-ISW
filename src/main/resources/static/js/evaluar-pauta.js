// Evaluación de una pauta: avance, navegación por dimensiones y validación en pantalla.
(function () {
    'use strict';

    var form = document.getElementById('ev-form');
    if (!form) { return; }

    var lista = document.getElementById('ev-nav-lista');
    var dimensiones = Array.prototype.slice.call(form.querySelectorAll('.ev-dim'));
    var itemsNav = Array.prototype.slice.call(form.querySelectorAll('.ev-nav__dim'));
    var lateral = form.querySelector('.editor__lateral');
    var movil = window.matchMedia('(max-width: 56rem)');

    function todos(raiz, selector) {
        return Array.prototype.slice.call(raiz.querySelectorAll(selector));
    }

    function formato(valor) {
        return valor.toLocaleString('es-CL', { maximumFractionDigits: 2 });
    }

    function valorDe(input) {
        var texto = input.value.trim();
        return /^\d+([.,]\d{1,2})?$/.test(texto) ? parseFloat(texto.replace(',', '.')) : NaN;
    }

    function maximoDe(input) {
        return parseFloat(input.getAttribute('data-max'));
    }

    function esValido(input) {
        var valor = valorDe(input);
        return !isNaN(valor) && valor >= 0 && valor <= maximoDe(input);
    }

    // ---- Avance: totales, dimensiones y puntos de la navegación
    function actualizar() {
        var total = 0;
        var maximo = 0;
        var hechos = 0;
        var criterios = 0;

        dimensiones.forEach(function (dimension, i) {
            var inputs = todos(dimension, '[data-validar]');
            var hechosDim = 0;
            var puntosDim = 0;
            var maximoDim = 0;
            inputs.forEach(function (input) {
                maximoDim += maximoDe(input);
                var criterio = input.closest('.ev-criterio');
                var punto = form.querySelector('[data-ir="' + criterio.getAttribute('data-id') + '"] .ev-punto');
                var valido = esValido(input);
                if (valido) {
                    hechosDim += 1;
                    puntosDim += valorDe(input);
                }
                punto.className = 'ev-punto' + (input.getAttribute('aria-invalid') === 'true' ? ' ev-punto--error' : (valido ? ' ev-punto--hecho' : ''));
            });

            dimension.querySelector('[data-meta-dim]').textContent =
                hechosDim + ' de ' + inputs.length + (inputs.length === 1 ? ' criterio' : ' criterios')
                + ', ' + formato(puntosDim) + ' de ' + formato(maximoDim) + ' pts';

            var nav = itemsNav[i];
            nav.querySelector('[data-avance-nav]').textContent = hechosDim + '/' + inputs.length;
            if (hechosDim === inputs.length) { nav.setAttribute('data-completa', ''); } else { nav.removeAttribute('data-completa'); }

            total += puntosDim;
            maximo += maximoDim;
            hechos += hechosDim;
            criterios += inputs.length;
        });

        form.querySelector('[data-total]').textContent = formato(total);
        form.querySelector('[data-maximo]').textContent = formato(maximo);
        form.querySelector('[data-evaluados]').textContent = hechos;
        form.querySelector('[data-criterios]').textContent = criterios;
        var barra = form.querySelector('[data-barra]');
        barra.max = criterios || 1;
        barra.value = hechos;
    }

    // ---- Navegación
    function abrirDimension(dimension) {
        dimension.querySelector('details').open = true;
    }

    function irADimension(indice) {
        var dimension = dimensiones[indice];
        abrirDimension(dimension);
        dimension.scrollIntoView({ block: 'start' });
    }

    function irACriterio(id) {
        var criterio = document.getElementById('criterio-' + id);
        if (!criterio) { return; }
        abrirDimension(criterio.closest('.ev-dim'));
        criterio.scrollIntoView({ block: 'start' });
        criterio.querySelector('input').focus({ preventScroll: true });
    }

    form.addEventListener('click', function (evento) {
        var resumen = evento.target.closest('.ev-nav__cab');
        if (resumen) {
            var detalle = resumen.parentNode;
            // si estaba cerrado se va a abrir: además de expandir la lista, lleva a esa dimensión
            if (!detalle.open) { irADimension(parseInt(detalle.parentNode.getAttribute('data-dim'), 10)); }
            return;
        }
        var destino = evento.target.closest('button');
        if (!destino) { return; }
        if (destino.hasAttribute('data-ir')) {
            irACriterio(destino.getAttribute('data-ir'));
        } else if (destino.id === 'ev-expandir' || destino.id === 'ev-plegar') {
            dimensiones.forEach(function (dimension) { dimension.querySelector('details').open = destino.id === 'ev-expandir'; });
        } else if (destino.id === 'ev-nav-expandir' || destino.id === 'ev-nav-plegar') {
            todos(lista, 'details').forEach(function (detalle) { detalle.open = destino.id === 'ev-nav-expandir'; });
        } else if (destino.hasAttribute('data-cerrar-alerta')) {
            destino.closest('.editor__alerta').hidden = true;
        }
    });

    function seguirLectura() {
        pendiente = false;
        Navegador.ajustarAlto(form, lateral, movil);
        Navegador.resaltarActual(dimensiones, itemsNav, lista);
    }
    var pendiente = false;
    window.addEventListener('scroll', function () {
        if (!pendiente) { pendiente = true; window.requestAnimationFrame(seguirLectura); }
    }, { passive: true });

    // ---- Validación en pantalla (el servidor sigue validando todo)
    function marcar(input, mensaje) {
        var error = input.closest('.campo').querySelector('.campo__error');
        if (mensaje) {
            input.setAttribute('aria-invalid', 'true');
            error.textContent = mensaje;
            error.hidden = false;
        } else {
            input.removeAttribute('aria-invalid');
            error.textContent = '';
            error.hidden = true;
        }
    }

    function validar(input) {
        var texto = input.value.trim();
        var mensaje = '';
        if (!texto) {
            mensaje = 'Escribe un puntaje.';
        } else if (isNaN(valorDe(input))) {
            mensaje = 'Usa un número, con hasta 2 decimales.';
        } else if (valorDe(input) > maximoDe(input)) {
            mensaje = 'No puede superar ' + formato(maximoDe(input)) + '.';
        }
        marcar(input, mensaje);
        return !mensaje;
    }

    form.addEventListener('submit', function (evento) {
        var fallos = todos(form, '[data-validar]').filter(function (input) { return !validar(input); });
        actualizar();
        if (!fallos.length) { return; }
        evento.preventDefault();
        fallos.forEach(function (input) { abrirDimension(input.closest('.ev-dim')); });
        fallos[0].closest('.ev-criterio').scrollIntoView({ block: 'center' });
        fallos[0].focus({ preventScroll: true });
    });

    form.addEventListener('input', function (evento) {
        var input = evento.target;
        if (input.hasAttribute('data-validar') && input.getAttribute('aria-invalid') === 'true') { validar(input); }
        actualizar();
    });

    // Con el mouse, si va a pulsar un botón no se muestra el error ahora: movería la página y el clic se perdería
    var puntero = false;
    document.addEventListener('pointerdown', function () { puntero = true; }, true);
    document.addEventListener('pointerup', function () { puntero = false; }, true);
    document.addEventListener('pointercancel', function () { puntero = false; }, true);

    form.addEventListener('focusout', function (evento) {
        var input = evento.target;
        var destino = evento.relatedTarget;
        if (puntero && destino && destino.closest && destino.closest('button, a')) { return; }
        if (input.hasAttribute && input.hasAttribute('data-validar') && input.value.trim()) {
            validar(input);
            actualizar();
        }
    });

    window.addEventListener('resize', seguirLectura);
    actualizar();
    seguirLectura();
})();
