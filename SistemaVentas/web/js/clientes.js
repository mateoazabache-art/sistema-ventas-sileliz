/**
 * clientes.html (tarea 10 de Mateo): lista paginada, buscador, alta, edición y desactivar.
 *
 * Reglas del proyecto:
 *  - Todas las llamadas al servidor pasan por API (api.js).
 *  - Los datos se escriben con textContent, nunca con innerHTML.
 *  - Las validaciones de aquí son una ayuda para el usuario; el servidor vuelve a validar.
 */

const TAM_PAGINA = 10;

// Lo que la página recuerda mientras está abierta
let numeroPagina = 1;       // página que se está viendo
let textoBusqueda = "";     // lo último que se buscó
let esAdmin = false;        // solo el administrador ve el botón Desactivar
let idEditando = null;      // null = cliente nuevo; un número = se está editando ese cliente

const CAMPOS = ["nombres", "apellidos", "dni", "telefono", "correo", "direccion", "observaciones"];

document.addEventListener("DOMContentLoaded", async () => {
    document.getElementById("formBuscar").addEventListener("submit", (e) => {
        e.preventDefault();
        buscarClientes();
    });
    document.getElementById("btnNuevo").addEventListener("click", () => mostrarFormulario(null));
    document.getElementById("btnCancelar").addEventListener("click", ocultarFormulario);
    document.getElementById("formCliente").addEventListener("submit", (e) => {
        e.preventDefault();
        guardarCliente();
    });

    // El rol sale de la sesión. Si el vendedor forzara el botón, el servidor igual lo rechaza.
    try {
        const sesion = await API.auth.obtenerSesion();
        if (!sesion) return;                    // sin sesión: api.js ya mandó al login
        esAdmin = sesion.rol === "ADMINISTRADOR";
    } catch (error) {
        esAdmin = false;
    }
    cargarClientes();
});

// ---------------------------------------------------------------
// Lista y paginación
// ---------------------------------------------------------------

async function cargarClientes() {
    try {
        const datos = await API.get(
            `api/clientes?pagina=${numeroPagina}&tam=${TAM_PAGINA}&texto=${encodeURIComponent(textoBusqueda)}`);
        if (!datos) return;

        const tbody = document.querySelector("#tablaClientes tbody");
        tbody.replaceChildren();                // vacía la tabla

        if (datos.items.length === 0) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 6;
            td.className = "text-center text-muted py-4";
            td.textContent = "No se encontraron clientes.";
            tr.appendChild(td);
            tbody.appendChild(tr);
        }

        datos.items.forEach(c => tbody.appendChild(crearFila(c)));
        dibujarPaginacion(datos);
    } catch (error) {
        mostrarError(error.message);
    }
}

/** Arma la fila <tr> de un cliente. */
function crearFila(c) {
    const tr = document.createElement("tr");
    if (!c.activo) tr.classList.add("text-muted");

    const tdDni = document.createElement("td");
    tdDni.textContent = c.dni;

    const tdNombre = document.createElement("td");
    tdNombre.textContent = `${c.nombres} ${c.apellidos}`;

    const tdTel = document.createElement("td");
    tdTel.textContent = c.telefono || "-";

    const tdCorreo = document.createElement("td");
    tdCorreo.textContent = c.correo || "-";

    const tdEstado = document.createElement("td");
    const etiqueta = document.createElement("span");
    etiqueta.className = c.activo ? "badge text-bg-success" : "badge text-bg-secondary";
    etiqueta.textContent = c.activo ? "Activo" : "Inactivo";
    tdEstado.appendChild(etiqueta);

    const tdAcciones = document.createElement("td");
    tdAcciones.className = "text-nowrap";

    const btnEditar = document.createElement("button");
    btnEditar.type = "button";
    btnEditar.textContent = "Editar";
    btnEditar.className = "btn btn-sm btn-outline-primary me-1";
    btnEditar.addEventListener("click", () => mostrarFormulario(c));
    tdAcciones.appendChild(btnEditar);

    // Plan 2.1: solo el administrador desactiva clientes
    if (esAdmin && c.activo) {
        const btnDesactivar = document.createElement("button");
        btnDesactivar.type = "button";
        btnDesactivar.textContent = "Desactivar";
        btnDesactivar.className = "btn btn-sm btn-danger";
        btnDesactivar.addEventListener("click", () => desactivarCliente(c));
        tdAcciones.appendChild(btnDesactivar);
    }

    tr.append(tdDni, tdNombre, tdTel, tdCorreo, tdEstado, tdAcciones);
    return tr;
}

/** Dibuja: Anterior, 1, 2, 3..., Siguiente. datos = {pagina, paginas, total}. */
function dibujarPaginacion(datos) {
    const lista = document.getElementById("paginacion");
    lista.replaceChildren();

    const paginas = Math.max(datos.paginas, 1);
    const actual = datos.pagina;

    lista.appendChild(crearBotonPagina("Anterior", actual - 1, actual <= 1, false));
    for (let n = 1; n <= paginas; n++) {
        lista.appendChild(crearBotonPagina(String(n), n, false, n === actual));
    }
    lista.appendChild(crearBotonPagina("Siguiente", actual + 1, actual >= paginas, false));

    const palabra = datos.total === 1 ? "cliente" : "clientes";
    document.getElementById("infoPaginacion").textContent =
        `Página ${actual} de ${paginas} (${datos.total} ${palabra})`;
}

