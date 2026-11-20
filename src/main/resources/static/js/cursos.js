// Abre la lista de estudiantes del curso en una ventana.
(function () {
    'use strict';

    var dialogo = document.getElementById('dialogo-curso');
    if (!dialogo || typeof dialogo.showModal !== 'function') { return; }

    var titulo = document.getElementById('dialogo-titulo');
    var cuerpo = document.getElementById('dialogo-cuerpo');
    var cerrar = document.getElementById('dialogo-cerrar');
    var pedido = 0;

    function parrafo(texto) {
        var p = document.createElement('p');
        p.className = 'vacio';
        p.textContent = texto;
        return p;
    }

    function mostrarError(url) {
        cuerpo.textContent = '';
        var p = parrafo('No se pudo cargar la lista de estudiantes. ');
        var enlace = document.createElement('a');
        enlace.href = url;
        enlace.textContent = 'Abrirla en una página';
        p.appendChild(enlace);
        cuerpo.appendChild(p);
    }

    function abrir(tarjeta) {
        var url = tarjeta.getAttribute('href');
        var numero = ++pedido;
        var nombre = tarjeta.querySelector('.curso__nombre');

        titulo.textContent = nombre ? nombre.textContent : 'Estudiantes';
        cuerpo.textContent = '';
        cuerpo.appendChild(parrafo('Cargando estudiantes…'));
        if (!dialogo.open) { dialogo.showModal(); }

        fetch(url + (url.indexOf('?') === -1 ? '?' : '&') + 'fragmento=1', {
            credentials: 'same-origin',
            headers: { 'X-Requested-With': 'fetch' }
        }).then(function (respuesta) {
            // sesión vencida: que el navegador pase por el login
            if (respuesta.redirected) {
                window.location.href = url;
                return null;
            }
            if (!respuesta.ok) { throw new Error(String(respuesta.status)); }
            return respuesta.text();
        }).then(function (html) {
            if (html === null || numero !== pedido) { return; }
            cuerpo.innerHTML = html;
        }).catch(function () {
            if (numero === pedido) { mostrarError(url); }
        });
    }

    document.addEventListener('click', function (evento) {
        var tarjeta = evento.target.closest ? evento.target.closest('a.curso[data-ventana]') : null;
        if (!tarjeta) { return; }
        if (evento.defaultPrevented || evento.button !== 0 || evento.metaKey || evento.ctrlKey
                || evento.shiftKey || evento.altKey) { return; }
        evento.preventDefault();
        abrir(tarjeta);
    });

    cerrar.addEventListener('click', function () { dialogo.close(); });
    dialogo.addEventListener('click', function (evento) {
        if (evento.target === dialogo) { dialogo.close(); }
    });
    dialogo.addEventListener('close', function () { pedido++; });
})();
