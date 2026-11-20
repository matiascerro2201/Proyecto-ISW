(function () {
    'use strict';

    var paneles = Array.prototype.slice.call(document.querySelectorAll('details.notif, details.ayuda'));
    if (!paneles.length) { return; }

    document.addEventListener('click', function (evento) {
        paneles.forEach(function (panel) {
            if (panel.open && !panel.contains(evento.target)) { panel.open = false; }
        });
    });

    document.addEventListener('keydown', function (evento) {
        if (evento.key !== 'Escape') { return; }
        paneles.forEach(function (panel) {
            if (!panel.open) { return; }
            panel.open = false;
            var boton = panel.querySelector('summary');
            if (boton) { boton.focus(); }
        });
    });
})();
