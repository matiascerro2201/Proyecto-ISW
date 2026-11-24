// Vistas del coordinador: ventana emergente del estudiante, confirmaciones y campos según el rol.
document.addEventListener('DOMContentLoaded', () => {
    // Pop-up de estudiantes. Si algo falla, el enlace sigue llevando a la página completa.
    const dialogo = document.getElementById('coord-dialogo');
    const contenido = document.getElementById('coord-dialogo-contenido');
    if (dialogo && contenido && typeof dialogo.showModal === 'function') {
        document.querySelectorAll('[data-estudiante-popup]').forEach(enlace => {
            enlace.addEventListener('click', async evento => {
                if (evento.ctrlKey || evento.metaKey || evento.shiftKey) { return; }
                evento.preventDefault();
                contenido.textContent = 'Cargando…';
                dialogo.showModal();
                try {
                    const respuesta = await fetch(enlace.href + '?fragmento', { headers: { 'X-Requested-With': 'fetch' } });
                    if (!respuesta.ok) { throw new Error(respuesta.status); }
                    contenido.innerHTML = await respuesta.text();
                } catch (error) {
                    dialogo.close();
                    window.location.href = enlace.href;
                }
            });
        });
        dialogo.querySelector('[data-cerrar-dialogo]')?.addEventListener('click', () => dialogo.close());
        dialogo.addEventListener('click', evento => { if (evento.target === dialogo) { dialogo.close(); } });
    }

    // Confirmación antes de enviar formularios marcados. Se escucha en el documento porque algunos formularios
    // (los de las ventanas de detalle) se copian a la página después de cargarla.
    document.addEventListener('submit', evento => {
        const formulario = evento.target.closest ? evento.target.closest('form[data-confirmar]') : null;
        if (formulario && !window.confirm(formulario.dataset.confirmar)) { evento.preventDefault(); }
    });

    // Botones de ayuda "?": el texto se muestra al pasar el mouse, al enfocar y al pulsar; se cierra con Escape o al
    // pulsar fuera.
    const ayudas = document.querySelectorAll('.ayuda');
    const cerrarAyudas = () => {
        ayudas.forEach(ayuda => {
            ayuda.classList.remove('ayuda--abierta');
            ayuda.querySelector('[data-ayuda]')?.setAttribute('aria-expanded', 'false');
        });
    };
    ayudas.forEach(ayuda => {
        const boton = ayuda.querySelector('[data-ayuda]');
        boton?.addEventListener('click', evento => {
            evento.stopPropagation();
            const abrir = !ayuda.classList.contains('ayuda--abierta');
            cerrarAyudas();
            ayuda.classList.toggle('ayuda--abierta', abrir);
            boton.setAttribute('aria-expanded', String(abrir));
        });
    });
    document.addEventListener('click', cerrarAyudas);
    document.addEventListener('keydown', evento => { if (evento.key === 'Escape') { cerrarAyudas(); } });

    // Muestra solo los campos que corresponden al rol elegido.
    const rol = document.getElementById('campo-rol');
    if (rol) {
        const actualizar = () => {
            const valor = rol.value || document.querySelector('input[type=hidden][name=rol]')?.value;
            document.querySelectorAll('[data-solo-rol]').forEach(campo => {
                const aplica = campo.dataset.soloRol === valor;
                campo.hidden = !aplica;
                campo.querySelectorAll('input, select').forEach(c => { c.disabled = !aplica; });
            });
        };
        rol.addEventListener('change', actualizar);
        actualizar();
    }
});
