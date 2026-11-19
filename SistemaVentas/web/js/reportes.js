/**
 * reportes.html (tarea 13 de Rafael). Solo ADMINISTRADOR.
 *
 * Consulta tres direcciones con el mismo rango de fechas:
 *   api/reportes/ventas        lista paginada + total acumulado
 *   api/reportes/mas-vendidos  [{etiqueta, cantidad, monto}]
 *   api/reportes/por-vendedor  [{etiqueta, cantidad, monto}]
 *
 * Reglas del proyecto: todas las llamadas pasan por API (api.js) y los datos
 * se escriben con textContent (nunca innerHTML). Los importes los calcula el
 * servidor; aqui solo se les da formato.
 */

const TAM_PAGINA = 10;

// Rango que se esta mostrando y pagina actual de la lista de ventas
const estado = { desde: '', hasta: '', pagina: 1, paginas: 1 };

const el = {
    form: document.getElementById('form-filtro'),
    desde: document.getElementById('desde'),
    hasta: document.getElementById('hasta'),
    consultar: document.getElementById('btn-consultar'),
    mes: document.getElementById('btn-mes'),
    hoy: document.getElementById('btn-hoy'),
    mensaje: document.getElementById('mensaje'),
    totalVentas: document.getElementById('total-ventas'),
    totalMonto: document.getElementById('total-monto'),
    totalRango: document.getElementById('total-rango'),
    ventas: document.getElementById('tabla-ventas'),
    masVendidos: document.getElementById('tabla-mas-vendidos'),
    porVendedor: document.getElementById('tabla-por-vendedor'),
    anterior: document.getElementById('btn-anterior'),
    siguiente: document.getElementById('btn-siguiente'),
    infoPagina: document.getElementById('info-pagina')
};

document.addEventListener('DOMContentLoaded', () => {
    ponerEsteMes();
    consultar();

    el.form.addEventListener('submit', (e) => {
        e.preventDefault();
        consultar();
    });
    el.mes.addEventListener('click', () => { ponerEsteMes(); consultar(); });
    el.hoy.addEventListener('click', () => {
        el.desde.value = hoyIso();
        el.hasta.value = hoyIso();
        consultar();
    });
    el.anterior.addEventListener('click', () => cargarVentas(estado.pagina - 1));
    el.siguiente.addEventListener('click', () => cargarVentas(estado.pagina + 1));
});

// ---------------------------------------------------------------
// Consultas
// ---------------------------------------------------------------

/** Valida el rango en la página y pide los tres reportes. */
async function consultar() {
    ocultarMensaje();
    const desde = el.desde.value;
    const hasta = el.hasta.value;

    if (!desde || !hasta) {
        mostrarMensaje('Elija la fecha "desde" y la fecha "hasta".');
        return;
    }
    if (desde > hasta) {            // AAAA-MM-DD se compara bien como texto
        mostrarMensaje('La fecha "desde" no puede ser posterior a la fecha "hasta".');
        return;
    }

    estado.desde = desde;
    estado.hasta = hasta;
    el.consultar.disabled = true;
    try {
        const rango = `desde=${desde}&hasta=${hasta}`;
        // Las tres consultas salen a la vez y se espera a que lleguen todas
        const [ventas, masVendidos, porVendedor] = await Promise.all([
            pedirVentas(1),
            API.get(`api/reportes/mas-vendidos?${rango}&limite=10`),
            API.get(`api/reportes/por-vendedor?${rango}`)
        ]);
        if (!ventas) return;        // sin sesión: api.js ya mandó al login

        pintarVentas(ventas);
        pintarFilas(el.masVendidos, masVendidos, 'No hay productos vendidos en este rango.');
        pintarFilas(el.porVendedor, porVendedor, 'No hay ventas en este rango.');
    } catch (error) {
        mostrarMensaje(error.message);      // el mensaje viene del servidor
    } finally {
        el.consultar.disabled = false;
    }
}