function crearBotonPagina(texto, pagina, deshabilitado, esActual) {
    const li = document.createElement("li");
    li.className = "page-item";
    if (deshabilitado) li.classList.add("disabled");
    if (esActual) li.classList.add("active");

    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "page-link";
    boton.textContent = texto;
    boton.disabled = deshabilitado;
    if (esActual) boton.setAttribute("aria-current", "page");
    boton.addEventListener("click", () => {
        numeroPagina = pagina;
        cargarClientes();
    });

    li.appendChild(boton);
    return li;
}

function buscarClientes() {
    textoBusqueda = document.getElementById("txtBuscar").value.trim();
    numeroPagina = 1;           // una búsqueda nueva empieza en la página 1
    ocultarMensajes();
    cargarClientes();
}

// ---------------------------------------------------------------
// Formulario: nuevo y editar
// ---------------------------------------------------------------

/** cliente = null para uno nuevo; o el cliente de la fila para editarlo. */
function mostrarFormulario(cliente) {
    idEditando = cliente ? cliente.idPersona : null;
    document.getElementById("tituloFormulario").textContent =
        cliente ? "Editar cliente" : "Nuevo cliente";

    CAMPOS.forEach(campo => {
        document.getElementById(campo).value = cliente ? (cliente[campo] || "") : "";
    });

    limpiarErroresFormulario();
    ocultarMensajes();
    const seccion = document.getElementById("seccionFormulario");
    seccion.classList.remove("d-none");
    seccion.scrollIntoView({ behavior: "smooth", block: "start" });
    document.getElementById("nombres").focus();
}

function ocultarFormulario() {
    idEditando = null;
    document.getElementById("seccionFormulario").classList.add("d-none");
}

/** Lee el formulario ya sin espacios sobrantes. */
function leerFormulario() {
    const cliente = {};
    CAMPOS.forEach(campo => {
        cliente[campo] = document.getElementById(campo).value.trim();
    });
    return cliente;
}

/**
 * Mismas reglas que el servidor (RN-04), usando validaciones.js de Naomi.
 * Devuelve true si todo está bien; si no, marca cada campo con su mensaje.
 */
function validarFormulario(c) {
    limpiarErroresFormulario();
    let valido = true;

    function error(campo, mensaje) {
        document.getElementById(campo).classList.add("is-invalid");
        document.getElementById(`error-${campo}`).textContent = mensaje;
        valido = false;
    }

    if (!Validaciones.esObligatorio(c.nombres)) {
        error("nombres", "Escriba los nombres.");
    }
    if (!Validaciones.esObligatorio(c.apellidos)) {
        error("apellidos", "Escriba los apellidos.");
    }
    if (!Validaciones.esDni(c.dni)) {
        error("dni", "El DNI debe tener exactamente 8 dígitos.");
    }
    // Celular y correo son opcionales: solo se revisan si se escribieron
    if (c.telefono !== "" && !Validaciones.esCelular(c.telefono)) {
        error("telefono", "El celular debe tener 9 dígitos y empezar con 9.");
    }
    if (c.correo !== "" && !Validaciones.esCorreo(c.correo)) {
        error("correo", "Escriba un correo válido, por ejemplo nombre@correo.com.");
    }
    return valido;
}

async function guardarCliente() {
    const cliente = leerFormulario();
    if (!validarFormulario(cliente)) return;

    const btnGuardar = document.getElementById("btnGuardar");
    btnGuardar.disabled = true;         // evita el doble clic
    try {
        if (idEditando === null) {
            await API.post("api/clientes", cliente);
            numeroPagina = 1;
            mostrarOk("Cliente registrado correctamente.");
        } else {
            await API.put(`api/clientes/${idEditando}`, cliente);
            mostrarOk("Cliente actualizado correctamente.");
        }
        ocultarFormulario();
        cargarClientes();
    } catch (error) {
        // 400 o 409: el mensaje claro viene del servidor (por ejemplo, DNI repetido)
        const caja = document.getElementById("mensaje-formulario");
        caja.textContent = error.message;
        caja.classList.remove("d-none");
    } finally {
        btnGuardar.disabled = false;
    }
}

function limpiarErroresFormulario() {
    CAMPOS.forEach(campo => {
        document.getElementById(campo).classList.remove("is-invalid");
        document.getElementById(`error-${campo}`).textContent = "";
    });
    document.getElementById("mensaje-formulario").classList.add("d-none");
}

// ---------------------------------------------------------------
// Desactivar (RN-05: no se borra, se desactiva)
// ---------------------------------------------------------------

async function desactivarCliente(c) {
    const pregunta = `¿Seguro que desea desactivar a ${c.nombres} ${c.apellidos}?`;
    if (!confirm(pregunta)) return;
    try {
        await API.delete(`api/clientes/${c.idPersona}`);
        mostrarOk("Cliente desactivado.");
        cargarClientes();
    } catch (error) {
        mostrarError(error.message);
    }
}

// ---------------------------------------------------------------
// Mensajes
// ---------------------------------------------------------------

function mostrarOk(mensaje) {
    ocultarMensajes();
    const div = document.getElementById("mensaje-ok");
    div.textContent = mensaje;
    div.classList.remove("d-none");
    setTimeout(() => div.classList.add("d-none"), 5000);
}

function mostrarError(mensaje) {
    ocultarMensajes();
    const div = document.getElementById("mensaje-error");
    div.textContent = mensaje;
    div.classList.remove("d-none");
}

function ocultarMensajes() {
    document.getElementById("mensaje-ok").classList.add("d-none");
    document.getElementById("mensaje-error").classList.add("d-none");
}
