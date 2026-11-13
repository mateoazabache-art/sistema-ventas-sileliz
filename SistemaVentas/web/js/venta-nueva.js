/**
 * venta-nueva.html (Luz, tareas 10 y 11 del plan): cliente por DNI, buscar prenda,
 * carrito, descuento, método de pago y guardar.
 *
 * Reglas del proyecto:
 *  - Todas las llamadas al servidor pasan por API (api.js).
 *  - Los datos se escriben con textContent, nunca con innerHTML.
 *  - El navegador envía SOLO ids y cantidades (contrato 3.7). Los totales de aquí son
 *    una vista previa: el servidor los vuelve a calcular y esos son los que valen.
 *  - Las validaciones de aquí ayudan al usuario; el servidor vuelve a validar todo
 *    (stock con UPDATE ... WHERE stock >= ?, descuento, cantidades).
 */

const MINIMO_POR_CANTIDAD = 3;   // igual que DescuentoFactory.MINIMO_POR_CANTIDAD
const PORCENTAJE_POR_CANTIDAD = 10;

// Lo que la página recuerda mientras está abierta
let cliente = null;              // null = consumidor final (RN-06)
let carrito = [];                // [{ idVariante, descripcion, precio, stock, cantidad }]
let ultimaBusqueda = "";         // para refrescar el stock después de vender
let guardando = false;           // evita el doble clic en "Registrar venta"

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("formCliente").addEventListener("submit", (e) => {
        e.preventDefault();
        buscarCliente();
    });
    document.getElementById("btnConsumidorFinal").addEventListener("click", quitarCliente);
    document.getElementById("formPrenda").addEventListener("submit", (e) => {
        e.preventDefault();
        buscarPrendas(document.getElementById("textoPrenda").value);
    });
    document.getElementById("tipoDescuento").addEventListener("change", () => {
        const esPorcentaje = valor("tipoDescuento") === "PORCENTAJE";
        document.getElementById("grupoPorcentaje").classList.toggle("d-none", !esPorcentaje);
        dibujarTotales();
    });
    document.getElementById("valorDescuento").addEventListener("input", dibujarTotales);
    document.getElementById("formVenta").addEventListener("submit", (e) => {
        e.preventDefault();
        registrarVenta();
    });
    document.getElementById("textoPrenda").focus();
});

// ======================= 1. Cliente =======================

async function buscarCliente() {
    const dni = valor("dni").trim();
    const error = document.getElementById("errorCliente");
    error.textContent = "";
    if (!/^\d{8}$/.test(dni)) {
        error.textContent = "El DNI debe tener exactamente 8 dígitos.";
        return;
    }
    try {
        const c = await API.get("api/clientes/dni/" + encodeURIComponent(dni));
        if (!c) return;                               // sin sesión: api.js ya mandó al login
        cliente = c;
        document.getElementById("clienteElegido").textContent =
            `${c.nombres} ${c.apellidos} (DNI ${c.dni})`;
    } catch (e) {
        quitarCliente();
        error.textContent = e.message + " Puede registrarlo en Clientes o vender como consumidor final.";
    }
}

function quitarCliente() {
    cliente = null;
    document.getElementById("dni").value = "";
    document.getElementById("errorCliente").textContent = "";
    document.getElementById("clienteElegido").textContent = "Varios (consumidor final)";
}

// ======================= 2. Buscar prenda =======================

async function buscarPrendas(texto) {
    const aviso = document.getElementById("avisoPrendas");
    const tabla = document.getElementById("tablaPrendas");
    const cuerpo = document.getElementById("cuerpoPrendas");
    texto = texto.trim();
    ultimaBusqueda = texto;
    cuerpo.replaceChildren();
    tabla.classList.add("d-none");
    if (texto.length < 2) {
        aviso.textContent = "Escriba al menos 2 letras del nombre o del SKU.";
        return;
    }
    aviso.textContent = "Buscando…";
    try {
        const variantes = await API.get("api/variantes?texto=" + encodeURIComponent(texto));
        if (!variantes) return;
        if (variantes.length === 0) {
            aviso.textContent = "No se encontraron prendas activas con ese texto.";
            return;
        }
        aviso.textContent = variantes.length === 20
            ? "Se muestran las primeras 20. Escriba algo más preciso para afinar."
            : "";
        variantes.forEach((v) => cuerpo.appendChild(filaPrenda(v)));
        tabla.classList.remove("d-none");
    } catch (e) {
        aviso.textContent = "";
        mostrarMensaje(e.message, "danger");
    }
}

