/**
 * usuarios.html (tarea 11 de Cesia; la armó Rafael como su suplente, plan 7.1).
 * CU-02: alta, edición, desactivar / activar y restablecer clave. Solo ADMINISTRADOR.
 * Usa la API del PR #50 (/api/usuarios). Mismo diseño y forma de trabajo que clientes.js.
 *
 * Reglas del proyecto:
 *  - Todas las llamadas al servidor pasan por API (api.js).
 *  - Los datos se escriben con textContent, nunca con innerHTML.
 *  - Las validaciones de aquí son una ayuda; el servidor vuelve a validar (UsuarioService).
 *  - La clave nunca se muestra ni se guarda en la página: se envía una vez y se borra del formulario.
 */

const TAM_PAGINA = 10;

let numeroPagina = 1;       // página que se está viendo
let textoBusqueda = "";     // lo último que se buscó
let idEditando = null;      // null = usuario nuevo; un número = se está editando ese usuario
let idClave = null;         // usuario al que se le está restableciendo la clave
let miId = null;            // el administrador conectado (no puede desactivarse a sí mismo)

const CAMPOS = ["nombres", "apellidos", "dni", "telefono", "correo", "direccion", "nombreUsuario", "rol", "clave"];
const PATRON_USUARIO = /^[A-Za-z0-9._]{4,30}$/;   // igual que UsuarioService

document.addEventListener("DOMContentLoaded", async () => {
    document.getElementById("formBuscar").addEventListener("submit", (e) => {
        e.preventDefault();
        buscarUsuarios();
    });
    document.getElementById("btnNuevo").addEventListener("click", () => mostrarFormulario(null));
    document.getElementById("btnCancelar").addEventListener("click", ocultarFormulario);
    document.getElementById("formUsuario").addEventListener("submit", (e) => {
        e.preventDefault();
        guardarUsuario();
    });
    document.getElementById("btnCancelarClave").addEventListener("click", ocultarClave);
    document.getElementById("formClave").addEventListener("submit", (e) => {
        e.preventDefault();
        guardarClave();
    });

    try {
        const sesion = await API.auth.obtenerSesion();
        if (!sesion) return;                    // sin sesión: api.js ya mandó al login
        miId = sesion.id;
    } catch (error) {
        miId = null;
    }
    cargarUsuarios();
});

// ---------------------------------------------------------------
// Lista y paginación
// ---------------------------------------------------------------

async function cargarUsuarios() {
    try {
        const datos = await API.get(
            `api/usuarios?pagina=${numeroPagina}&tam=${TAM_PAGINA}&texto=${encodeURIComponent(textoBusqueda)}`);
        if (!datos) return;

        const tbody = document.querySelector("#tablaUsuarios tbody");
        tbody.replaceChildren();

        if (datos.items.length === 0) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 6;
            td.className = "text-center text-muted py-4";
            td.textContent = "No se encontraron usuarios.";
            tr.appendChild(td);
            tbody.appendChild(tr);
        }

        datos.items.forEach(u => tbody.appendChild(crearFila(u)));
        dibujarPaginacion(datos);
    } catch (error) {
        // El vendedor que entra escribiendo la dirección recibe 403 del servidor
        mostrarError(error.message);
        document.getElementById("btnNuevo").classList.add("d-none");
    }
}

/** ¿Sigue bloqueado por intentos fallidos? (Gson no envía bloqueadoHasta si es null) */
function estaBloqueado(u) {
    return Boolean(u.bloqueadoHasta) && new Date(u.bloqueadoHasta) > new Date();
}

/** Arma la fila <tr> de un usuario. */
function crearFila(u) {
    const tr = document.createElement("tr");
    if (!u.activo) tr.classList.add("text-muted");

    const tdUsuario = document.createElement("td");
    tdUsuario.textContent = u.nombreUsuario;

    const tdNombre = document.createElement("td");
    tdNombre.textContent = `${u.nombres} ${u.apellidos}`;
    if (u.idPersona === miId) {
        const yo = document.createElement("span");
        yo.className = "text-muted small ms-1";
        yo.textContent = "(usted)";
        tdNombre.appendChild(yo);
    }

    const tdDni = document.createElement("td");
    tdDni.className = "d-none d-md-table-cell";
    tdDni.textContent = u.dni;

    const tdRol = document.createElement("td");
    tdRol.textContent = u.rol === "ADMINISTRADOR" ? "Administrador" : "Vendedor";

    const tdEstado = document.createElement("td");
    const etiqueta = document.createElement("span");
    if (!u.activo) {
        etiqueta.className = "badge text-bg-secondary";
        etiqueta.textContent = "Inactivo";
    } else if (estaBloqueado(u)) {
        etiqueta.className = "badge text-bg-warning";
        etiqueta.textContent = "Bloqueado";
        etiqueta.title = "5 intentos fallidos. Restablezca la clave para desbloquearlo.";
    } else {
        etiqueta.className = "badge text-bg-success";
        etiqueta.textContent = "Activo";
    }
    tdEstado.appendChild(etiqueta);

    const tdAcciones = document.createElement("td");
    tdAcciones.className = "text-nowrap";
    tdAcciones.append(
        crearBoton("Editar", "btn-outline-primary", () => mostrarFormulario(u)),
        crearBoton("Clave", "btn-outline-secondary", () => mostrarClave(u)));

    // El administrador no puede desactivarse a sí mismo (el servidor también lo impide)
    if (u.activo && u.idPersona !== miId) {
        tdAcciones.appendChild(crearBoton("Desactivar", "btn-danger", () => desactivarUsuario(u)));
    } else if (!u.activo) {
        tdAcciones.appendChild(crearBoton("Activar", "btn-outline-success", () => activarUsuario(u)));
    }

    tr.append(tdUsuario, tdNombre, tdDni, tdRol, tdEstado, tdAcciones);
    return tr;
}

