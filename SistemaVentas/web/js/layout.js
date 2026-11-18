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
 *
 * Modo oscuro (15 nov; ícono de luna 18 nov): el botón de la luna pone data-bs-theme="dark" en <html>
 * (Bootstrap 5.3 lo trae listo) y la elección se recuerda en este navegador.
 */

// Tema guardado: se aplica apenas carga el archivo, antes de armar el menú.
const CLAVE_TEMA = 'sileliz-tema';
aplicarTema(leerTemaGuardado());

function leerTemaGuardado() {
    try {
        return localStorage.getItem(CLAVE_TEMA) === 'oscuro' ? 'oscuro' : 'claro';
    } catch (e) {
        return 'claro';             // sin almacenamiento: tema claro
    }
}

function aplicarTema(tema) {
    document.documentElement.setAttribute('data-bs-theme', tema === 'oscuro' ? 'dark' : 'light');
}
 
// Opciones del menú. soloAdmin = solo las ve el ADMINISTRADOR.
const MENU = [
    { texto: 'Inicio',            href: 'index.html' },
    { texto: 'Nueva venta',       href: 'venta-nueva.html' },
    { texto: 'Ventas realizadas', href: 'ventas.html' },
    { texto: 'Productos',         href: 'productos.html' },
    { texto: 'Categorías',        href: 'categorias.html', soloAdmin: true },   // plan 2.1: el vendedor no gestiona categorías
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
 
    header.classList.add('navbar', 'navbar-expand-xl', 'navbar-dark', 'barra-sileliz');   // xl: el menú completo no cabe en menos de 1200 px
 
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
    lista.className = 'navbar-nav me-auto mb-2 mb-xl-0';
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
        caja.className = 'usuario-sesion d-flex align-items-xl-center flex-column flex-xl-row gap-2';
 
        elUsuario = document.createElement('span');
        elUsuario.id = 'lblUsuarioNombre';
 
        elRol = document.createElement('span');
        elRol.id = 'lblUsuarioRol';
 
        elRol.className = 'rol-etiqueta';

        const etiqueta = document.createElement('span');
        etiqueta.className = 'usuario-etiqueta';
        etiqueta.append(elUsuario, elRol);

        const btnTema = document.createElement('button');
        btnTema.type = 'button';
        btnTema.id = 'btnTema';
        btnTema.className = 'btn btn-sm boton-tema';
        btnTema.addEventListener('click', CambiarTema);
 
        btnLogout = document.createElement('button');
        btnLogout.type = 'button';
        btnLogout.id = 'btnLogout';
        btnLogout.className = 'btn btn-outline-light btn-sm';
        btnLogout.textContent = 'Cerrar sesión';
 
        caja.append(etiqueta, btnTema, btnLogout);
        // Dentro del menú colapsable si existe; si no, al final del header.
        (document.getElementById('menuPrincipal') || header).appendChild(caja);
    }
 
    // Solo el primer nombre ("Rafael"), para que la barra no se parta en dos líneas
    elUsuario.textContent = (sesion.nombre || '').trim().split(/\s+/)[0];
    elUsuario.title = sesion.nombre;
    if (elRol) elRol.textContent = sesion.rolEtiqueta || sesion.rol || 'Usuario';
    PintarBotonTema();
}

/** Cambia entre claro y oscuro y lo recuerda en este navegador. */
function CambiarTema() {
    const nuevo = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'claro' : 'oscuro';
    aplicarTema(nuevo);
    try {
        localStorage.setItem(CLAVE_TEMA, nuevo);
    } catch (e) {
        // sin almacenamiento: el tema dura hasta recargar la página
    }
    PintarBotonTema();
}

/*
 * Íconos de luna de Phosphor Icons (MIT, phosphoricons.com), copiados aquí como trazos SVG
 * para que funcionen sin internet. Contorno = modo claro; rellena = modo oscuro activo.
 */
const ICONO_LUNA = 'M233.54,142.23a8,8,0,0,0-8-2,88.08,88.08,0,0,1-109.8-109.8,8,8,0,0,0-10-10,104.84,104.84,0,0,0-52.91,37A104,104,0,0,0,136,224a103.09,103.09,0,0,0,62.52-20.88,104.84,104.84,0,0,0,37-52.91A8,8,0,0,0,233.54,142.23ZM188.9,190.34A88,88,0,0,1,65.66,67.11a89,89,0,0,1,31.4-26A106,106,0,0,0,96,56,104.11,104.11,0,0,0,200,160a106,106,0,0,0,14.92-1.06A89,89,0,0,1,188.9,190.34Z';
const ICONO_LUNA_RELLENA = 'M235.54,150.21a104.84,104.84,0,0,1-37,52.91A104,104,0,0,1,32,120,103.09,103.09,0,0,1,52.88,57.48a104.84,104.84,0,0,1,52.91-37,8,8,0,0,1,10,10,88.08,88.08,0,0,0,109.8,109.8,8,8,0,0,1,10,10Z';

/** Arma el <svg> con createElementNS (sin innerHTML, como en todo el proyecto). */
function crearIcono(trazo) {
    const NS = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('viewBox', '0 0 256 256');
    svg.setAttribute('width', '20');
    svg.setAttribute('height', '20');
    svg.setAttribute('fill', 'currentColor');
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('focusable', 'false');
    const path = document.createElementNS(NS, 'path');
    path.setAttribute('d', trazo);
    svg.appendChild(path);
    return svg;
}

/** Luna de contorno en modo claro; luna rellena (fucsia) cuando el modo oscuro está activo. */
function PintarBotonTema() {
    const btn = document.getElementById('btnTema');
    if (!btn) return;
    const oscuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
    btn.replaceChildren(crearIcono(oscuro ? ICONO_LUNA_RELLENA : ICONO_LUNA));
    btn.classList.toggle('activo', oscuro);
    btn.setAttribute('aria-pressed', oscuro ? 'true' : 'false');
    const texto = oscuro ? 'Modo oscuro activado. Cambiar a modo claro' : 'Cambiar a modo oscuro';
    btn.title = texto;
    btn.setAttribute('aria-label', texto);
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
