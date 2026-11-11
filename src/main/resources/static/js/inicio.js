// Calendario: usa data-fecha de la lista de pendientes.
(function () {
    'use strict';

    var raiz = document.getElementById('calendario');
    if (!raiz) { return; }

    var MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio',
                 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    var DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

    var grilla = document.getElementById('cal-grilla');
    var titulo = document.getElementById('cal-titulo');
    var navegacion = raiz.querySelector('.cal__nav');
    var filtro = document.getElementById('filtro');
    var filtroTexto = document.getElementById('filtro-texto');
    var sinResultados = document.getElementById('sin-resultados');
    var items = Array.prototype.slice.call(document.querySelectorAll('#lista-pendientes .pendiente'));

    var porFecha = {};
    items.forEach(function (li) {
        var fecha = li.getAttribute('data-fecha');
        if (!fecha) { return; }
        var dia = porFecha[fecha] || (porFecha[fecha] = { total: 0, vencidas: 0 });
        dia.total += 1;
        if (li.getAttribute('data-vencida') === 'true') { dia.vencidas += 1; }
    });

    function dos(n) { return n < 10 ? '0' + n : String(n); }
    function clave(anio, mes, dia) { return anio + '-' + dos(mes + 1) + '-' + dos(dia); }

    var hoy = new Date();
    var claveHoy = clave(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    var mesVisible = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    var seleccionado = null;

    function elemento(etiqueta, clase, texto) {
        var el = document.createElement(etiqueta);
        if (clase) { el.className = clase; }
        if (texto !== undefined) { el.textContent = texto; }
        return el;
    }

    function descripcionDia(dia, mes, total) {
        var texto = dia + ' de ' + MESES[mes];
        if (total) { texto += ', ' + total + (total === 1 ? ' pendiente' : ' pendientes'); }
        return texto;
    }

    function pintar() {
        var anio = mesVisible.getFullYear();
        var mes = mesVisible.getMonth();
        titulo.textContent = MESES[mes] + ' ' + anio;

        var desfase = (new Date(anio, mes, 1).getDay() + 6) % 7;
        var diasDelMes = new Date(anio, mes + 1, 0).getDate();

        grilla.textContent = '';
        DIAS.forEach(function (nombre) {
            var cabecera = elemento('span', 'cal__dsem', nombre);
            cabecera.setAttribute('aria-hidden', 'true');
            grilla.appendChild(cabecera);
        });
        for (var i = 0; i < desfase; i++) { grilla.appendChild(elemento('span', 'cal__relleno')); }

        for (var d = 1; d <= diasDelMes; d++) {
            var k = clave(anio, mes, d);
            var datos = porFecha[k];
            var boton = elemento('button', 'cal__dia');
            boton.type = 'button';
            boton.setAttribute('data-clave', k);
            boton.setAttribute('aria-label', descripcionDia(d, mes, datos ? datos.total : 0));
            boton.setAttribute('aria-pressed', k === seleccionado ? 'true' : 'false');
            if (k === claveHoy) { boton.setAttribute('aria-current', 'date'); }

            boton.appendChild(document.createTextNode(String(d)));
            var marcas = elemento('span', 'cal__marcas');
            if (datos) {
                for (var m = 0; m < Math.min(datos.total, 3); m++) {
                    marcas.appendChild(elemento('span', m < datos.vencidas ? 'cal__marca cal__marca--vencida' : 'cal__marca'));
                }
            }
            boton.appendChild(marcas);
            grilla.appendChild(boton);
        }
    }

    function aplicarFiltro() {
        var visibles = 0;
        items.forEach(function (li) {
            var mostrar = !seleccionado || li.getAttribute('data-fecha') === seleccionado;
            li.hidden = !mostrar;
            if (mostrar) { visibles += 1; }
        });

        if (filtro) {
            filtro.hidden = !seleccionado;
            if (seleccionado) {
                var partes = seleccionado.split('-');
                filtroTexto.textContent = 'Mostrando los pendientes del ' +
                    parseInt(partes[2], 10) + ' de ' + MESES[parseInt(partes[1], 10) - 1];
            }
        }
        if (sinResultados) { sinResultados.hidden = !(seleccionado && visibles === 0); }
    }

    function elegir(k) {
        seleccionado = (seleccionado === k) ? null : k;
        pintar();
        aplicarFiltro();
        if (seleccionado) {
            var activo = grilla.querySelector('[data-clave="' + seleccionado + '"]');
            if (activo) { activo.focus(); }
        }
    }

    grilla.addEventListener('click', function (evento) {
        var boton = evento.target.closest ? evento.target.closest('.cal__dia') : null;
        if (boton) { elegir(boton.getAttribute('data-clave')); }
    });

    document.getElementById('mes-anterior').addEventListener('click', function () {
        mesVisible = new Date(mesVisible.getFullYear(), mesVisible.getMonth() - 1, 1);
        pintar();
    });
    document.getElementById('mes-siguiente').addEventListener('click', function () {
        mesVisible = new Date(mesVisible.getFullYear(), mesVisible.getMonth() + 1, 1);
        pintar();
    });

    var quitar = document.getElementById('quitar-filtro');
    if (quitar) {
        quitar.addEventListener('click', function () {
            seleccionado = null;
            pintar();
            aplicarFiltro();
        });
    }

    if (navegacion) { navegacion.hidden = false; }
    pintar();
    aplicarFiltro();
})();
