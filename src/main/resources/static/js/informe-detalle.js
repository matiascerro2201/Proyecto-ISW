// Informe de un estudiante: abre la ventana de revisión (botón o #revisar).
(function () {
    'use strict';

    var dialogo = document.getElementById('dialogo-revision');
    var abrir = document.getElementById('abrir-revision');
    if (!dialogo || !abrir || typeof dialogo.showModal !== 'function') { return; }

    function mostrar() {
        if (!dialogo.open) { dialogo.showModal(); }
    }

    abrir.addEventListener('click', mostrar);
    document.getElementById('cerrar-revision').addEventListener('click', function () { dialogo.close(); });
    dialogo.addEventListener('click', function (evento) {
        if (evento.target === dialogo) { dialogo.close(); }
    });

    if (window.location.hash === '#revisar') { mostrar(); }
})();
