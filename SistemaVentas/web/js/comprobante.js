/**
 * comprobante.html (tarea 12 de Cesia). Se abre como comprobante.html?id=5
 * Pide API.get('api/ventas/5') y dibuja el comprobante interno con textContent.
 * Todo va dentro de una función para no chocar con los nombres globales de layout.js.
 * Los totales se muestran tal cual vienen del servidor (nunca se recalculan aquí).
 */
(function () {
    // Datos de la tienda. PENDIENTE: dirección y teléfono (pedirlos a Rafael, el dueño).
    const TIENDA = {
        nombre: 'Sileliz.mas - Moda en tallas grandes',
        direccion: 'Dirección: por confirmar',
        telefono: 'Teléfono: por confirmar'
    };

    const METODOS_PAGO = {
        EFECTIVO: 'Efectivo', YAPE: 'Yape', PLIN: 'Plin',
        TARJETA: 'Tarjeta', TRANSFERENCIA: 'Transferencia'
    };

    const porId = (id) => document.getElementById(id);

    document.addEventListener('DOMContentLoaded', cargarComprobante);

    async function cargarComprobante() {
        const id = new URLSearchParams(window.location.search).get('id');
        if (!/^\d+$/.test(id || '')) {
            mostrarMensaje('No se indicó la venta.');
            return;
        }
        try {
            const datos = await API.get('api/ventas/' + id);
            if (!datos) return;             // sin sesión: api.js ya mandó a login.html
            pintar(datos);
        } catch (error) {
            mostrarMensaje(error.message);  // 404, 403 o 400: el mensaje viene del servidor
        }
    }

    function pintar(c) {
        const v = c.venta;

        porId('tienda-nombre').textContent = TIENDA.nombre;
        porId('tienda-direccion').textContent = TIENDA.direccion;
        porId('tienda-telefono').textContent = TIENDA.telefono;

        porId('numero').textContent = v.serie + '-' + String(v.numero).padStart(6, '0');
        porId('fecha').textContent = fechaHora(v.fechaHora);
        porId('cliente').textContent = c.cliente;
        porId('dni').textContent = c.dniCliente ?? '—';
        porId('vendedor').textContent = c.vendedor;
        porId('pago').textContent = METODOS_PAGO[v.metodoPago] || v.metodoPago;

        // Venta anulada: sello rojo y motivo (solo viene si está anulada)
        if (v.estado === 'ANULADA') {
            porId('bloque-anulada').hidden = false;
            porId('motivo').textContent = v.motivoAnulacion ? 'Motivo: ' + v.motivoAnulacion : '';
        }

        const filas = (v.detalles || []).map(d => {
            const tr = document.createElement('tr');
            [
                d.descripcion,
                { texto: d.cantidad },
                { texto: soles(d.precioUnitario) },
                { texto: soles(importe(d)) }
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
        porId('detalle').replaceChildren(...filas);

        // La fila de descuento solo se muestra si hubo descuento
        const hayDescuento = Number(v.descuento) > 0;
        porId('fila-descuento-t').hidden = !hayDescuento;
        porId('descuento').hidden = !hayDescuento;
        porId('descuento').textContent = '- ' + soles(v.descuento);

        porId('base').textContent = soles(v.baseImponible);
        porId('igv').textContent = soles(v.igv);
        porId('total').textContent = soles(v.total);

        porId('comprobante').hidden = false;
        porId('btn-imprimir').hidden = false;
        porId('btn-imprimir').addEventListener('click', () => window.print());
    }

    function mostrarMensaje(texto) {
        porId('mensaje').textContent = texto;
        porId('mensaje').hidden = false;
    }

    /** Importe de una línea = cantidad x precio (solo para mostrar, en céntimos). */
    function importe(d) {
        return Math.round(Number(d.precioUnitario) * 100) * Number(d.cantidad) / 100;
    }

    /** 209.7 -> "S/ 209.70" */
    function soles(valor) {
        return 'S/ ' + Number(valor || 0).toFixed(2);
    }

    /** "2026-11-05T16:30" (con o sin segundos) -> "05/11/2026 16:30" */
    function fechaHora(iso) {
        const [a, m, d] = String(iso).substring(0, 10).split('-');
        return d + '/' + m + '/' + a + ' ' + String(iso).substring(11, 16);
    }
})();