function filaPrenda(v) {
    const tr = document.createElement("tr");
    const nombre = v.producto ? v.producto.nombre : "";
    const precio = v.producto ? Number(v.producto.precioVenta) : 0;
    // Nombre arriba y SKU abajo en letra chica: así la tabla entra en el celular.
    const tdPrenda = celda(nombre);
    const sku = document.createElement("small");
    sku.className = "d-block text-muted";
    sku.textContent = v.sku;
    tdPrenda.appendChild(sku);
    tr.append(
        tdPrenda,
        celda(v.talla),
        celda(v.color, "d-none d-sm-table-cell"),
        celda(soles(precio), "text-end text-nowrap"),
        celda(String(v.stock), "text-end" + (v.stock === 0 ? " sin-stock" : ""))
    );
    const td = document.createElement("td");
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "btn btn-sm btn-dark";
    btn.textContent = v.stock === 0 ? "Sin stock" : "Agregar";
    btn.disabled = v.stock === 0;
    btn.addEventListener("click", () => agregarAlCarrito({
        idVariante: v.idVariante,
        descripcion: `${nombre} ${v.talla} ${v.color}`,
        precio: precio,
        stock: v.stock
    }));
    td.appendChild(btn);
    tr.appendChild(td);
    return tr;
}

// ======================= 3. Carrito =======================

function agregarAlCarrito(prenda) {
    const linea = carrito.find((l) => l.idVariante === prenda.idVariante);
    if (linea) {
        if (linea.cantidad >= linea.stock) {
            mostrarMensaje(`Solo hay ${linea.stock} unidad(es) de ${linea.descripcion}.`, "warning");
            return;
        }
        linea.cantidad++;
    } else {
        carrito.push({ ...prenda, cantidad: 1 });
    }
    ocultarMensaje();
    dibujarCarrito();
}

function dibujarCarrito() {
    const cuerpo = document.getElementById("cuerpoCarrito");
    cuerpo.replaceChildren();
    carrito.forEach((l, i) => {
        const tr = document.createElement("tr");
        const tdDesc = celda(l.descripcion);
        const unit = document.createElement("small");
        unit.className = "d-block text-muted";
        unit.textContent = soles(l.precio) + " c/u · stock " + l.stock;
        tdDesc.appendChild(unit);
        tr.appendChild(tdDesc);

        const tdCant = document.createElement("td");
        const input = document.createElement("input");
        input.type = "number";
        input.min = "1";
        input.max = String(l.stock);
        input.value = String(l.cantidad);
        input.className = "form-control form-control-sm cantidad";
        input.setAttribute("aria-label", "Cantidad de " + l.descripcion);
        input.addEventListener("change", () => cambiarCantidad(i, input));
        tdCant.appendChild(input);
        tr.appendChild(tdCant);

        tr.appendChild(celda(soles(l.precio * l.cantidad), "text-end text-nowrap"));

        const tdQuitar = document.createElement("td");
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "btn btn-sm btn-outline-danger";
        btn.textContent = "Quitar";
        btn.addEventListener("click", () => {
            carrito.splice(i, 1);
            dibujarCarrito();
        });
        tdQuitar.appendChild(btn);
        tr.appendChild(tdQuitar);
        cuerpo.appendChild(tr);
    });
    const vacio = carrito.length === 0;
    document.getElementById("tablaCarrito").classList.toggle("d-none", vacio);
    document.getElementById("carritoVacio").classList.toggle("d-none", !vacio);
    document.getElementById("btnGuardar").disabled = vacio || guardando;
    dibujarTotales();
}

function cambiarCantidad(i, input) {
    const l = carrito[i];
    const n = Number(input.value);
    if (!Number.isInteger(n) || n < 1) {
        mostrarMensaje("La cantidad debe ser un número entero mayor que 0.", "warning");
    } else if (n > l.stock) {
        mostrarMensaje(`Solo hay ${l.stock} unidad(es) de ${l.descripcion}.`, "warning");
    } else {
        l.cantidad = n;
        ocultarMensaje();
    }
    dibujarCarrito();   // vuelve a dibujar con la cantidad válida
}

// ======================= Totales (vista previa) =======================
// Se calcula en CÉNTIMOS (enteros) para que 0.1 + 0.2 no dé 0.30000000000000004.
// Misma fórmula que Venta.calcularTotales: IGV incluido en el precio (RN-12).

