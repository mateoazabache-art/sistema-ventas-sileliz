/**
 * Control del Layout, menú según el rol y sesión de usuario (plan, tarea 5 de Cesia).
 *
 * Cada página (menos login.html) carga api.js y luego este archivo.
 * Al cargar:
 *   1. Pide GET api/sesion. Sin sesión (401), api.js manda a login.html.
 *   2. Arma el menú dentro de <header><nav><ul> según el rol:
 *      el ADMINISTRADOR ve además Usuarios y Reportes.
 *   3. Convierte el <header> en una navbar de Bootstrap que se colapsa en celular.
 *   4. Muestra el nombre y el rol del usuario y el botón "Cerrar sesión".
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
 
/**
 * Arma los enlaces del menú dentro de <header><nav><ul> según el rol
 * y luego convierte el <header> en una navbar de Bootstrap.
 */
function ArmarMenu(rol) {
    const lista = document.querySelector('header nav ul');
    if (!lista) return;
 
    lista.replaceChildren();        // borra los enlaces escritos a mano
    const esAdmin = rol === 'ADMINISTRADOR';
    const actual = paginaActual();
 
    MENU.filter(opcion => esAdmin || !opcion.soloAdmin)
        .forEach(opcion => {
            const li = document.createElement('li');
            li.className = 'nav-item';
            const a = document.createElement('a');
            a.className = 'nav-link';
            a.href = opcion.href;
            a.textContent = opcion.texto;
            if (opcion.href === actual) {
                a.classList.add('activo', 'active');
                a.setAttribute('aria-current', 'page');
            }
            li.appendChild(a);
            lista.appendChild(li);
        });
 
    ArmarNavbar(lista);
}
 
/**
 * Convierte <header> en una navbar de Bootstrap (colapsable en celular):
 * [logo + marca] [botón hamburguesa] [menú colapsable con la lista de enlaces].
 */
function ArmarNavbar(lista) {
    const header = document.querySelector('header');
    if (!header) return;
 
    header.classList.add('navbar', 'navbar-expand-lg', 'navbar-dark', 'barra-sileliz');
 
    const contenedor = document.createElement('div');
    contenedor.className = 'container-fluid';
 
    // Marca: logo + nombre. Lleva a Inicio.
    const marca = document.createElement('a');
    marca.className = 'navbar-brand d-flex align-items-center gap-2';
    marca.href = 'index.html';
    const logo = document.createElement('img');
    logo.src = 'img/logo-transparente.png';
    logo.alt = 'Sileliz.mas';
    logo.height = 44;
    logo.className = 'logo-barra';
    const nombreMarca = document.createElement('span');
    nombreMarca.textContent = 'Sileliz.mas';
    marca.append(logo, nombreMarca);
 
    // Botón hamburguesa (solo se ve en celular).
    const boton = document.createElement('button');
    boton.className = 'navbar-toggler';
    boton.type = 'button';
    boton.setAttribute('data-bs-toggle', 'collapse');
    boton.setAttribute('data-bs-target', '#menuPrincipal');
    boton.setAttribute('aria-controls', 'menuPrincipal');
    boton.setAttribute('aria-expanded', 'false');
    boton.setAttribute('aria-label', 'Mostrar u ocultar el menú');
    const icono = document.createElement('span');
    icono.className = 'navbar-toggler-icon';
    boton.appendChild(icono);
 
    // Zona que se colapsa: lista de enlaces (y luego el usuario).
    const colapsable = document.createElement('div');
    colapsable.className = 'collapse navbar-collapse';
    colapsable.id = 'menuPrincipal';
    lista.className = 'navbar-nav me-auto mb-2 mb-lg-0';
    colapsable.appendChild(lista);
 
    contenedor.append(marca, boton, colapsable);
    header.replaceChildren(contenedor);   // reemplaza el h1 y el nav originales
}
 
/**
 * Muestra "Nombre (Rol)" y el botón Cerrar sesión dentro del menú.
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
        caja.className = 'usuario-sesion d-flex align-items-lg-center flex-column flex-lg-row gap-2';
 
        elUsuario = document.createElement('span');
        elUsuario.id = 'lblUsuarioNombre';
 
        elRol = document.createElement('span');
        elRol.id = 'lblUsuarioRol';
 
        const etiqueta = document.createElement('span');
        etiqueta.className = 'usuario-etiqueta';
        etiqueta.append(elUsuario, ' (', elRol, ')');
 
        btnLogout = document.createElement('button');
        btnLogout.type = 'button';
        btnLogout.id = 'btnLogout';
        btnLogout.className = 'btn btn-outline-light btn-sm';
        btnLogout.textContent = 'Cerrar sesión';
 
        caja.append(etiqueta, btnLogout);
        // Dentro del menú colapsable si existe; si no, al final del header.
        (document.getElementById('menuPrincipal') || header).appendChild(caja);
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
