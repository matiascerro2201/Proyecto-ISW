// Formularios donde se crea o cambia una contraseña: pide repetirla y avisa si Bloq Mayús está activado.
// Se activa con form[data-formulario-clave] y los campos [data-clave] y [data-clave-confirmacion].
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('form[data-formulario-clave]').forEach(formulario => {
        const clave = formulario.querySelector('[data-clave]');
        const confirmacion = formulario.querySelector('[data-clave-confirmacion]');
        const avisoMayus = formulario.querySelector('[data-aviso-mayus]');
        const avisoCoincidencia = formulario.querySelector('[data-aviso-coincidencia]');
        if (!clave || !confirmacion) { return; }

        // Aviso de Bloq Mayús: solo se sabe al pulsar una tecla, así que se revisa en cada una.
        const revisarMayusculas = evento => {
            if (!avisoMayus || typeof evento.getModifierState !== 'function') { return; }
            avisoMayus.hidden = !evento.getModifierState('CapsLock');
        };
        [clave, confirmacion].forEach(campo => {
            campo.addEventListener('keydown', revisarMayusculas);
            campo.addEventListener('keyup', revisarMayusculas);
            campo.addEventListener('blur', () => { if (avisoMayus) { avisoMayus.hidden = true; } });
        });

        // Las dos contraseñas deben ser iguales. Al editar, repetirla solo es obligatorio si se escribió una nueva.
        const revisarCoincidencia = () => {
            confirmacion.required = clave.value.length > 0;
            const distintas = confirmacion.value.length > 0 && clave.value !== confirmacion.value;
            confirmacion.setCustomValidity(distintas ? 'Las contraseñas no coinciden.' : '');
            if (avisoCoincidencia) { avisoCoincidencia.hidden = !distintas; }
        };
        clave.addEventListener('input', revisarCoincidencia);
        confirmacion.addEventListener('input', revisarCoincidencia);
        revisarCoincidencia();
    });
});
