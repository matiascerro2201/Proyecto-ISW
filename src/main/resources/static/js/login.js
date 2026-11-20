(function () {
    'use strict';

    var boton = document.getElementById('ver-clave');
    var campo = document.getElementById('password');
    if (!boton || !campo) { return; }

    boton.hidden = false;
    boton.addEventListener('click', function () {
        var mostrar = campo.type === 'password';
        campo.type = mostrar ? 'text' : 'password';
        boton.textContent = mostrar ? 'Ocultar' : 'Mostrar';
        boton.setAttribute('aria-pressed', String(mostrar));
        boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
    });
})();
