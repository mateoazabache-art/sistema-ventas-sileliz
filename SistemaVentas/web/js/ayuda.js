/**
 * Control de contenido de la página de Ayuda según el rol del usuario.
 */

document.addEventListener("DOMContentLoaded", async () => {
    const elementosAdmin = document.querySelectorAll("[data-solo-admin]");

    try {
        const sesion = await API.auth.obtenerSesion();
        if (!sesion) return;

        const esAdministrador = sesion.rol === "ADMINISTRADOR";

        elementosAdmin.forEach(elemento => {
            elemento.classList.toggle("d-none", !esAdministrador);
        });
    } catch (error) {
        console.warn("No se pudo verificar el rol para mostrar la ayuda:", error);
    }
});