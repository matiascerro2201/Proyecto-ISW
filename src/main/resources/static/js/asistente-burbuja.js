// Burbuja flotante del asistente: abre el chat de la planificación en un panel sobre la misma página,
// de modo que el estudiante conversa sin perder de vista su PDF. El chat es la pantalla de siempre
// (/chat), cargada dentro de un marco; esa pantalla se adapta sola cuando detecta que está embebida.
(function () {
    'use strict';

    const raiz = document.getElementById('asistente');
    if (!raiz) {
        return;
    }

    const boton = document.getElementById('asistente-boton');
    const saludo = document.getElementById('asistente-saludo');
    const panel = document.getElementById('asistente-panel');
    const marco = document.getElementById('asistente-marco');
    const cerrar = document.getElementById('asistente-cerrar');

    function abrir() {
        // El chat se carga la primera vez que se abre, no al entrar a la página.
        if (!marco.getAttribute('src')) {
            marco.setAttribute('src', raiz.dataset.urlChat);
        }
        panel.hidden = false;
        saludo.hidden = true;
        boton.setAttribute('aria-expanded', 'true');
        boton.setAttribute('aria-label', 'Cerrar el asistente');
    }

    function cerrarPanel(devolverFoco) {
        panel.hidden = true;
        boton.setAttribute('aria-expanded', 'false');
        boton.setAttribute('aria-label', 'Abrir el asistente');
        if (devolverFoco) {
            boton.focus();
        }
    }

    boton.addEventListener('click', function () {
        if (panel.hidden) {
            abrir();
        } else {
            cerrarPanel(true);
        }
    });
    saludo.addEventListener('click', abrir);
    cerrar.addEventListener('click', function () {
        cerrarPanel(true);
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !panel.hidden) {
            cerrarPanel(true);
        }
    });
})();
