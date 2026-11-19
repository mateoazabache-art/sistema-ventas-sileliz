/**
 * Página de inicio de sesión (tareas 6 y 7 de Cesia).
 *
 * POST api/login  {"usuario": "...", "clave": "..."}
 *   200 -> {id, nombre, usuario, rol, rolEtiqueta}  -> ir a index.html
 *   401 -> {"error": "Usuario o clave incorrectos."}
 *   423 -> {"error": "Cuenta bloqueada por 15 minutos."}  (5 intentos fallidos, RN-11)
 * api.js NO redirige con el 401 del login: lo lanza como error y aquí se muestra.
 * Todo con textContent (nunca innerHTML).
 */
document.addEventListener('DOMContentLoaded', () => {
    const formulario = document.getElementById('form-login');
    const campoUsuario = document.getElementById('usuario');
    const campoClave = document.getElementById('clave');
    const btnVerClave = document.getElementById('btn-ver-clave');
    const btnIngresar = document.getElementById('btn-ingresar');
    const mensaje = document.getElementById('mensaje-login');

    // Mostrar u ocultar la contraseña
    btnVerClave.addEventListener('click', () => {
        const oculta = campoClave.type === 'password';
        campoClave.type = oculta ? 'text' : 'password';
        btnVerClave.textContent = oculta ? 'Ocultar' : 'Mostrar';
        btnVerClave.setAttribute('aria-pressed', String(oculta));
        campoClave.focus();
    });

    // Al escribir, se limpia el error de ese campo
    campoUsuario.addEventListener('input', () => marcarError(campoUsuario, 'error-usuario', ''));
    campoClave.addEventListener('input', () => marcarError(campoClave, 'error-clave', ''));

    formulario.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        mensaje.textContent = '';

        const usuario = campoUsuario.value.trim();
        const clave = campoClave.value;

        // Validación en el navegador (el servidor vuelve a validar)
        let valido = true;
        if (usuario === '') {
            marcarError(campoUsuario, 'error-usuario', 'Ingrese su usuario.');
            valido = false;
        }
        if (clave === '') {
            marcarError(campoClave, 'error-clave', 'Ingrese su contraseña.');
            valido = false;
        }
        if (!valido) {
            (usuario === '' ? campoUsuario : campoClave).focus();
            return;
        }

        // Enviar
        btnIngresar.disabled = true;
        btnIngresar.textContent = 'Ingresando...';
        try {
            await API.auth.login({ usuario, clave });
            window.location.href = `${API.baseUrl}index.html`;
        } catch (error) {
            mensaje.textContent = error.message || 'No se pudo iniciar sesión. Intente de nuevo.';
            campoClave.value = '';
            campoClave.focus();
            btnIngresar.disabled = false;
            btnIngresar.textContent = 'Ingresar';
        }
    });

    /** Pone (o quita, si texto está vacío) el mensaje de error debajo de un campo. */
    function marcarError(campo, idError, texto) {
        document.getElementById(idError).textContent = texto;
        campo.classList.toggle('campo-invalido', texto !== '');
        campo.setAttribute('aria-invalid', String(texto !== ''));
    }
});