/** Cambia de página en la lista de ventas (el total y las otras tablas no cambian). */
async function cargarVentas(pagina) {
    if (pagina < 1 || pagina > estado.paginas) return;
    ocultarMensaje();
    try {
        const ventas = await pedirVentas(pagina);
        if (ventas) pintarVentas(ventas);
    } catch (error) {
        mostrarMensaje(error.message);
    }
}

function pedirVentas(pagina) {
    return API.get(`api/reportes/ventas?desde=${estado.desde}&hasta=${estado.hasta}`
        + `&pagina=${pagina}&tam=${TAM_PAGINA}`);
}

// ---------------------------------------------------------------
// Dibujar
// ---------------------------------------------------------------

function pintarVentas(r) {
    el.totalVentas.textContent = r.ventas;
    el.totalMonto.textContent = soles(r.total);
    el.totalRango.textContent = `${fechaCorta(r.desde)} al ${fechaCorta(r.hasta)}`;

    const filas = r.pagina.items.map(v => {
        const tr = fila([
            v.comprobante,
            fechaHora(v.fechaHora),
            v.cliente,
            v.vendedor,
            v.metodoPago,
            { texto: soles(v.descuento), num: true },
            { texto: soles(v.total), num: true },
            v.estado === 'ANULADA' ? 'Anulada' : 'Emitida'
        ]);
        if (v.estado === 'ANULADA') tr.classList.add('fila-anulada');
        return tr;
    });
    reemplazar(el.ventas, filas, 8, 'No hay ventas en este rango.');

    estado.pagina = r.pagina.pagina;
    estado.paginas = Math.max(r.pagina.paginas, 1);
    el.infoPagina.textContent = `Página ${estado.pagina} de ${estado.paginas} (${r.pagina.total} ventas)`;
    el.anterior.disabled = estado.pagina <= 1;
    el.siguiente.disabled = estado.pagina >= estado.paginas;
}

/** Tablas de 3 columnas: etiqueta, cantidad, monto. */
function pintarFilas(tbody, lista, textoVacio) {
    const filas = (lista || []).map(f => fila([
        f.etiqueta,
        { texto: f.cantidad, num: true },
        { texto: soles(f.monto), num: true }
    ]));
    reemplazar(tbody, filas, 3, textoVacio);
}

/** Crea un <tr>. Cada celda es un texto o {texto, num:true} para alinear a la derecha. */
function fila(celdas) {
    const tr = document.createElement('tr');
    celdas.forEach(c => {
        const td = document.createElement('td');
        if (c !== null && typeof c === 'object') {
            td.textContent = c.texto;
            if (c.num) td.classList.add('num');
        } else {
            td.textContent = c;
        }
        tr.appendChild(td);
    });
    return tr;
}

/** Pone las filas en el tbody; si no hay, una fila que lo dice. */
function reemplazar(tbody, filas, columnas, textoVacio) {
    if (filas.length === 0) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = columnas;
        td.className = 'vacio';
        td.textContent = textoVacio;
        tr.appendChild(td);
        filas = [tr];
    }
    tbody.replaceChildren(...filas);
}

// ---------------------------------------------------------------
// Ayudantes
// ---------------------------------------------------------------

function mostrarMensaje(texto) {
    el.mensaje.textContent = texto;
    el.mensaje.hidden = false;
}

function ocultarMensaje() {
    el.mensaje.hidden = true;
    el.mensaje.textContent = '';
}

/** Fecha de hoy de la PC como AAAA-MM-DD (hora local, no UTC). */
function hoyIso() {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
}

function ponerEsteMes() {
    const hoy = hoyIso();
    el.desde.value = hoy.substring(0, 8) + '01';
    el.hasta.value = hoy;
}

/** 209.7 -> "S/ 209.70" */
function soles(valor) {
    return 'S/ ' + Number(valor || 0).toFixed(2);
}

/** "2026-11-07" -> "07/11/2026" */
function fechaCorta(iso) {
    const [a, m, d] = String(iso).substring(0, 10).split('-');
    return `${d}/${m}/${a}`;
}

/** "2026-11-07T15:42:10" -> "07/11/2026 15:42" */
function fechaHora(iso) {
    return `${fechaCorta(iso)} ${String(iso).substring(11, 16)}`;
}
