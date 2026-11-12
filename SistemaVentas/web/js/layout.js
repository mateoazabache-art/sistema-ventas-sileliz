/**
 * Control del Layout, menú según el rol y sesión de usuario (plan, tarea 5 de Cesia).
 *
 * Cada página (menos login.html) carga api.js y luego este archivo.
 * Al cargar:
 *   1. Pide GET api/sesion. Sin sesión (401), api.js manda a login.html.
 *   2. Arma el menú dentro de <header><nav><ul> según el rol:
 *      el ADMINISTRADOR ve además Usuarios y Reportes.
 *   3. Muestra el nombre y el rol del usuario y el botón "Cerrar sesión".
 * Todo con createElement y textContent (nunca innerHTML con datos).
 */

// Opciones del menú. soloAdmin = solo las ve el ADMINISTRADOR.
const MENU = [
    { texto: 'Inicio',            href: 'index.html' },
    { texto: 'Nueva venta',       href: 'venta-nueva.html' },
    { texto: 'Ventas realizadas', href: 'ventas.html' },
    { texto: 'Productos',         href: 'productos.html' },
    { texto: 'Categorías',        href: 'categorias.html' },
    { texto: 'Clientes',          href: 'clientes.html' },
    { texto: 'Usuarios',          href: 'usuarios.html', soloAdmin: true },
    { texto: 'Reportes',          href: 'reportes.html', soloAdmin: true },
    { texto: 'Ayuda',             href: 'ayuda.html' }
];

document.addEventListener('DOMContentLoaded', async () => {
    // La página de login no tiene sesión todavía: no se arma el menú ahí.
    if (paginaActual() === 'login.html') return;

    const sesion = await CargarSesionUsuario();
    if (!sesion) return;            // sin sesión: api.js ya redirigió a login.html

    ArmarMenu(sesion.rol);
    MostrarUsuario(sesion);
    ConfigurarBotonLogout();
});

/** Nombre del archivo de la página actual, por ejemplo "clientes.html". */
function paginaActual() {
    const nombre = window.location.pathname.split('/').pop();
    return nombre === '' ? 'index.html' : nombre;
}

/**
 * Obtiene los datos del usuario actual:
 * {id, nombre, usuario, rol, rolEtiqueta} o null si no hay sesión.
 */
async function CargarSesionUsuario() {
    try {
        if (!window.API || !window.API.auth) return null;
        return await window.API.auth.obtenerSesion();
    } catch (error) {
        console.warn("No se pudo cargar la información de la sesión activa:", error);
        return null;
    }
}

/** Arma los enlaces del menú dentro de <header><nav><ul> según el rol. */
function ArmarMenu(rol) {
    const lista = document.querySelector('header nav ul');
    if (!lista) return;

    lista.replaceChildren();        // borra los enlaces escritos a mano
    const esAdmin = rol === 'ADMINISTRADOR';
    const actual = paginaActual();

    MENU.filter(opcion => esAdmin || !opcion.soloAdmin)
        .forEach(opcion => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = opcion.href;
            a.textContent = opcion.texto;
            if (opcion.href === actual) {
                a.classList.add('activo');
                a.setAttribute('aria-current', 'page');
            }
            li.appendChild(a);
            lista.appendChild(li);
        });
}

/**
 * Muestra "Nombre (Rol)" y el botón Cerrar sesión al final del <header>.
 * Si la página ya tiene #lblUsuarioNombre, #lblUsuarioRol y #btnLogout, usa esos.
 */
function MostrarUsuario(sesion) {
    const header = document.querySelector('header');
    if (!header) return;

    let elUsuario = document.getElementById('lblUsuarioNombre');
    let elRol = document.getElementById('lblUsuarioRol');
    let btnLogout = document.getElementById('btnLogout');

    if (!elUsuario || !btnLogout) {
        const caja = document.createElement('div');
        caja.className = 'usuario-sesion';

        elUsuario = document.createElement('span');
        elUsuario.id = 'lblUsuarioNombre';

        elRol = document.createElement('span');
        elRol.id = 'lblUsuarioRol';

        btnLogout = document.createElement('button');
        btnLogout.type = 'button';
        btnLogout.id = 'btnLogout';
        btnLogout.textContent = 'Cerrar sesión';

        caja.append(elUsuario, ' (', elRol, ') ', btnLogout);
        header.appendChild(caja);
    }

    elUsuario.textContent = sesion.nombre;
    if (elRol) elRol.textContent = sesion.rolEtiqueta || sesion.rol || 'Usuario';
}

/** Cerrar sesión: POST api/logout y volver a login.html (aunque falle la red). */
function ConfigurarBotonLogout() {
    const btnLogout = document.getElementById('btnLogout');
    if (!btnLogout) return;

    btnLogout.addEventListener('click', async (e) => {
        e.preventDefault();
        try {
            await window.API.auth.logout();
        } catch (error) {
            console.error("Error al cerrar sesión:", error);
        }
        window.location.href = `${window.API.baseUrl}login.html`;
    });
}