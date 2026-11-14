/**
 * index.html (Inicio) - tarea 10 de Cesia.
 * Pide API.estadisticas.hoy() -> GET api/estadisticas/hoy y escribe las tarjetas.
 * Formato: {"fecha":"2026-11-10","ventas":3,"total":330.00,"anuladas":1,"clientes":10,"stockBajo":13}
 * Todo con textContent (nunca innerHTML). Los importes los calcula el servidor:
 * aquí solo se les da formato.
 */

const el = {
    fecha: document.getElementById('fecha-resumen'),
    mensaje: document.getElementById('mensaje'),
    ventas: document.getElementById('valor-ventas'),
    anuladas: document.getElementById('nota-anuladas'),
    total: document.getElementById('valor-total'),
    clientes: document.getElementById('valor-clientes'),
    stock: document.getElementById('valor-stock'),
    enlaceStock: document.getElementById('enlace-stock')
};

document.addEventListener('DOMContentLoaded', cargarResumen);

async function cargarResumen() {
    try {
        const datos = await API.estadisticas.hoy();
        if (!datos) return;             // sin sesión: api.js ya mandó a login.html
        pintar(datos);
    } catch (error) {
        mostrarMensaje(error.message);  // el mensaje viene del servidor
    }
}

function pintar(d) {
    el.fecha.textContent = 'Resumen del día ' + fechaCorta(d.fecha);
    el.ventas.textContent = d.ventas;
    el.anuladas.textContent = d.anuladas > 0 ? 'Anuladas: ' + d.anuladas : '';
    el.total.textContent = 'S/ ' + Number(d.total || 0).toFixed(2);
    el.clientes.textContent = d.clientes;
    el.stock.textContent = d.stockBajo;

    // Stock bajo en rojo (RN-10) y con enlace a Productos si hay algo que atender
    const hayBajo = d.stockBajo > 0;
    el.stock.classList.toggle('stock-bajo', hayBajo);
    el.enlaceStock.hidden = !hayBajo;
}

function mostrarMensaje(texto) {
    el.mensaje.textContent = texto;
    el.mensaje.hidden = false;
}

/** "2026-11-10" -> "10/11/2026" */
function fechaCorta(iso) {
    const [a, m, d] = String(iso).substring(0, 10).split('-');
    return d + '/' + m + '/' + a;
}