/**
 * comprobante.html (tarea 12 de Cesia). Se abre como comprobante.html?id=15
 * Pide API.get('api/ventas/15') y dibuja el comprobante interno con textContent.
 * Respuesta (Luz, PR #30):
 *   { venta: {idVenta, serie, numero, fechaHora, metodoPago, baseImponible, descuento,
 *             igv, total, estado, detalles:[{descripcion, cantidad, precioUnitario}]},
 *     cliente: "María ..." | "Varios", dniCliente (no viene si es "Varios"), vendedor }
 * Para probar sin datos reales: comprobante.html?mock=1 (usa mock/venta.json).
 */

// Datos de la tienda (RF-16). PENDIENTE: confirmar con el dueño y reemplazar los textos.
const TIENDA = {
    nombre: 'Sileliz.mas - Más que una talla',
    ruc: 'RUC: por confirmar',
    direccion: 'Dirección: por confirmar',
    telefono: 'Teléfono: por confirmar'
};

const METODOS_PAGO = {
    EFECTIVO: 'Efectivo', YAPE: 'Yape', PLIN: 'Plin',
    TARJETA: 'Tarjeta', TRANSFERENCIA: 'Transferencia'
};

const $ = (id) => document.getElementById(id);

document.addEventListener('DOMContentLoaded', cargarComprobante);

async function cargarComprobante() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');
    const usarMock = params.get('mock') === '1';

    if (!usarMock && !/^\d+$/.test(id || '')) {
        mostrarMensaje('Falta el número de la venta. Ábrala desde Ventas realizadas.');
        return;
    }

    try {
        const c = await API.get(usarMock ? 'mock/venta.json' : 'api/ventas/' + id);
        if (!c) return;                 // sin sesión: api.js ya mandó a login.html
        pintar(c);
    } catch (error) {
        mostrarMensaje(error.message);  // 404 o 403: el mensaje viene del servidor
    }
}

function pintar(c) {
    const v = c.venta;

    $('tienda-nombre').textContent = TIENDA.nombre;
    $('tienda-ruc').textContent = TIENDA.ruc;
    $('tienda-direccion').textContent = TIENDA.direccion;
    $('tienda-telefono').textContent = TIENDA.telefono;

    $('numero').textContent = v.serie + '-' + String(v.numero).padStart(6, '0');
    $('aviso-anulada').hidden = v.estado !== 'ANULADA';
    $('fecha').textContent = fechaHora(v.fechaHora);
    $('cliente').textContent = c.cliente;
    $('dni').textContent = c.dniCliente ?? '—';
    $('vendedor').textContent = c.vendedor;
    $('pago').textContent = METODOS_PAGO[v.metodoPago] || v.metodoPago;

    const filas = (v.detalles || []).map(d => {
        const tr = document.createElement('tr');
        [
            d.descripcion,
            { texto: d.cantidad, num: true },
            { texto: soles(d.precioUnitario), num: true },
            { texto: soles(subtotal(d)), num: true }
        ].forEach(celda => {
            const td = document.createElement('td');
            if (typeof celda === 'object') {
                td.textContent = celda.texto;
                td.classList.add('num');
            } else {
                td.textContent = celda;
            }
            tr.appendChild(td);
        });
        return tr;
    });
    $('detalle').replaceChildren(...filas);

    $('base').textContent = soles(v.baseImponible);
    $('descuento').textContent = soles(v.descuento);
    $('igv').textContent = soles(v.igv);
    $('total').textContent = soles(v.total);

    $('comprobante').hidden = false;
    $('btn-imprimir').hidden = false;
    $('btn-imprimir').addEventListener('click', () => window.print());
}

function mostrarMensaje(texto) {
    $('mensaje').textContent = texto;
    $('mensaje').hidden = false;
}

/** Subtotal de una línea = precio x cantidad (solo para mostrar, en centavos). */
function subtotal(d) {
    return Math.round(Number(d.precioUnitario) * 100) * Number(d.cantidad) / 100;
}

/** 209.7 -> "S/ 209.70" */
function soles(valor) {
    return 'S/ ' + Number(valor || 0).toFixed(2);
}

/** "2026-11-07T15:42:10" -> "07/11/2026 15:42" */
function fechaHora(iso) {
    const [a, m, d] = String(iso).substring(0, 10).split('-');
    return d + '/' + m + '/' + a + ' ' + String(iso).substring(11, 16);
}
