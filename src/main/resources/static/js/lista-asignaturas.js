// Lista con búsqueda y sugerencias, filtros y ventana de detalle. La usan informes, pautas, estudiantes y planificaciones.
(function () {
    'use strict';

    var lista = document.getElementById('lista-asignaturas');
    var dialogo = document.getElementById('dialogo-lista');
    if (!lista || !dialogo || typeof dialogo.showModal !== 'function') { return; }

    var buscar = document.getElementById('buscar');
    var sugerencias = document.getElementById('sugerencias');
    var periodo = document.getElementById('filtro-periodo');
    var estado = document.getElementById('filtro-estado');
    var linea = document.getElementById('filtro-linea');
    var desde = document.getElementById('filtro-desde');
    var hasta = document.getElementById('filtro-hasta');
    var avisoFechas = document.getElementById('filtro-fechas-aviso');
    var pendientes = document.getElementById('filtro-pendientes');
    var limpiar = document.getElementById('limpiar-filtros');
    var contador = document.getElementById('contador');
    var vacio = document.getElementById('sin-resultados');
    var titulo = document.getElementById('dialogo-lista-titulo');
    var cuerpo = document.getElementById('dialogo-lista-cuerpo');
    var cerrar = document.getElementById('dialogo-lista-cerrar');

    var singular = lista.getAttribute('data-singular') || 'asignatura';
    var plural = lista.getAttribute('data-plural') || 'asignaturas';
    var sugerenciasUnicas = lista.hasAttribute('data-sugerencias-unicas');

    function normalizar(texto) {
        return texto.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function palabrasDe(texto) {
        return normalizar(texto.trim()).split(/\s+/).filter(Boolean);
    }

    var filas = Array.prototype.slice.call(lista.querySelectorAll('.asignatura')).map(function (li) {
        var nombre = li.querySelector('.asignatura__nombre').textContent;
        var secundario = li.querySelector('.asignatura__periodo');
        var periodoTexto = li.getAttribute('data-periodo') || '';
        return {
            li: li,
            nombre: nombre,
            nombreNorm: normalizar(nombre),
            detalle: secundario ? secundario.textContent : periodoTexto,
            texto: normalizar(nombre + ' ' + periodoTexto + ' ' + (li.getAttribute('data-busqueda') || '')),
            periodo: periodoTexto,
            orden: parseInt(li.getAttribute('data-orden') || '0', 10),
            estado: li.getAttribute('data-estado'),
            linea: li.getAttribute('data-linea') || '',
            fecha: li.getAttribute('data-fecha') || '',
            pendientes: parseInt(li.getAttribute('data-pendientes') || '0', 10)
        };
    });

    function contiene(fila, palabras) {
        return palabras.every(function (palabra) { return fila.texto.indexOf(palabra) !== -1; });
    }

    if (periodo) {
        var vistos = {};
        filas.slice().sort(function (a, b) { return b.orden - a.orden; }).forEach(function (fila) {
            if (!fila.periodo || vistos[fila.periodo]) { return; }
            vistos[fila.periodo] = true;
            var opcion = document.createElement('option');
            opcion.value = fila.periodo;
            opcion.textContent = fila.periodo;
            periodo.appendChild(opcion);
        });
    }

    if (linea) {
        var lineasVistas = {};
        filas.map(function (fila) { return fila.linea; }).filter(Boolean).sort(function (a, b) { return a.localeCompare(b); })
            .forEach(function (nombre) {
                if (lineasVistas[nombre]) { return; }
                lineasVistas[nombre] = true;
                var opcion = document.createElement('option');
                opcion.value = nombre;
                opcion.textContent = nombre;
                linea.appendChild(opcion);
            });
    }

    // Las fechas son aaaa-mm-dd, así que se comparan como texto. Sin fecha (un borrador) no entra en un intervalo.
    function enIntervalo(fila) {
        var inicio = desde ? desde.value : '';
        var fin = hasta ? hasta.value : '';
        if (!inicio && !fin) { return true; }
        if (!fila.fecha) { return false; }
        return (!inicio || fila.fecha >= inicio) && (!fin || fila.fecha <= fin);
    }

    function avisarFechas() {
        if (!avisoFechas) { return; }
        var inicio = desde.value;
        var fin = hasta.value;
        var mensaje = '';
        if (inicio && fin && inicio > fin) {
            mensaje = 'La fecha inicial es posterior a la final: no hay nada entre esas dos fechas.';
        } else if ((inicio || fin) && filas.some(function (fila) { return !fila.fecha; })) {
            mensaje = 'Los borradores no tienen fecha de envío, por eso no aparecen al filtrar por fechas.';
        }
        avisoFechas.textContent = mensaje;
        avisoFechas.hidden = !mensaje;
    }

    function filtrar() {
        var palabras = palabrasDe(buscar.value);
        var visibles = 0;
        filas.forEach(function (fila) {
            var coincide = (!palabras.length || contiene(fila, palabras))
                && (!periodo || !periodo.value || fila.periodo === periodo.value)
                && (!estado || !estado.value || fila.estado === estado.value)
                && (!linea || !linea.value || fila.linea === linea.value)
                && enIntervalo(fila)
                && (!pendientes || !pendientes.checked || fila.pendientes > 0);
            fila.li.hidden = !coincide;
            if (coincide) { visibles += 1; }
        });
        contador.textContent = visibles + ' ' + (visibles === 1 ? singular : plural);
        vacio.hidden = visibles !== 0;
        avisarFechas();
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
            .map(function (x) { return x.fila; })
            .filter(function (fila, i, todas) {
                return !sugerenciasUnicas || todas.findIndex(function (otra) { return otra.nombreNorm === fila.nombreNorm; }) === i;
            })
            .slice(0, 8);
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
        if (periodo && fila.periodo) { periodo.value = fila.periodo; }
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
            opcion.appendChild(nombre);
            if (fila.detalle && !sugerenciasUnicas) {
                var detalle = document.createElement('span');
                detalle.className = 'sugerencia__periodo';
                detalle.textContent = fila.detalle;
                opcion.appendChild(detalle);
            }
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

    [periodo, estado, linea, desde, hasta, pendientes].forEach(function (control) {
        if (control) { control.addEventListener('change', filtrar); }
    });
    limpiar.addEventListener('click', function () {
        buscar.value = '';
        if (periodo) { periodo.value = ''; }
        if (estado) { estado.value = ''; }
        if (linea) { linea.value = ''; }
        if (desde) { desde.value = ''; }
        if (hasta) { hasta.value = ''; }
        if (pendientes) { pendientes.checked = false; }
        cerrarSugerencias();
        filtrar();
        buscar.focus();
    });

    // ---- Ventana de detalle de la asignatura
    function abrir(boton) {
        var plantilla = document.getElementById('plantilla-' + boton.getAttribute('data-id'));
        if (!plantilla) { return; }
        titulo.textContent = boton.getAttribute('data-titulo') || boton.querySelector('.asignatura__nombre').textContent;
        cuerpo.textContent = '';
        cuerpo.appendChild(plantilla.content.cloneNode(true));
        Array.prototype.forEach.call(cuerpo.querySelectorAll('[data-vacio]'), function (contenedor) {
            if (contenedor.querySelector('li')) { return; }
            var aviso = document.createElement('p');
            aviso.className = 'vacio';
            aviso.textContent = contenedor.getAttribute('data-vacio');
            contenedor.parentNode.insertBefore(aviso, contenedor.nextSibling);
        });
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

    // Enlace directo a una asignatura: #<prefijo>-<id>, con el prefijo en data-hash de la lista
    var prefijo = lista.getAttribute('data-hash');
    var enlace = prefijo ? new RegExp('^#' + prefijo + '-(\\d+)$').exec(window.location.hash) : null;
    if (enlace) {
        var boton = lista.querySelector('.asignatura__fila[data-id="' + enlace[1] + '"], .asignatura__fila[data-ids~="' + enlace[1] + '"]');
        if (boton) { abrir(boton); }
    }
})();
