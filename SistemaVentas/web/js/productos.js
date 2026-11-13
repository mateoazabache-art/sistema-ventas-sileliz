/**
 * productos.html (tareas de Naomi, conectada a la API el 13 nov):
 * lista paginada con buscador y filtro por categoría, alta y edición de productos,
 * desactivar, y las tallas y colores (variantes) de cada producto.
 *
 * API que usa:
 *   GET    api/categorias?tam=50                     categorías para los combos
 *   GET    api/productos?pagina=&tam=&texto=&idCategoria=
 *   POST   api/productos                             201 con el producto creado
 *   PUT    api/productos/{id}
 *   DELETE api/productos/{id}                        desactiva (nunca borra)
 *   GET    api/productos/{id}/variantes              activas e inactivas
 *   POST   api/productos/{id}/variantes              201 con la variante creada
 *   PUT    api/variantes/{id}
 *   DELETE api/variantes/{id}                        desactiva
 *
 * Reglas del proyecto:
 *  - Todas las llamadas al servidor pasan por API (api.js).
 *  - Los datos se escriben con textContent, nunca con innerHTML.
 *  - Las validaciones de aquí son una ayuda; el servidor vuelve a validar (Validador).
 *  - El stock NO se envía: nace en 0, lo suben los ingresos y lo bajan las ventas (plan 4.4).
 *  - Solo el administrador escribe; el vendedor solo consulta (el filtro devuelve 403 igual).
 */

const TAM_PAGINA_PRODUCTOS = 10;

// Lo que la página recuerda mientras está abierta
let numeroPaginaProductos = 1;
let textoProductos = "";
let idCategoriaFiltro = 0;
let esAdministrador = false;
let idProductoEditando = null;   // null = producto nuevo
let productoAbierto = null;      // producto cuyas variantes se están viendo
let idVarianteEditando = null;   // null = variante nueva
let skuEscritoAMano = false;     // si el usuario escribió el SKU, ya no se sugiere

const CAMPOS_PRODUCTO = ["codigo", "nombre", "categoria", "descripcion", "precioVenta"];
const CAMPOS_VARIANTE = ["talla", "color", "sku", "stockMinimo"];

document.addEventListener("DOMContentLoaded", async () => {
    document.getElementById("formBuscar").addEventListener("submit", (e) => {
        e.preventDefault();
        buscarProductos();
    });
    document.getElementById("selFiltroCategoria").addEventListener("change", buscarProductos);
    document.getElementById("btnNuevo").addEventListener("click", () => mostrarFormularioProducto(null));
    document.getElementById("btnCancelarProducto").addEventListener("click", ocultarFormularioProducto);
    document.getElementById("formProducto").addEventListener("submit", (e) => {
        e.preventDefault();
        guardarProducto();
    });

    document.getElementById("btnCerrarVariantes").addEventListener("click", cerrarVariantes);
    document.getElementById("btnCancelarVariante").addEventListener("click", limpiarFormularioVariante);
    document.getElementById("formVariante").addEventListener("submit", (e) => {
        e.preventDefault();
        guardarVariante();
    });
    document.getElementById("talla").addEventListener("input", sugerirSku);
    document.getElementById("color").addEventListener("input", sugerirSku);
    document.getElementById("sku").addEventListener("input", () => { skuEscritoAMano = true; });

    // El rol sale de la sesión. Si el vendedor forzara un botón, el servidor igual responde 403.
    try {
        const sesion = await API.auth.obtenerSesion();
        if (!sesion) return;                     // sin sesión: api.js ya mandó al login
        esAdministrador = sesion.rol === "ADMINISTRADOR";
    } catch (error) {
        esAdministrador = false;
    }
    document.getElementById("btnNuevo").classList.toggle("d-none", !esAdministrador);

    await cargarCategorias();
    cargarProductos();
});

// ---------------------------------------------------------------
// Categorías (para el filtro y el formulario)
// ---------------------------------------------------------------