function calcularTotales() {
    const bruto = carrito.reduce((s, l) => s + Math.round(l.precio * 100) * l.cantidad, 0);
    const prendas = carrito.reduce((s, l) => s + l.cantidad, 0);
    let pct = 0;
    const tipo = valor("tipoDescuento");
    if (tipo === "POR_CANTIDAD" && prendas >= MINIMO_POR_CANTIDAD) {
        pct = PORCENTAJE_POR_CANTIDAD;
    } else if (tipo === "PORCENTAJE") {
        const v = Number(valor("valorDescuento"));
        pct = Number.isFinite(v) && v >= 0 && v <= 100 ? v : 0;
    }
    const descuento = Math.min(redondear(bruto * pct / 100), bruto);
    const total = bruto - descuento;
    const base = redondear(total * 100 / 118);
    return { bruto, descuento, base, igv: total - base, total };
}

function dibujarTotales() {
    const t = calcularTotales();
    document.getElementById("totBruto").textContent = soles(t.bruto / 100);
    document.getElementById("totDescuento").textContent = "− " + soles(t.descuento / 100);
    document.getElementById("totBase").textContent = soles(t.base / 100);
    document.getElementById("totIgv").textContent = soles(t.igv / 100);
    document.getElementById("totTotal").textContent = soles(t.total / 100);
}

// ======================= 4. Registrar =======================

async function registrarVenta() {
    if (guardando) return;
    if (carrito.length === 0) {
        mostrarMensaje("Agregue al menos una prenda al carrito.", "warning");
        return;
    }
    const tipo = valor("tipoDescuento");
    let valorDescuento = null;
    if (tipo === "PORCENTAJE") {
        valorDescuento = Number(valor("valorDescuento"));
        if (valor("valorDescuento") === "" || !(valorDescuento >= 0 && valorDescuento <= 100)) {
            mostrarMensaje("El porcentaje de descuento debe estar entre 0 y 100.", "warning");
            return;
        }
    }

    // Solo ids y cantidades: el precio y los totales los pone el servidor.
    const solicitud = {
        idCliente: cliente ? cliente.idPersona : null,
        metodoPago: valor("metodoPago"),
        tipoDescuento: tipo,
        valorDescuento: valorDescuento,
        lineas: carrito.map((l) => ({ idVariante: l.idVariante, cantidad: l.cantidad }))
    };

    guardando = true;
    const btn = document.getElementById("btnGuardar");
    btn.disabled = true;
    btn.textContent = "Registrando…";
    try {
        const v = await API.post("api/ventas", solicitud);
        if (!v) return;
        const numero = `${v.serie}-${String(v.numero).padStart(6, "0")}`;
        mostrarExito(numero, v.idVenta, v.total);
        carrito = [];
        quitarCliente();
        document.getElementById("tipoDescuento").value = "SIN";
        document.getElementById("valorDescuento").value = "";
        document.getElementById("grupoPorcentaje").classList.add("d-none");
        if (ultimaBusqueda) buscarPrendas(ultimaBusqueda);   // stock actualizado
    } catch (e) {
        // 409 sin stock, 400 datos, 404 prenda... el mensaje claro viene del servidor
        mostrarMensaje(e.message, "danger");
        if (ultimaBusqueda) buscarPrendas(ultimaBusqueda);
    } finally {
        guardando = false;
        btn.textContent = "Registrar venta";
        dibujarCarrito();
    }
}

// ======================= Ayudas =======================

function valor(id) {
    return document.getElementById(id).value;
}

function celda(texto, clase) {
    const td = document.createElement("td");
    td.textContent = texto;
    if (clase) td.className = clase;
    return td;
}

/** Redondeo HALF_UP de un número positivo (el + 1e-9 evita errores tipo 2.4999999). */
function redondear(x) {
    return Math.round(x + 1e-9);
}

function soles(monto) {
    return "S/ " + Number(monto).toFixed(2);
}

function mostrarMensaje(texto, tipo) {
    const m = document.getElementById("mensaje");
    m.replaceChildren(document.createTextNode(texto));
    m.className = "alert alert-" + tipo;
    m.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function mostrarExito(numero, idVenta, total) {
    const m = document.getElementById("mensaje");
    const enlace = document.createElement("a");
    enlace.href = "comprobante.html?id=" + encodeURIComponent(idVenta);
    enlace.className = "alert-link ms-2";
    enlace.textContent = "Ver comprobante";
    m.replaceChildren(
        document.createTextNode(`Venta ${numero} registrada correctamente. Total: ${soles(total)}.`),
        enlace
    );
    m.className = "alert alert-success";
    m.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function ocultarMensaje() {
    document.getElementById("mensaje").className = "alert d-none";
}