function crearBoton(texto, estilo, alHacerClic) {
    const boton = document.createElement("button");
    boton.type = "button";
    boton.textContent = texto;
    boton.className = `btn btn-sm ${estilo} me-1`;
    boton.addEventListener("click", alHacerClic);
    return boton;
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

    const palabra = datos.total === 1 ? "usuario" : "usuarios";
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
        cargarUsuarios();
    });

    li.appendChild(boton);
    return li;
}

function buscarUsuarios() {
    textoBusqueda = document.getElementById("txtBuscar").value.trim();
    numeroPagina = 1;
    ocultarMensajes();
    cargarUsuarios();
}

// ---------------------------------------------------------------
// Formulario: nuevo y editar
// ---------------------------------------------------------------

/** usuario = null para uno nuevo; o el usuario de la fila para editarlo. */
function mostrarFormulario(usuario) {
    ocultarClave();
    idEditando = usuario ? usuario.idPersona : null;
    document.getElementById("tituloFormulario").textContent =
        usuario ? `Editar usuario ${usuario.nombreUsuario}` : "Nuevo usuario";

    CAMPOS.forEach(campo => {
        const valor = usuario ? (usuario[campo] || "") : "";
        document.getElementById(campo).value = campo === "rol" ? (valor || "VENDEDOR") : valor;
    });
    document.getElementById("clave").value = "";
    // La clave solo se pide al crear; para cambiarla está el botón "Clave"
    document.getElementById("grupoClave").classList.toggle("d-none", usuario !== null);

    limpiarErrores(CAMPOS, "mensaje-formulario");
    ocultarMensajes();
    const seccion = document.getElementById("seccionFormulario");
    seccion.classList.remove("d-none");
    seccion.scrollIntoView({ behavior: "smooth", block: "start" });
    document.getElementById("nombres").focus();
}

function ocultarFormulario() {
    idEditando = null;
    document.getElementById("clave").value = "";
    document.getElementById("seccionFormulario").classList.add("d-none");
}

function leerFormulario() {
    const u = {};
    CAMPOS.forEach(campo => {
        u[campo] = campo === "clave"
            ? document.getElementById(campo).value          // la clave se envía tal cual
            : document.getElementById(campo).value.trim();
    });
    if (idEditando !== null) delete u.clave;                // al editar no se envía clave
    return u;
}

/** Mismas reglas que UsuarioService (RN-04 y RN-11). */
function validarFormulario(u) {
    limpiarErrores(CAMPOS, "mensaje-formulario");
    let valido = true;
    const error = (campo, mensaje) => { marcarError(campo, mensaje); valido = false; };

    if (!Validaciones.esObligatorio(u.nombres)) error("nombres", "Escriba los nombres.");
    if (!Validaciones.esObligatorio(u.apellidos)) error("apellidos", "Escriba los apellidos.");
    if (!Validaciones.esDni(u.dni)) error("dni", "El DNI debe tener exactamente 8 dígitos.");
    if (u.telefono !== "" && !Validaciones.esCelular(u.telefono)) {
        error("telefono", "El celular debe tener 9 dígitos y empezar con 9.");
    }
    if (u.correo !== "" && !Validaciones.esCorreo(u.correo)) {
        error("correo", "Escriba un correo válido, por ejemplo nombre@correo.com.");
    }
    if (!PATRON_USUARIO.test(u.nombreUsuario)) {
        error("nombreUsuario", "De 4 a 30 caracteres: letras sin tilde, números, punto o guion bajo.");
    }
    if (idEditando === null && !clavePermitida(u.clave)) {
        error("clave", "Al menos 8 caracteres, con letras y números.");
    }
    if (idEditando !== null && idEditando === miId && u.rol !== "ADMINISTRADOR") {
        error("rol", "No puede quitarse a usted mismo el rol de administrador.");
    }
    return valido;
}

/** RN-11, igual que Clave.cumplePolitica: 8 o más, con al menos una letra y un número. */
function clavePermitida(clave) {
    return typeof clave === "string" && clave.length >= 8 && /[A-Za-zÁÉÍÓÚáéíóúÑñ]/.test(clave) && /\d/.test(clave);
}