async function cargarCategorias() {
    try {
        const datos = await API.get("api/categorias?pagina=1&tam=50");
        if (!datos) return;
        const activas = datos.items.filter(c => c.activo);

        const filtro = document.getElementById("selFiltroCategoria");
        const combo = document.getElementById("categoria");
        filtro.replaceChildren(new Option("Todas las categorías", "0"));
        combo.replaceChildren(new Option("Seleccione...", ""));
        activas.forEach(c => {
            filtro.add(new Option(c.nombre, c.idCategoria));
            combo.add(new Option(c.nombre, c.idCategoria));
        });
    } catch (error) {
        mostrarError("No se pudieron cargar las categorías: " + error.message);
    }
}

// ---------------------------------------------------------------
// Lista de productos y paginación
// ---------------------------------------------------------------

async function cargarProductos() {
    try {
        const datos = await API.get(`api/productos?pagina=${numeroPaginaProductos}&tam=${TAM_PAGINA_PRODUCTOS}`
            + `&texto=${encodeURIComponent(textoProductos)}&idCategoria=${idCategoriaFiltro}`);
        if (!datos) return;

        const tbody = document.querySelector("#tablaProductos tbody");
        tbody.replaceChildren();

        if (datos.items.length === 0) {
            tbody.appendChild(filaVacia(6, "No se encontraron productos."));
        }
        datos.items.forEach(p => tbody.appendChild(crearFilaProducto(p)));
        dibujarPaginacion(datos);
    } catch (error) {
        mostrarError(error.message);
    }
}

function crearFilaProducto(p) {
    const tr = document.createElement("tr");
    if (!p.activo) tr.classList.add("text-muted");

    const tdCodigo = celda(p.codigo);
    const tdNombre = celda(p.nombre);
    const tdCategoria = celda(p.categoria ? p.categoria.nombre : "-");
    tdCategoria.className = "d-none d-md-table-cell";
    const tdPrecio = celda(soles(p.precioVenta));
    tdPrecio.className = "text-end text-nowrap";
    const tdEstado = document.createElement("td");
    tdEstado.appendChild(etiquetaEstado(p.activo));

    const tdAcciones = document.createElement("td");
    tdAcciones.className = "text-nowrap";
    tdAcciones.appendChild(boton(esAdministrador ? "Tallas" : "Ver tallas",
        "btn btn-sm btn-outline-dark me-1", () => abrirVariantes(p)));

    if (esAdministrador) {
        tdAcciones.appendChild(boton("Editar", "btn btn-sm btn-outline-primary me-1",
            () => mostrarFormularioProducto(p)));
        if (p.activo) {
            tdAcciones.appendChild(boton("Desactivar", "btn btn-sm btn-danger",
                () => desactivarProducto(p)));
        }
    }

    tr.append(tdCodigo, tdNombre, tdCategoria, tdPrecio, tdEstado, tdAcciones);
    return tr;
}

/** Dibuja: Anterior, 1, 2, 3..., Siguiente. datos = {pagina, paginas, total}. */
function dibujarPaginacion(datos) {
    const lista = document.getElementById("paginacion");
    lista.replaceChildren();

    const paginas = Math.max(datos.paginas, 1);
    const actual = datos.pagina;

    lista.appendChild(botonPagina("Anterior", actual - 1, actual <= 1, false));
    for (let n = 1; n <= paginas; n++) {
        lista.appendChild(botonPagina(String(n), n, false, n === actual));
    }
    lista.appendChild(botonPagina("Siguiente", actual + 1, actual >= paginas, false));

    const palabra = datos.total === 1 ? "producto" : "productos";
    document.getElementById("infoPaginacion").textContent =
        `Página ${actual} de ${paginas} (${datos.total} ${palabra})`;
}

function botonPagina(texto, pagina, deshabilitado, esActual) {
    const li = document.createElement("li");
    li.className = "page-item";
    if (deshabilitado) li.classList.add("disabled");
    if (esActual) li.classList.add("active");

    const b = document.createElement("button");
    b.type = "button";
    b.className = "page-link";
    b.textContent = texto;
    b.disabled = deshabilitado;
    if (esActual) b.setAttribute("aria-current", "page");
    b.addEventListener("click", () => {
        numeroPaginaProductos = pagina;
        cargarProductos();
    });

    li.appendChild(b);
    return li;
}

