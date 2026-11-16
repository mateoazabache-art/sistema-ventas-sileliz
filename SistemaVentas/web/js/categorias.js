let categorias = [];
let esAdministrador = false;
let idCategoriaPendiente = null;

let temporizadorFormulario;
let temporizadorListado;

const seccionFormulario = document.getElementById("seccion-formulario");
const encabezadoAcciones = document.getElementById("encabezado-acciones");

const formulario = document.getElementById("form-categoria");
const nombreCategoria = document.getElementById("nombre-categoria");
const errorNombreCategoria = document.getElementById("error-nombre-categoria");
const idCategoria = document.getElementById("id-categoria");
const tablaCategorias = document.getElementById("tabla-categorias");
const buscarCategoria = document.getElementById("buscar-categoria");
const filtroEstado = document.getElementById("filtro-estado");
const tituloFormulario = document.getElementById("titulo-formulario");
const btnGuardar = document.getElementById("btn-guardar-categoria");

const mensajeFormulario = document.getElementById("mensaje-formulario");
const mensajeListado = document.getElementById("mensaje-listado");

const modalElemento = document.getElementById("modal-confirmar-desactivacion");
const modalDesactivacion = new bootstrap.Modal(modalElemento);
const btnConfirmarDesactivacion = document.getElementById("btn-confirmar-desactivacion");

// Mostrar mensajes de resultado
function mostrarMensaje(contenedor, mensaje, tipo) {
    const esFormulario = contenedor === mensajeFormulario;
    const temporizador = esFormulario
        ? temporizadorFormulario
        : temporizadorListado;

    clearTimeout(temporizador);

    contenedor.className = "alert alert-" + tipo + " mb-0";
    contenedor.textContent = mensaje;
    contenedor.hidden = false;

    const nuevoTemporizador = setTimeout(() => {
        limpiarMensaje(contenedor);
    }, 10000);

    if (esFormulario) {
        temporizadorFormulario = nuevoTemporizador;
    } else {
        temporizadorListado = nuevoTemporizador;
    }
}

// Limpiar mensajes de resultado
function limpiarMensaje(contenedor) {
    if (contenedor === mensajeFormulario) {
        clearTimeout(temporizadorFormulario);
        temporizadorFormulario = null;
    } else {
        clearTimeout(temporizadorListado);
        temporizadorListado = null;
    }

    contenedor.textContent = "";
    contenedor.hidden = true;
}

// Mostrar error debajo del nombre
function mostrarErrorNombre(mensaje) {
    errorNombreCategoria.textContent = mensaje;
    errorNombreCategoria.hidden = false;
    nombreCategoria.classList.add("is-invalid");
}

// Limpiar error del nombre
function limpiarErrorNombre() {
    errorNombreCategoria.textContent = "";
    errorNombreCategoria.hidden = true;
    nombreCategoria.classList.remove("is-invalid");
}

// Mostrar categorias
function listarCategorias() {
    const busqueda = buscarCategoria.value.trim().toLowerCase();
    const estado = filtroEstado.value;

    const resultado = categorias.filter(categoria => {
        const coincideNombre = categoria.nombre.toLowerCase().includes(busqueda);
        const coincideEstado = estado === ""
            || String(categoria.activo) === estado;

        return coincideNombre && coincideEstado;
    });

    tablaCategorias.innerHTML = "";

    if (resultado.length === 0) {
        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = esAdministrador ? 4 : 3;
        celda.textContent = "No se encontraron categorías.";

        fila.appendChild(celda);
        tablaCategorias.appendChild(fila);
        return;
    }

    resultado.forEach(categoria => {
        const fila = document.createElement("tr");

        const celdaId = document.createElement("td");
        celdaId.textContent = categoria.idCategoria;

        const celdaNombre = document.createElement("td");
        celdaNombre.textContent = categoria.nombre;

        const celdaEstado = document.createElement("td");
        celdaEstado.textContent = categoria.activo ? "Activa" : "Inactiva";

        fila.appendChild(celdaId);
        fila.appendChild(celdaNombre);
        fila.appendChild(celdaEstado);

        if (esAdministrador) {
            const celdaAcciones = document.createElement("td");
            celdaAcciones.className = "ps-3";

            const contenedorAcciones = document.createElement("div");
            contenedorAcciones.className = "d-flex gap-2 flex-wrap";

            const btnEditar = document.createElement("button");
            btnEditar.type = "button";
            btnEditar.className = "btn btn-sm btn-outline-secondary";
            btnEditar.textContent = "Editar";

            btnEditar.addEventListener("click", function() {
                editarCategoria(categoria.idCategoria);
            });

            contenedorAcciones.appendChild(btnEditar);

            if (categoria.activo) {
                const btnDesactivar = document.createElement("button");
                btnDesactivar.type = "button";
                btnDesactivar.className = "btn btn-sm btn-danger";
                btnDesactivar.textContent = "Desactivar";

                btnDesactivar.addEventListener("click", function() {
                    confirmarDesactivacion(categoria.idCategoria);
                });

                contenedorAcciones.appendChild(btnDesactivar);
            }

            celdaAcciones.appendChild(contenedorAcciones);
            fila.appendChild(celdaAcciones);
        }

        tablaCategorias.appendChild(fila);
    });
}

