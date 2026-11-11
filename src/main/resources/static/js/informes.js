// Informes finales: búsqueda con sugerencias, filtros de asignaturas y ventana con sus estudiantes.
(function () {
    'use strict';

    var lista = document.getElementById('lista-asignaturas');
    var dialogo = document.getElementById('dialogo-informes');
    if (!lista || !dialogo || typeof dialogo.showModal !== 'function') { return; }

    var buscar = document.getElementById('buscar');
    var sugerencias = document.getElementById('sugerencias');
    var periodo = document.getElementById('filtro-periodo');
    var plazo = document.getElementById('filtro-plazo');
    var pendientes = document.getElementById('filtro-pendientes');
    var limpiar = document.getElementById('limpiar-filtros');
    var contador = document.getElementById('contador');
    var vacio = document.getElementById('sin-resultados');
    var titulo = document.getElementById('dialogo-informes-titulo');
    var cuerpo = document.getElementById('dialogo-informes-cuerpo');
    var cerrar = document.getElementById('dialogo-informes-cerrar');

    function normalizar(texto) {
        return texto.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function palabrasDe(texto) {
        return normalizar(texto.trim()).split(/\s+/).filter(Boolean);
    }

    var filas = Array.prototype.slice.call(lista.querySelectorAll('.asignatura')).map(function (li) {
        var nombre = li.querySelector('.asignatura__nombre').textContent;
        var periodoTexto = li.getAttribute('data-periodo');
        return {
            li: li,
            nombre: nombre,
            nombreNorm: normalizar(nombre),
            texto: normalizar(nombre + ' ' + periodoTexto),
            periodo: periodoTexto,
            orden: parseInt(li.getAttribute('data-orden'), 10),
            plazo: li.getAttribute('data-plazo'),
            pendientes: parseInt(li.getAttribute('data-pendientes'), 10)
        };
    });

    function contiene(fila, palabras) {
        return palabras.every(function (palabra) { return fila.texto.indexOf(palabra) !== -1; });
    }

    var vistos = {};
    filas.slice().sort(function (a, b) { return b.orden - a.orden; }).forEach(function (fila) {
        if (vistos[fila.periodo]) { return; }
        vistos[fila.periodo] = true;
        var opcion = document.createElement('option');
        opcion.value = fila.periodo;
        opcion.textContent = fila.periodo;
        periodo.appendChild(opcion);
    });

    function filtrar() {
        var palabras = palabrasDe(buscar.value);
        var visibles = 0;
        filas.forEach(function (fila) {
            var coincide = (!palabras.length || contiene(fila, palabras))
                && (!periodo.value || fila.periodo === periodo.value)
                && (!plazo.value || fila.plazo === plazo.value)
                && (!pendientes.checked || fila.pendientes > 0);
            fila.li.hidden = !coincide;
            if (coincide) { visibles += 1; }
        });
        contador.textContent = visibles + (visibles === 1 ? ' asignatura' : ' asignaturas');
        vacio.hidden = visibles !== 0;
    }

    // ---- Sugerencias mientras se escribe
    var sugeridas = [];
    var activa = -1;

    function sugerir() {
        var palabras = palabrasDe(buscar.value);
        if (!palabras.length) { return []; }
        return filas
            .filter(function (fila) { return contiene(fila, palabras); })
            .map(function (fila) {
                var rango = fila.nombreNorm.indexOf(palabras[0]) === 0 ? 0
                    : (fila.nombreNorm.indexOf(' ' + palabras[0]) !== -1 ? 1 : 2);
                return { fila: fila, rango: rango };
            })
            .sort(function (a, b) {
                return a.rango - b.rango || b.fila.orden - a.fila.orden || a.fila.nombreNorm.localeCompare(b.fila.nombreNorm);
            })
            .slice(0, 8)
            .map(function (x) { return x.fila; });
    }

    function resaltar(destino, texto, palabras) {
        var norm = normalizar(texto);
        if (norm.length !== texto.length) { destino.textContent = texto; return; }
        var rangos = [];
        palabras.forEach(function (palabra) {
            var i = norm.indexOf(palabra);
            if (i !== -1) { rangos.push([i, i + palabra.length]); }
        });
        rangos.sort(function (a, b) { return a[0] - b[0]; });
        var unidos = [];
        rangos.forEach(function (rango) {
            var ultimo = unidos[unidos.length - 1];
            if (ultimo && rango[0] <= ultimo[1]) { ultimo[1] = Math.max(ultimo[1], rango[1]); }
            else { unidos.push([rango[0], rango[1]]); }
        });
        var pos = 0;
        unidos.forEach(function (rango) {
            if (rango[0] > pos) { destino.appendChild(document.createTextNode(texto.slice(pos, rango[0]))); }
            var marca = document.createElement('mark');
            marca.textContent = texto.slice(rango[0], rango[1]);
            destino.appendChild(marca);
            pos = rango[1];
        });
        if (pos < texto.length) { destino.appendChild(document.createTextNode(texto.slice(pos))); }
    }

    function cerrarSugerencias() {
        sugerencias.hidden = true;
        buscar.setAttribute('aria-expanded', 'false');
        buscar.removeAttribute('aria-activedescendant');
        activa = -1;
    }

    function marcar(indice) {
        var opciones = sugerencias.children;
        for (var i = 0; i < opciones.length; i++) {
            opciones[i].setAttribute('aria-selected', i === indice ? 'true' : 'false');
        }
        activa = indice;
        if (indice >= 0) {
            buscar.setAttribute('aria-activedescendant', opciones[indice].id);
            opciones[indice].scrollIntoView({ block: 'nearest' });
        } else {
            buscar.removeAttribute('aria-activedescendant');
        }
    }

    function elegir(fila) {
        buscar.value = fila.nombre;
        periodo.value = fila.periodo;
        cerrarSugerencias();
        filtrar();
    }

    function mostrarSugerencias() {
        sugeridas = sugerir();
        sugerencias.textContent = '';
        activa = -1;
        if (!sugeridas.length) { cerrarSugerencias(); return; }
        var palabras = palabrasDe(buscar.value);
        sugeridas.forEach(function (fila, i) {
            var opcion = document.createElement('li');
            opcion.className = 'sugerencia';
            opcion.id = 'sugerencia-' + i;
            opcion.setAttribute('role', 'option');
            opcion.setAttribute('aria-selected', 'false');
            var nombre = document.createElement('span');
            nombre.className = 'sugerencia__nombre';
            resaltar(nombre, fila.nombre, palabras);
            var detalle = document.createElement('span');
            detalle.className = 'sugerencia__periodo';
            detalle.textContent = fila.periodo;
            opcion.appendChild(nombre);
            opcion.appendChild(detalle);
            opcion.addEventListener('mousedown', function (evento) {
                evento.preventDefault();
                elegir(fila);
            });
            sugerencias.appendChild(opcion);
        });
        sugerencias.hidden = false;
        buscar.setAttribute('aria-expanded', 'true');
    }

    buscar.addEventListener('input', function () {
        filtrar();
        mostrarSugerencias();
    });
    buscar.addEventListener('focus', function () {
        if (buscar.value.trim()) { mostrarSugerencias(); }
    });
    buscar.addEventListener('blur', cerrarSugerencias);
    buscar.addEventListener('keydown', function (evento) {
        var abiertas = !sugerencias.hidden;
        if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
            if (!abiertas) { mostrarSugerencias(); }
            if (sugerencias.hidden) { return; }
            evento.preventDefault();
            var total = sugeridas.length;
            marcar(evento.key === 'ArrowDown' ? (activa + 1) % total : (activa - 1 + total) % total);
        } else if (evento.key === 'Enter' && abiertas && activa >= 0) {
            evento.preventDefault();
            elegir(sugeridas[activa]);
        } else if (evento.key === 'Escape' && abiertas) {
            evento.preventDefault();
            cerrarSugerencias();
        }
    });

    periodo.addEventListener('change', filtrar);
    plazo.addEventListener('change', filtrar);
    pendientes.addEventListener('change', filtrar);
    limpiar.addEventListener('click', function () {
        buscar.value = '';
        periodo.value = '';
        plazo.value = '';
        pendientes.checked = false;
        cerrarSugerencias();
        filtrar();
        buscar.focus();
    });

    // ---- Ventana con los estudiantes de la asignatura
    function abrir(boton) {
        var plantilla = document.getElementById('plantilla-' + boton.getAttribute('data-seccion'));
        if (!plantilla) { return; }
        titulo.textContent = boton.querySelector('.asignatura__nombre').textContent;
        cuerpo.textContent = '';
        cuerpo.appendChild(plantilla.content.cloneNode(true));
        if (!cuerpo.querySelector('.estudiantes li')) {
            var aviso = document.createElement('p');
            aviso.className = 'vacio';
            aviso.textContent = 'Esta asignatura aún no tiene estudiantes inscritos.';
            cuerpo.appendChild(aviso);
        }
        if (!dialogo.open) { dialogo.showModal(); }
    }

    lista.addEventListener('click', function (evento) {
        var boton = evento.target.closest ? evento.target.closest('.asignatura__fila') : null;
        if (boton) { abrir(boton); }
    });

    cerrar.addEventListener('click', function () { dialogo.close(); });
    dialogo.addEventListener('click', function (evento) {
        if (evento.target === dialogo) { dialogo.close(); }
    });

    var enlace = /^#curso-(\d+)$/.exec(window.location.hash);
    if (enlace) {
        var boton = lista.querySelector('.asignatura__fila[data-seccion="' + enlace[1] + '"]');
        if (boton) { abrir(boton); }
    }
})();