function buscarProductos() {
    textoProductos = document.getElementById("txtBuscar").value.trim();
    idCategoriaFiltro = Number(document.getElementById("selFiltroCategoria").value) || 0;
    numeroPaginaProductos = 1;            // una búsqueda nueva empieza en la página 1
    ocultarMensajes();
    cargarProductos();
}

// ---------------------------------------------------------------
// Formulario de producto: nuevo y editar
// ---------------------------------------------------------------

function mostrarFormularioProducto(p) {
    idProductoEditando = p ? p.idProducto : null;
    document.getElementById("tituloProducto").textContent = p ? `Editar producto ${p.codigo}` : "Nuevo producto";
    document.getElementById("codigo").value = p ? p.codigo : "";
    document.getElementById("nombre").value = p ? p.nombre : "";
    document.getElementById("categoria").value = p && p.categoria ? String(p.categoria.idCategoria) : "";
    document.getElementById("descripcion").value = p && p.descripcion ? p.descripcion : "";
    document.getElementById("precioVenta").value = p ? Number(p.precioVenta).toFixed(2) : "";

    limpiarErrores(CAMPOS_PRODUCTO, "mensaje-producto");
    ocultarMensajes();
    const seccion = document.getElementById("seccionProducto");
    seccion.classList.remove("d-none");
    seccion.scrollIntoView({ behavior: "smooth", block: "start" });
    document.getElementById("codigo").focus();
}

function ocultarFormularioProducto() {
    document.getElementById("seccionProducto").classList.add("d-none");
    document.getElementById("formProducto").reset();
    idProductoEditando = null;
}

function leerProducto() {
    return {
        codigo: document.getElementById("codigo").value.trim(),
        nombre: document.getElementById("nombre").value.trim(),
        descripcion: document.getElementById("descripcion").value.trim(),
        idCategoria: document.getElementById("categoria").value,
        precioVenta: document.getElementById("precioVenta").value.trim()
    };
}

/** Mismas reglas que ProductoService (largos 20/100/250, precio > 0). */
function validarProducto(d) {
    const errores = {};
    if (!Validaciones.esObligatorio(d.codigo)) errores.codigo = "Ingrese el código.";
    else if (!Validaciones.cumpleLongitudMaxima(d.codigo, 20)) errores.codigo = "Máximo 20 caracteres.";

    if (!Validaciones.esObligatorio(d.nombre)) errores.nombre = "Ingrese el nombre.";
    else if (!Validaciones.cumpleLongitudMaxima(d.nombre, 100)) errores.nombre = "Máximo 100 caracteres.";

    if (!Validaciones.esObligatorio(d.idCategoria)) errores.categoria = "Elija una categoría.";

    if (!Validaciones.cumpleLongitudMaxima(d.descripcion, 250)) errores.descripcion = "Máximo 250 caracteres.";

    if (!Validaciones.esNumeroPositivo(d.precioVenta)) errores.precioVenta = "El precio debe ser mayor que 0.";
    else if (!/^\d+(\.\d{1,2})?$/.test(d.precioVenta)) errores.precioVenta = "Use como máximo 2 decimales.";
    return errores;
}