// Obtener categorias del servidor
async function cargarCategorias() {
    try {
        const respuesta = await API.get("api/categorias?pagina=1&tam=50");

        categorias = respuesta.items || [];
        listarCategorias();

        return true;
    } catch (error) {
        mostrarMensaje(
            mensajeListado,
            "No se pudieron cargar las categorías: " + error.message,
            "danger"
        );

        return false;
    }
}

// Registrar o actualizar categoria
formulario.addEventListener("submit", async function(event) {
    event.preventDefault();

    limpiarMensaje(mensajeFormulario);
    limpiarErrorNombre();

    const nombre = nombreCategoria.value.trim();
    const id = Number(idCategoria.value);

    if (!Validaciones.esObligatorio(nombre)) {
        mostrarErrorNombre("Ingresa el nombre de la categoría.");
        nombreCategoria.focus();
        return;
    }

    if (!Validaciones.cumpleLongitudMaxima(nombre, 60)) {
        mostrarErrorNombre("El nombre no puede superar los 60 caracteres.");
        nombreCategoria.focus();
        return;
    }

    btnGuardar.disabled = true;

    try {
        if (id) {
            await API.put("api/categorias/" + id, {
                nombre: nombre
            });

            limpiarFormulario();

            await cargarCategorias();

            mostrarMensaje(
                mensajeFormulario,
                "Categoría actualizada correctamente.",
                "success"
            );
        } else {
            await API.post("api/categorias", {
                nombre: nombre
            });

            limpiarFormulario();

            await cargarCategorias();

            mostrarMensaje(
                mensajeFormulario,
                "Categoría registrada correctamente.",
                "success"
            );
        }
    } catch (error) {
        mostrarMensaje(mensajeFormulario, error.message, "danger");
    } finally {
        btnGuardar.disabled = false;
    }
});

// Cargar categoria en el formulario
function editarCategoria(id) {
    const categoria = categorias.find(
        categoria => categoria.idCategoria === id
    );

    if (!categoria) {
        return;
    }

    limpiarMensaje(mensajeFormulario);
    limpiarErrorNombre();

    idCategoria.value = categoria.idCategoria;
    nombreCategoria.value = categoria.nombre;

    tituloFormulario.textContent = "Editar categoría";
    btnGuardar.textContent = "Actualizar categoría";

    nombreCategoria.focus();
}

// Abrir confirmacion de desactivacion
function confirmarDesactivacion(id) {
    idCategoriaPendiente = id;
    modalDesactivacion.show();
}

// Desactivar categoria
async function cambiarEstado() {
    if (!idCategoriaPendiente) {
        return;
    }

    const id = idCategoriaPendiente;

    btnConfirmarDesactivacion.disabled = true;

    try {
        await API.delete("api/categorias/" + id);

        modalDesactivacion.hide();

        idCategoriaPendiente = null;

        const actualizado = await cargarCategorias();

        if (actualizado) {
            mostrarMensaje(
                mensajeListado,
                "Categoría desactivada correctamente.",
                "success"
            );
        }
    } catch (error) {
        modalDesactivacion.hide();

        mostrarMensaje(mensajeListado, error.message, "danger");
    } finally {
        btnConfirmarDesactivacion.disabled = false;
        idCategoriaPendiente = null;
    }
}

// Confirmar desactivacion
btnConfirmarDesactivacion.addEventListener("click", cambiarEstado);

// Limpiar formulario
function limpiarFormulario() {
    formulario.reset();
    idCategoria.value = "";

    tituloFormulario.textContent = "Registrar categoría";
    btnGuardar.textContent = "Guardar categoría";
}

// Eventos de busqueda y filtros
buscarCategoria.addEventListener("input", listarCategorias);
filtroEstado.addEventListener("change", listarCategorias);

// Limpiar validaciones y mensajes al reiniciar el formulario
formulario.addEventListener("reset", function() {
    limpiarErrorNombre();
    limpiarMensaje(mensajeFormulario);

    tituloFormulario.textContent = "Registrar categoría";
    btnGuardar.textContent = "Guardar categoría";
});

// Eliminar error del nombre al empezar a escribir
nombreCategoria.addEventListener("input", function() {
    limpiarErrorNombre();
});

// Mostrar listado inicial segun el rol
async function inicializarCategorias() {
    seccionFormulario.hidden = true;
    encabezadoAcciones.hidden = true;

    try {
        const sesion = await API.auth.obtenerSesion();

        esAdministrador = sesion && sesion.rol === "ADMINISTRADOR";

        seccionFormulario.hidden = !esAdministrador;
        encabezadoAcciones.hidden = !esAdministrador;
    } catch (error) {
        console.error("No se pudo obtener el rol del usuario:", error);
    }

    await cargarCategorias();
}

inicializarCategorias();
