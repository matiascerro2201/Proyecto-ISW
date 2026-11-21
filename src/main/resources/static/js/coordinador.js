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

    // Confirmación antes de enviar formularios marcados.
    document.querySelectorAll('form[data-confirmar]').forEach(formulario => {
        formulario.addEventListener('submit', evento => {
            if (!window.confirm(formulario.dataset.confirmar)) { evento.preventDefault(); }
        });
    });

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