async function guardarProducto() {
    limpiarErrores(CAMPOS_PRODUCTO, "mensaje-producto");
    const d = leerProducto();
    const errores = validarProducto(d);
    if (marcarErrores(errores)) return;

    // El JSON tiene la forma de la clase Producto (categoria es un objeto)
    const cuerpo = {
        codigo: d.codigo,
        nombre: d.nombre,
        descripcion: d.descripcion || null,
        categoria: { idCategoria: Number(d.idCategoria) },
        precioVenta: Number(d.precioVenta)
    };

    const boton = document.getElementById("btnGuardarProducto");
    boton.disabled = true;
    try {
        if (idProductoEditando === null) {
            const creado = await API.post("api/productos", cuerpo);
            if (!creado) return;
            ocultarFormularioProducto();
            mostrarOk(`Producto ${creado.codigo} registrado. Ahora agregue sus tallas y colores.`);
            await cargarProductos();
            const categoria = document.querySelector(`#categoria option[value="${cuerpo.categoria.idCategoria}"]`);
            creado.categoria = { idCategoria: cuerpo.categoria.idCategoria,
                                 nombre: categoria ? categoria.textContent : "" };
            creado.activo = true;
            abrirVariantes(creado);
        } else {
            await API.put(`api/productos/${idProductoEditando}`, cuerpo);
            const id = idProductoEditando;
            ocultarFormularioProducto();
            mostrarOk(`Producto ${cuerpo.codigo} actualizado.`);
            await cargarProductos();
            if (productoAbierto && productoAbierto.idProducto === id) {
                productoAbierto = { ...productoAbierto, ...cuerpo };
                pintarTituloVariantes();
            }
        }
    } catch (error) {
        mostrarErrorEn("mensaje-producto", error.message);   // 400 o 409 del servidor (código repetido, etc.)
    } finally {
        boton.disabled = false;
    }
}

async function desactivarProducto(p) {
    if (!confirm(`¿Desactivar el producto ${p.codigo} - ${p.nombre}?\n`
            + "Ya no se podrá vender, pero sus ventas anteriores se conservan.")) return;
    ocultarMensajes();
    try {
        await API.delete(`api/productos/${p.idProducto}`);
        mostrarOk(`Producto ${p.codigo} desactivado.`);
        await cargarProductos();
        if (productoAbierto && productoAbierto.idProducto === p.idProducto) {
            productoAbierto.activo = false;
            abrirVariantes(productoAbierto);
        }
    } catch (error) {
        mostrarError(error.message);
    }
}

// ---------------------------------------------------------------
// Variantes (tallas y colores) del producto elegido
// ---------------------------------------------------------------

async function abrirVariantes(p) {
    productoAbierto = p;
    pintarTituloVariantes();
    limpiarFormularioVariante();

    // Se agregan variantes solo si es administrador y el producto está activo
    const puedeEscribir = esAdministrador && p.activo;
    document.getElementById("formVariante").classList.toggle("d-none", !puedeEscribir);

    const seccion = document.getElementById("seccionVariantes");
    seccion.classList.remove("d-none");
    await cargarVariantes();
    seccion.scrollIntoView({ behavior: "smooth", block: "start" });
}

function pintarTituloVariantes() {
    const p = productoAbierto;
    const estado = p.activo === false ? " (inactivo)" : "";
    document.getElementById("tituloVariantes").textContent =
        `Tallas y colores de ${p.codigo} - ${p.nombre}${estado}`;
}

function cerrarVariantes() {
    document.getElementById("seccionVariantes").classList.add("d-none");
    productoAbierto = null;
    limpiarFormularioVariante();
}

async function cargarVariantes() {
    const tbody = document.querySelector("#tablaVariantes tbody");
    try {
        const lista = await API.get(`api/productos/${productoAbierto.idProducto}/variantes`);
        if (!lista) return;
        tbody.replaceChildren();
        if (lista.length === 0) {
            tbody.appendChild(filaVacia(7, esAdministrador && productoAbierto.activo
                ? "Este producto todavía no tiene tallas. Agréguelas con el formulario de abajo."
                : "Este producto no tiene tallas registradas."));
        }
        lista.forEach(v => tbody.appendChild(crearFilaVariante(v)));
    } catch (error) {
        mostrarErrorEn("mensaje-variante", error.message);
    }
}

function crearFilaVariante(v) {
    const tr = document.createElement("tr");
    if (!v.activo) tr.classList.add("text-muted");

    const tdStock = document.createElement("td");
    tdStock.className = "text-end text-nowrap";
    tdStock.append(String(v.stock));
    if (v.activo && v.stock <= v.stockMinimo) {
        const aviso = document.createElement("span");
        aviso.className = "badge text-bg-warning ms-1";
        aviso.textContent = v.stock === 0 ? "Sin stock" : "Stock bajo";
        tdStock.appendChild(aviso);
    }
    const tdMinimo = celda(String(v.stockMinimo));
    tdMinimo.className = "text-end";
    const tdEstado = document.createElement("td");
    tdEstado.appendChild(etiquetaEstado(v.activo));

    const tdAcciones = document.createElement("td");
    tdAcciones.className = "text-nowrap";
    if (esAdministrador && v.activo && productoAbierto.activo) {
        tdAcciones.appendChild(boton("Editar", "btn btn-sm btn-outline-primary me-1",
            () => editarVariante(v)));
        tdAcciones.appendChild(boton("Desactivar", "btn btn-sm btn-outline-danger",
            () => desactivarVariante(v)));
    }

    tr.append(celda(v.talla), celda(v.color), celda(v.sku), tdStock, tdMinimo, tdEstado, tdAcciones);
    return tr;
}