async function guardarUsuario() {
    const usuario = leerFormulario();
    if (!validarFormulario(usuario)) return;

    const btnGuardar = document.getElementById("btnGuardar");
    btnGuardar.disabled = true;         // evita el doble clic
    try {
        if (idEditando === null) {
            await API.post("api/usuarios", usuario);
            numeroPagina = 1;
            mostrarOk(`Usuario ${usuario.nombreUsuario} registrado. Ya puede ingresar con su clave.`);
        } else {
            await API.put(`api/usuarios/${idEditando}`, usuario);
            mostrarOk("Usuario actualizado correctamente.");
        }
        ocultarFormulario();
        cargarUsuarios();
    } catch (error) {
        // 400 o 409: el mensaje claro viene del servidor (DNI o usuario repetido, clave débil...)
        mostrarEnCaja("mensaje-formulario", error.message);
    } finally {
        usuario.clave = "";
        btnGuardar.disabled = false;
    }
}

// ---------------------------------------------------------------
// Restablecer clave
// ---------------------------------------------------------------

function mostrarClave(usuario) {
    ocultarFormulario();
    idClave = usuario.idPersona;
    document.getElementById("tituloClave").textContent =
        `Restablecer clave de ${usuario.nombreUsuario} (${usuario.nombres} ${usuario.apellidos})`;
    document.getElementById("claveNueva").value = "";
    document.getElementById("claveRepetida").value = "";
    limpiarErrores(["claveNueva", "claveRepetida"], "mensaje-clave");
    ocultarMensajes();
    const seccion = document.getElementById("seccionClave");
    seccion.classList.remove("d-none");
    seccion.scrollIntoView({ behavior: "smooth", block: "start" });
    document.getElementById("claveNueva").focus();
}

function ocultarClave() {
    idClave = null;
    document.getElementById("claveNueva").value = "";
    document.getElementById("claveRepetida").value = "";
    document.getElementById("seccionClave").classList.add("d-none");
}

async function guardarClave() {
    const nueva = document.getElementById("claveNueva").value;
    const repetida = document.getElementById("claveRepetida").value;
    limpiarErrores(["claveNueva", "claveRepetida"], "mensaje-clave");

    let valido = true;
    if (!clavePermitida(nueva)) {
        marcarError("claveNueva", "Al menos 8 caracteres, con letras y números.");
        valido = false;
    }
    if (nueva !== repetida) {
        marcarError("claveRepetida", "Las dos claves no son iguales.");
        valido = false;
    }
    if (!valido) return;

    const boton = document.getElementById("btnGuardarClave");
    boton.disabled = true;
    try {
        const respuesta = await API.post(`api/usuarios/${idClave}/clave`, { clave: nueva });
        if (!respuesta) return;
        ocultarClave();
        mostrarOk(respuesta.mensaje || "Clave restablecida.");
        cargarUsuarios();
    } catch (error) {
        mostrarEnCaja("mensaje-clave", error.message);
    } finally {
        boton.disabled = false;
    }
}

// ---------------------------------------------------------------
// Desactivar y activar (RN-05: no se borra, se desactiva)
// ---------------------------------------------------------------

async function desactivarUsuario(u) {
    const pregunta = `¿Seguro que desea desactivar a ${u.nombres} ${u.apellidos} (${u.nombreUsuario})? Ya no podrá ingresar; sus ventas se conservan.`;
    if (!confirm(pregunta)) return;
    try {
        await API.delete(`api/usuarios/${u.idPersona}`);
        mostrarOk("Usuario desactivado.");
        cargarUsuarios();
    } catch (error) {
        mostrarError(error.message);
    }
}

async function activarUsuario(u) {
    try {
        await API.post(`api/usuarios/${u.idPersona}/activar`, {});
        mostrarOk(`Usuario ${u.nombreUsuario} activado.`);
        cargarUsuarios();
    } catch (error) {
        mostrarError(error.message);
    }
}

// ---------------------------------------------------------------
// Errores de campo y mensajes
// ---------------------------------------------------------------

function marcarError(campo, mensaje) {
    document.getElementById(campo).classList.add("is-invalid");
    document.getElementById(`error-${campo}`).textContent = mensaje;
}

function limpiarErrores(campos, idCaja) {
    campos.forEach(campo => {
        document.getElementById(campo).classList.remove("is-invalid");
        document.getElementById(`error-${campo}`).textContent = "";
    });
    document.getElementById(idCaja).classList.add("d-none");
}

function mostrarEnCaja(idCaja, mensaje) {
    const caja = document.getElementById(idCaja);
    caja.textContent = mensaje;
    caja.classList.remove("d-none");
}

function mostrarOk(mensaje) {
    ocultarMensajes();
    const div = document.getElementById("mensaje-ok");
    div.textContent = mensaje;
    div.classList.remove("d-none");
    setTimeout(() => div.classList.add("d-none"), 5000);
}

function mostrarError(mensaje) {
    ocultarMensajes();
    mostrarEnCaja("mensaje-error", mensaje);
}

function ocultarMensajes() {
    document.getElementById("mensaje-ok").classList.add("d-none");
    document.getElementById("mensaje-error").classList.add("d-none");
}
