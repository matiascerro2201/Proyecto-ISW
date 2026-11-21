// Subida del PDF de la planificación con barra de progreso (HU-31/32).
// Usa XMLHttpRequest porque fetch no informa el avance de la subida.
(function () {
    'use strict';

    const raiz = document.getElementById('subida-pdf');
    if (!raiz) {
        return;
    }

    const MAX_BYTES = 20 * 1024 * 1024;
    const urlSubida = raiz.dataset.urlSubida;
    const idPlanificacion = raiz.dataset.idPlanificacion || '';
    const urlEditar = raiz.dataset.urlEditar;

    const entrada = document.getElementById('archivo-pdf');
    const boton = document.getElementById('boton-subir');
    const barra = document.getElementById('progreso-subida');
    const estado = document.getElementById('estado-subida');
    const avisos = document.getElementById('avisos-subida');
    const selectorLinea = document.getElementById('idLinea');

    const token = document.querySelector('meta[name="_csrf"]').content;
    const cabeceraToken = document.querySelector('meta[name="_csrf_header"]').content;

    const claveAvisos = 'pdf-avisos-' + idPlanificacion;
    const claveMensaje = 'pdf-mensaje';

    function mostrarEstado(texto, esError) {
        estado.textContent = texto;
        estado.className = 'subida__estado' + (esError ? ' msg error' : '');
    }

    function mostrarAvisos(lista) {
        avisos.textContent = '';
        lista.forEach(function (texto) {
            const item = document.createElement('li');
            item.textContent = texto;
            avisos.appendChild(item);
        });
        avisos.hidden = lista.length === 0;
    }

    function actualizarBoton() {
        const hayArchivo = entrada.files.length > 0;
        const hayLinea = !selectorLinea || selectorLinea.value !== '';
        boton.disabled = !(hayArchivo && hayLinea);
    }

    function validar(archivo) {
        if (!/\.pdf$/i.test(archivo.name)) {
            return 'Solo se permiten archivos PDF.';
        }
        if (archivo.size > MAX_BYTES) {
            return 'El archivo supera el tamaño máximo de 20 MB.';
        }
        if (archivo.size === 0) {
            return 'El archivo está vacío.';
        }
        return null;
    }

    function enCurso(activo) {
        entrada.disabled = activo;
        boton.disabled = activo;
        if (selectorLinea) {
            selectorLinea.disabled = activo;
        }
    }

    function subir() {
        const archivo = entrada.files[0];
        if (!archivo) {
            return;
        }
        const problema = validar(archivo);
        if (problema) {
            mostrarEstado(problema, true);
            return;
        }

        const datos = new FormData();
        datos.append('archivo', archivo);
        if (idPlanificacion) {
            datos.append('idPlanificacion', idPlanificacion);
        } else {
            datos.append('idLinea', selectorLinea.value);
        }

        const xhr = new XMLHttpRequest();
        xhr.open('POST', urlSubida);
        xhr.setRequestHeader(cabeceraToken, token);
        xhr.setRequestHeader('Accept', 'application/json');

        xhr.upload.addEventListener('progress', function (e) {
            if (e.lengthComputable) {
                const porcentaje = Math.round((e.loaded / e.total) * 100);
                barra.value = porcentaje;
                mostrarEstado('Subiendo… ' + porcentaje + '%', false);
            }
        });
        // Al terminar de enviar, el servidor todavía lee el PDF y extrae su texto.
        xhr.upload.addEventListener('load', function () {
            barra.value = 100;
            mostrarEstado('Procesando el PDF…', false);
        });

        function fallo(texto) {
            barra.hidden = true;
            enCurso(false);
            actualizarBoton();
            mostrarEstado(texto, true);
        }

        xhr.addEventListener('load', function () {
            let cuerpo = null;
            try {
                cuerpo = JSON.parse(xhr.responseText);
            } catch (e) {
                // Respuesta que no es JSON (por ejemplo, la sesión expiró y llegó el login).
            }
            if (xhr.status >= 200 && xhr.status < 300 && cuerpo && cuerpo.idPlanificacion) {
                try {
                    sessionStorage.setItem('pdf-avisos-' + cuerpo.idPlanificacion,
                            JSON.stringify(cuerpo.advertencias || []));
                    sessionStorage.setItem(claveMensaje,
                            cuerpo.reemplazo ? 'PDF reemplazado correctamente.' : 'PDF subido. Tu borrador quedó guardado.');
                } catch (e) {
                    // Sin almacenamiento de sesión solo se pierden los avisos.
                }
                window.location.href = urlEditar + '/' + cuerpo.idPlanificacion + '/editar';
                return;
            }
            if (cuerpo && cuerpo.error) {
                fallo(cuerpo.error);
            } else if (xhr.status === 401 || xhr.status === 403 || xhr.responseURL.indexOf('/login') !== -1) {
                fallo('Tu sesión expiró. Vuelve a iniciar sesión e inténtalo de nuevo.');
            } else {
                fallo('No se pudo subir el PDF. Inténtalo de nuevo.');
            }
        });
        xhr.addEventListener('error', function () {
            fallo('Falló la conexión. Revisa tu internet e inténtalo de nuevo.');
        });
        xhr.addEventListener('abort', function () {
            fallo('La subida se canceló.');
        });

        avisos.hidden = true;
        barra.value = 0;
        barra.hidden = false;
        enCurso(true);
        mostrarEstado('Subiendo… 0%', false);
        xhr.send(datos);
    }

    entrada.addEventListener('change', function () {
        const archivo = entrada.files[0];
        const problema = archivo ? validar(archivo) : null;
        mostrarEstado(problema || '', Boolean(problema));
        if (problema) {
            entrada.value = '';
        }
        actualizarBoton();
    });
    if (selectorLinea) {
        selectorLinea.addEventListener('change', actualizarBoton);
    }
    boton.addEventListener('click', subir);

    // Tras la subida la página se recarga: aquí se muestra lo que dejó el resultado anterior.
    try {
        const mensaje = sessionStorage.getItem(claveMensaje);
        if (mensaje) {
            mostrarEstado(mensaje, false);
            sessionStorage.removeItem(claveMensaje);
        }
        if (idPlanificacion) {
            const guardados = sessionStorage.getItem(claveAvisos);
            if (guardados) {
                mostrarAvisos(JSON.parse(guardados));
            }
        }
    } catch (e) {
        // Sin almacenamiento de sesión no hay mensajes diferidos.
    }
})();