function editarVariante(v) {
    idVarianteEditando = v.idVariante;
    skuEscritoAMano = true;                  // al editar no se cambia el SKU solo
    document.getElementById("tituloFormVariante").textContent = `Editar ${v.sku}`;
    document.getElementById("talla").value = v.talla;
    document.getElementById("color").value = v.color;
    document.getElementById("sku").value = v.sku;
    document.getElementById("stockMinimo").value = v.stockMinimo;
    document.getElementById("btnGuardarVariante").textContent = "Guardar cambios";
    document.getElementById("btnCancelarVariante").classList.remove("d-none");
    limpiarErrores(CAMPOS_VARIANTE, "mensaje-variante");
    document.getElementById("talla").focus();
}

function limpiarFormularioVariante() {
    idVarianteEditando = null;
    skuEscritoAMano = false;
    document.getElementById("formVariante").reset();
    document.getElementById("stockMinimo").value = "2";
    document.getElementById("tituloFormVariante").textContent = "Agregar talla y color";
    document.getElementById("btnGuardarVariante").textContent = "Agregar";
    document.getElementById("btnCancelarVariante").classList.add("d-none");
    limpiarErrores(CAMPOS_VARIANTE, "mensaje-variante");
}

/** Sugiere el SKU con el formato de los datos de la tienda: P-001-XL-BLA. */
function sugerirSku() {
    if (skuEscritoAMano || !productoAbierto) return;
    const talla = sinTildes(document.getElementById("talla").value).toUpperCase().replace(/\s+/g, "");
    const color = sinTildes(document.getElementById("color").value).toUpperCase().replace(/[^A-Z]/g, "").substring(0, 3);
    document.getElementById("sku").value = talla && color
        ? `${productoAbierto.codigo}-${talla}-${color}`.substring(0, 30)
        : "";
}

function leerVariante() {
    return {
        talla: document.getElementById("talla").value.trim().toUpperCase(),   // "2xl" -> "2XL", como los datos de la tienda
        color: document.getElementById("color").value.trim(),
        sku: document.getElementById("sku").value.trim(),
        stockMinimo: document.getElementById("stockMinimo").value.trim()
    };
}

/** Mismas reglas que VarianteService (largos 10/30/30, mínimo entero >= 0). */
function validarVariante(d) {
    const errores = {};
    if (!Validaciones.esObligatorio(d.talla)) errores.talla = "Ingrese la talla.";
    else if (!Validaciones.cumpleLongitudMaxima(d.talla, 10)) errores.talla = "Máximo 10 caracteres.";

    if (!Validaciones.esObligatorio(d.color)) errores.color = "Ingrese el color.";
    else if (!Validaciones.cumpleLongitudMaxima(d.color, 30)) errores.color = "Máximo 30 caracteres.";

    if (!Validaciones.esObligatorio(d.sku)) errores.sku = "Ingrese el SKU.";
    else if (!Validaciones.cumpleLongitudMaxima(d.sku, 30)) errores.sku = "Máximo 30 caracteres.";

    if (!Validaciones.esNumeroNoNegativo(d.stockMinimo)) errores.stockMinimo = "Entero desde 0.";
    return errores;
}

