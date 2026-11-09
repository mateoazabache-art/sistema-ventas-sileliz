/**
 * Control del Layout, Menú Lateral y Sesión de Usuario
 */
document.addEventListener('DOMContentLoaded', async () => {
    console.log("Plantilla e interfaz inicializadas correctamente.");
    await CargarSesionUsuario();
    ConfigurarBotonLogout();
});

/**
 * Obtiene los datos del usuario actual y actualiza la interfaz
 */
async function CargarSesionUsuario() {
    try {
        if (!window.API || !window.API.auth) return;

        const sesion = await window.API.auth.obtenerSesion();
        if (sesion && sesion.nombre) {
            const elUsuario = document.getElementById('lblUsuarioNombre');
            const elRol = document.getElementById('lblUsuarioRol');

            if (elUsuario) elUsuario.textContent = sesion.nombre;
            if (elRol) elRol.textContent = sesion.rol || 'Usuario';
        }
    } catch (error) {
        console.warn("No se pudo cargar la información de la sesión activa:", error);
    }
}

/**
 * Vincula el evento click al botón de cerrar sesión
 */
function ConfigurarBotonLogout() {
    const btnLogout = document.getElementById('btnLogout');
    if (!btnLogout) return;

    btnLogout.addEventListener('click', async (e) => {
        e.preventDefault();
        try {
            await window.API.auth.logout();
            window.location.href = `${window.API.baseUrl}login.jsp`;
        } catch (error) {
            console.error("Error al cerrar sesión:", error);
            window.location.href = `${window.API.baseUrl}login.jsp`;
        }
    });
}