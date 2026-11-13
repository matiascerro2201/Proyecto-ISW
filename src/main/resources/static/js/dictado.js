// Dictado por voz de los comentarios: botones .btn-voz[data-target] y su textarea.
(function () {
    'use strict';

    var botones = Array.prototype.slice.call(document.querySelectorAll('.btn-voz'));
    if (!botones.length) { return; }

    var Reconocimiento = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Reconocimiento) {
        botones.forEach(function (boton) { boton.hidden = true; });
        return;
    }

    var reconocimiento = new Reconocimiento();
    reconocimiento.lang = 'es-CL';
    reconocimiento.continuous = true;
    reconocimiento.interimResults = true;

    var area = null;
    var botonActivo = null;
    var grabando = false;
    var consolidado = '';

    // "coma", "punto", "nueva línea"... se convierten en signos y se corrigen las mayúsculas
    function formatear(texto) {
        var procesado = texto
            .replace(/\s+punto y coma/gi, ';')
            .replace(/\s+punto aparte/gi, '.\n')
            .replace(/\s+nueva l[ií]nea/gi, '\n')
            .replace(/\s+dos puntos/gi, ':')
            .replace(/\s+coma/gi, ',')
            .replace(/\s+punto/gi, '.')
            .replace(/\s+abrir interrogaci[oó]n/gi, ' ¿')
            .replace(/\s+cerrar interrogaci[oó]n/gi, '?')
            .replace(/\s+signo de interrogaci[oó]n/gi, '?')
            .replace(/\s+abrir exclamaci[oó]n/gi, ' ¡')
            .replace(/\s+cerrar exclamaci[oó]n/gi, '!');

        procesado = procesado.replace(/([.,;:?!])(?=[a-zA-ZáéíóúÁÉÍÓÚñÑ¿¡])/g, '$1 ');
        procesado = procesado.replace(/[ ]{2,}/g, ' ');
        procesado = procesado.replace(/(^\s*[a-záéíóúñ]|[.?!]\s+[a-záéíóúñ]|\n\s*[a-záéíóúñ])/g, function (coincidencia) {
            return coincidencia.toUpperCase();
        });
        return procesado;
    }

    function mostrar(boton, activo) {
        boton.classList.toggle('grabando', activo);
        boton.setAttribute('aria-pressed', activo ? 'true' : 'false');
        boton.querySelector('.btn-voz__texto').textContent = activo ? 'Detener dictado' : 'Dictar nota de voz';
    }

    function detener() {
        grabando = false;
        reconocimiento.stop();
        if (botonActivo) { mostrar(botonActivo, false); }
        botonActivo = null;
        area = null;
    }

    botones.forEach(function (boton) {
        boton.setAttribute('aria-pressed', 'false');
        boton.addEventListener('click', function () {
            var destino = document.getElementById(boton.getAttribute('data-target'));
            if (grabando && botonActivo === boton) {
                detener();
                return;
            }
            if (grabando) { detener(); }

            area = destino;
            botonActivo = boton;
            consolidado = area.value.trim();
            grabando = true;
            try {
                reconocimiento.start();
            } catch (e) {
                detener();
            }
        });
    });

    reconocimiento.addEventListener('start', function () {
        if (botonActivo) { mostrar(botonActivo, true); }
    });

    reconocimiento.addEventListener('result', function (evento) {
        if (!area) { return; }
        var provisional = '';
        for (var i = evento.resultIndex; i < evento.results.length; i++) {
            var transcripcion = evento.results[i][0].transcript;
            if (evento.results[i].isFinal) {
                var separador = (consolidado && !consolidado.endsWith('\n')) ? ' ' : '';
                consolidado = formatear(consolidado + separador + transcripcion.trim());
            } else {
                provisional += transcripcion;
            }
        }
        var union = (consolidado && provisional) ? ' ' : '';
        area.value = formatear(consolidado + union + provisional);
        area.scrollTop = area.scrollHeight;
    });

    reconocimiento.addEventListener('error', function (evento) {
        if (evento.error === 'no-speech') { return; }
        if (evento.error === 'not-allowed') {
            window.alert('Debes permitir el acceso al micrófono en tu navegador para dictar notas de voz.');
            detener();
        }
    });

    // El navegador corta la escucha tras una pausa larga: se reinicia mientras el usuario no la detenga
    reconocimiento.addEventListener('end', function () {
        if (!grabando) { return; }
        consolidado = area ? area.value.trim() : consolidado;
        try {
            reconocimiento.start();
        } catch (e) {
            detener();
        }
    });
})();