async function guardarVariante() {
    limpiarErrores(CAMPOS_VARIANTE, "mensaje-variante");
    const d = leerVariante();
    const errores = validarVariante(d);
    if (marcarErrores(errores)) return;

    // No se envía el stock: el servidor lo ignora y lo maneja solo (regla única del stock)
    const cuerpo = { talla: d.talla, color: d.color, sku: d.sku, stockMinimo: Number(d.stockMinimo) };

    const boton = document.getElementById("btnGuardarVariante");
    boton.disabled = true;
    try {
        if (idVarianteEditando === null) {
            const creada = await API.post(`api/productos/${productoAbierto.idProducto}/variantes`, cuerpo);
            if (!creada) return;
            mostrarOk(`Talla ${creada.talla} ${creada.color} (${creada.sku}) agregada con stock 0.`);
        } else {
            await API.put(`api/variantes/${idVarianteEditando}`, cuerpo);
            mostrarOk(`Variante ${cuerpo.sku} actualizada.`);
        }
        limpiarFormularioVariante();
        await cargarVariantes();
        document.getElementById("talla").focus();
    } catch (error) {
        mostrarErrorEn("mensaje-variante", error.message);   // 409: SKU o talla+color repetidos
    } finally {
        boton.disabled = false;
    }
}

async function desactivarVariante(v) {
    if (!confirm(`¿Desactivar la talla ${v.talla} ${v.color} (${v.sku})?\nYa no aparecerá en las ventas.`)) return;
    ocultarMensajes();
    try {
        await API.delete(`api/variantes/${v.idVariante}`);
        mostrarOk(`Variante ${v.sku} desactivada.`);
        if (idVarianteEditando === v.idVariante) limpiarFormularioVariante();
        await cargarVariantes();
    } catch (error) {
        mostrarErrorEn("mensaje-variante", error.message);
    }
}

// ---------------------------------------------------------------
// Ayudantes
// ---------------------------------------------------------------

function celda(texto) {
    const td = document.createElement("td");
    td.textContent = texto;
    return td;
}

function filaVacia(columnas, texto) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = columnas;
    td.className = "text-center text-muted py-4";
    td.textContent = texto;
    tr.appendChild(td);
    return tr;
}

function boton(texto, clases, alHacerClic) {
    const b = document.createElement("button");
    b.type = "button";
    b.className = clases;
    b.textContent = texto;
    b.addEventListener("click", alHacerClic);
    return b;
}

function etiquetaEstado(activo) {
    const span = document.createElement("span");
    span.className = activo ? "badge text-bg-success" : "badge text-bg-secondary";
    span.textContent = activo ? "Activo" : "Inactivo";
    return span;
}

/** 59.9 -> "S/ 59.90" */
function soles(valor) {
    return "S/ " + Number(valor || 0).toFixed(2);
}

function sinTildes(texto) {
    return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/ñ/gi, "N");
}

/** Marca en rojo cada campo con error. Devuelve true si hubo alguno. */
function marcarErrores(errores) {
    const campos = Object.keys(errores);
    campos.forEach(campo => {
        document.getElementById(campo).classList.add("is-invalid");
        document.getElementById(`error-${campo}`).textContent = errores[campo];
    });
    if (campos.length > 0) document.getElementById(campos[0]).focus();
    return campos.length > 0;
}

function limpiarErrores(campos, idMensaje) {
    campos.forEach(campo => {
        document.getElementById(campo).classList.remove("is-invalid");
        document.getElementById(`error-${campo}`).textContent = "";
    });
    const div = document.getElementById(idMensaje);
    div.classList.add("d-none");
    div.textContent = "";
}

function mostrarOk(mensaje) {
    ocultarMensajes();
    const div = document.getElementById("mensaje-ok");
    div.textContent = mensaje;
    div.classList.remove("d-none");
    setTimeout(() => div.classList.add("d-none"), 5000);
}

function mostrarError(mensaje) {
    const div = document.getElementById("mensaje-error");
    div.textContent = mensaje;
    div.classList.remove("d-none");
}

function mostrarErrorEn(idDiv, mensaje) {
    const div = document.getElementById(idDiv);
    div.textContent = mensaje;
    div.classList.remove("d-none");
}

function ocultarMensajes() {
    document.getElementById("mensaje-ok").classList.add("d-none");
    document.getElementById("mensaje-error").classList.add("d-none");
}
