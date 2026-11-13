// Editor de pautas: dimensiones y criterios dinámicos, confirmaciones y validación en pantalla.
(function () {
    'use strict';

    var form = document.getElementById('editor-pauta');
    if (!form) { return; }

    var lista = document.getElementById('dimensiones');
    var plantillaDimension = document.getElementById('plantilla-dimension');
    var plantillaCriterio = document.getElementById('plantilla-criterio');
    var botonAgregar = document.getElementById('agregar-dimension');
    var errorDimensiones = document.getElementById('error-dimensiones');
    var contadorIds = 0;

    function todos(raiz, selector) {
        return Array.prototype.slice.call(raiz.querySelectorAll(selector));
    }

    function numero(texto) {
        return parseFloat(texto.trim().replace(',', '.'));
    }

    function formato(valor) {
        return valor.toLocaleString('es-CL', { maximumFractionDigits: 2 });
    }

    // ---- Campos: ayuda y error enlazados al campo para lectores de pantalla
    function prepararCampos(raiz) {
        todos(raiz, '.campo').forEach(function (campo) {
            var control = campo.querySelector('input, select');
            if (!control || control.hasAttribute('data-listo')) { return; }
            control.setAttribute('data-listo', '');
            var ids = [];
            ['campo__ayuda', 'campo__error'].forEach(function (clase) {
                var nota = campo.querySelector('.' + clase);
                if (!nota) { return; }
                contadorIds += 1;
                nota.id = 'nota-' + contadorIds;
                ids.push(nota.id);
            });
            if (ids.length) { control.setAttribute('aria-describedby', ids.join(' ')); }
        });
    }

    // ---- Numeración, nombres de los campos y resumen
    function reindexar() {
        var criteriosTotal = 0;
        var puntosTotal = 0;
        var dimensiones = Array.prototype.slice.call(lista.children);

        dimensiones.forEach(function (dimension, i) {
            dimension.querySelector('.dimension__numero').textContent = i + 1;
            var nombre = dimension.querySelector('[data-campo="nombre"]');
            nombre.name = 'dimensiones[' + i + '].nombre';
            dimension.querySelector('.dimension__titulo').textContent = nombre.value.trim() || 'Dimensión sin nombre';

            var quitar = dimension.querySelector('[data-quitar-dimension]');
            if (quitar) { quitar.setAttribute('aria-label', 'Quitar la dimensión ' + (i + 1)); }

            var puntos = 0;
            var criterios = todos(dimension, '.criterio');
            criterios.forEach(function (criterio, j) {
                var descripcion = criterio.querySelector('[data-campo="descripcion"]');
                var puntaje = criterio.querySelector('[data-campo="puntajeMax"]');
                descripcion.name = 'dimensiones[' + i + '].criterios[' + j + '].descripcion';
                puntaje.name = 'dimensiones[' + i + '].criterios[' + j + '].puntajeMax';
                var quitarCriterio = criterio.querySelector('[data-quitar-criterio]');
                if (quitarCriterio) {
                    quitarCriterio.setAttribute('aria-label', 'Quitar el criterio ' + (j + 1) + ' de la dimensión ' + (i + 1));
                }
                var valor = numero(puntaje.value);
                if (valor > 0) { puntos += valor; }
            });

            dimension.querySelector('.dimension__meta').textContent = criterios.length
                + (criterios.length === 1 ? ' criterio' : ' criterios')
                + (puntos > 0 ? ', ' + formato(puntos) + ' pts' : '');
            criteriosTotal += criterios.length;
            puntosTotal += puntos;
        });

        form.querySelector('[data-resumen="dimensiones"]').textContent = dimensiones.length;
        form.querySelector('[data-resumen="criterios"]').textContent = criteriosTotal;
        form.querySelector('[data-resumen="puntaje"]').textContent = formato(puntosTotal);
    }

    // ---- Agregar
    function agregarCriterio(dimension, enfocar) {
        var nodo = plantillaCriterio.content.firstElementChild.cloneNode(true);
        dimension.querySelector('.criterios').appendChild(nodo);
        prepararCampos(nodo);
        dimension.querySelector('.dimension__error').hidden = true;
        reindexar();
        if (enfocar) { nodo.querySelector('[data-campo="descripcion"]').focus(); }
    }

    function agregarDimension() {
        var nodo = plantillaDimension.content.firstElementChild.cloneNode(true);
        lista.appendChild(nodo);
        prepararCampos(nodo);
        agregarCriterio(nodo, false);
        errorDimensiones.hidden = true;
        nodo.querySelector('details').open = true;
        nodo.scrollIntoView({ block: 'nearest' });
        nodo.querySelector('[data-campo="nombre"]').focus();
    }

    // ---- Confirmación antes de quitar
    var dialogo = document.getElementById('dialogo-confirmar');
    var pendiente = null;

    function confirmar(titulo, mensaje, alAceptar) {
        document.getElementById('confirmar-titulo').textContent = titulo;
        document.getElementById('confirmar-mensaje').textContent = mensaje;
        pendiente = alAceptar;
        if (typeof dialogo.showModal === 'function') {
            dialogo.showModal();
        } else if (window.confirm(mensaje)) {
            alAceptar();
        }
    }

    document.getElementById('confirmar-aceptar').addEventListener('click', function () {
        var accion = pendiente;
        pendiente = null;
        dialogo.close();
        if (accion) { accion(); }
    });
    document.getElementById('confirmar-cancelar').addEventListener('click', function () { dialogo.close(); });
    dialogo.addEventListener('close', function () { pendiente = null; });
    dialogo.addEventListener('click', function (evento) {
        if (evento.target === dialogo) { dialogo.close(); }
    });

    // ---- Validación en pantalla (el servidor sigue validando todo)
    var VALIDADORES = {
        texto: function (valor) { return valor.trim().length > 0; },
        aplicacion: function (valor) {
            var texto = valor.trim();
            return /^\d+$/.test(texto) && Number(texto) >= 1 && Number(texto) <= 10;
        },
        ponderacion: function (valor) {
            return /^\d+([.,]\d{1,2})?$/.test(valor.trim()) && numero(valor) > 0 && numero(valor) <= 100;
        },
        puntaje: function (valor) {
            return /^\d+([.,]\d{1,2})?$/.test(valor.trim()) && numero(valor) > 0;
        }
    };

    function marcar(control, valido) {
        var error = control.closest('.campo').querySelector('.campo__error');
        if (valido) {
            control.removeAttribute('aria-invalid');
            error.hidden = true;
            error.textContent = '';
        } else {
            control.setAttribute('aria-invalid', 'true');
            error.textContent = control.getAttribute('data-mensaje');
            error.hidden = false;
        }
    }

    function validarCampo(control) {
        var validador = VALIDADORES[control.getAttribute('data-validar')];
        var valido = validador ? validador(control.value) : true;
        marcar(control, valido);
        return valido;
    }

    function validarTodo() {
        var fallos = [];
        todos(form, '[data-validar]').forEach(function (control) {
            if (!validarCampo(control)) { fallos.push(control); }
        });
        todos(lista, '.dimension').forEach(function (dimension) {
            var sinCriterios = !dimension.querySelector('.criterio');
            dimension.querySelector('.dimension__error').hidden = !sinCriterios;
            if (sinCriterios) { fallos.push(dimension.querySelector('[data-agregar-criterio]')); }
        });
        var sinDimensiones = !lista.children.length;
        errorDimensiones.hidden = !sinDimensiones;
        if (sinDimensiones && botonAgregar) { fallos.push(botonAgregar); }
        return fallos;
    }

    form.addEventListener('submit', function (evento) {
        var fallos = validarTodo();
        if (!fallos.length) { return; }
        evento.preventDefault();
        fallos.forEach(function (control) {
            var detalle = control.closest('details');
            if (detalle) { detalle.open = true; }
        });
        fallos[0].focus();
        fallos[0].scrollIntoView({ block: 'center' });
    });

    // ---- Eventos
    form.addEventListener('input', function (evento) {
        var control = evento.target;
        if (control.getAttribute('aria-invalid') === 'true') { validarCampo(control); }
        reindexar();
    });

    form.addEventListener('focusout', function (evento) {
        var control = evento.target;
        var destino = evento.relatedTarget;
        // Si va a pulsar un botón no se muestra el error ahora: movería la página y el clic se perdería
        if (destino && destino.closest && destino.closest('button, a')) { return; }
        if (control.hasAttribute && control.hasAttribute('data-validar') && control.value.trim()) { validarCampo(control); }
    });

    form.addEventListener('click', function (evento) {
        var destino = evento.target.closest('button');
        if (!destino) { return; }

        if (destino.hasAttribute('data-quitar-dimension')) {
            evento.preventDefault();
            evento.stopPropagation();
            var dimension = destino.closest('.dimension');
            var nombre = dimension.querySelector('.dimension__titulo').textContent;
            var cantidad = dimension.querySelectorAll('.criterio').length;
            confirmar('Quitar dimensión',
                '¿Quitar la dimensión «' + nombre + '»'
                    + (cantidad === 1 ? ' y su criterio' : (cantidad ? ' y sus ' + cantidad + ' criterios' : '')) + '?',
                function () {
                    dimension.parentNode.removeChild(dimension);
                    reindexar();
                    if (botonAgregar) { botonAgregar.focus(); }
                });
        } else if (destino.hasAttribute('data-quitar-criterio')) {
            var criterio = destino.closest('.criterio');
            var dimensionDelCriterio = destino.closest('.dimension');
            var texto = criterio.querySelector('[data-campo="descripcion"]').value.trim();
            if (texto.length > 80) { texto = texto.slice(0, 77) + '…'; }
            confirmar('Quitar criterio', texto ? '¿Quitar el criterio «' + texto + '»?' : '¿Quitar este criterio?', function () {
                criterio.parentNode.removeChild(criterio);
                reindexar();
                var agregar = dimensionDelCriterio.querySelector('[data-agregar-criterio]');
                if (agregar) { agregar.focus(); }
            });
        } else if (destino.hasAttribute('data-agregar-criterio')) {
            agregarCriterio(destino.closest('.dimension'), true);
        } else if (destino.id === 'agregar-dimension') {
            agregarDimension();
        } else if (destino.hasAttribute('data-cerrar-alerta')) {
            destino.closest('.editor__alerta').hidden = true;
        } else if (destino.id === 'eliminar-pauta') {
            confirmar('Eliminar pauta',
                '¿Eliminar esta pauta completa, con sus dimensiones y criterios? No se puede deshacer.',
                function () { document.getElementById('form-eliminar').submit(); });
        }
    });

    prepararCampos(form);
    reindexar();
})();
