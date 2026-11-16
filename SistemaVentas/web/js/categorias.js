let categorias = [];
let esAdministrador = false;
let idCategoriaPendiente = null;

let temporizadorFormulario;
let temporizadorListado;
let temporizadorBusqueda;

let paginaCategorias = 1;
const tamPagina = 10;
let totalPaginas = 0;
let totalCategorias = 0;

const seccionFormulario = document.getElementById("seccion-formulario");
const encabezadoAcciones = document.getElementById("encabezado-acciones");

const formulario = document.getElementById("form-categoria");
const nombreCategoria = document.getElementById("nombre-categoria");
const errorNombreCategoria = document.getElementById("error-nombre-categoria");
const idCategoria = document.getElementById("id-categoria");
const tablaCategorias = document.getElementById("tabla-categorias");
const buscarCategoria = document.getElementById("buscar-categoria");
const filtroEstado = document.getElementById("filtro-estado");
const paginacion = document.getElementById("paginacion");
const infoPaginacion = document.getElementById("info-paginacion");
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

// Crear botón de paginación
function crearBotonPagina(texto, pagina, deshabilitado = false, activo = false) {
    const elemento = document.createElement("li");
    elemento.className = "page-item";

    if (deshabilitado) {
        elemento.classList.add("disabled");
    }

    if (activo) {
        elemento.classList.add("active");
    }

    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "page-link";
    boton.textContent = texto;
    boton.disabled = deshabilitado;

    if (activo) {
        boton.setAttribute("aria-current", "page");
    }

    boton.addEventListener("click", function() {
        if (!deshabilitado && pagina !== paginaCategorias) {
            paginaCategorias = pagina;
            cargarCategorias();
        }
    });

    elemento.appendChild(boton);
    return elemento;
}

// Mostrar controles de paginación
function mostrarPaginacion() {
    paginacion.innerHTML = "";

    if (totalPaginas <= 1) {
        infoPaginacion.textContent = totalCategorias === 0
            ? "No hay categorías para mostrar."
            : "Mostrando " + totalCategorias + " categorías.";

        return;
    }

    paginacion.appendChild(
        crearBotonPagina("Anterior", paginaCategorias - 1, paginaCategorias === 1)
    );

    const inicio = Math.max(1, paginaCategorias - 2);
    const fin = Math.min(totalPaginas, paginaCategorias + 2);

    for (let pagina = inicio; pagina <= fin; pagina++) {
        paginacion.appendChild(
            crearBotonPagina(
                String(pagina),
                pagina,
                false,
                pagina === paginaCategorias
            )
        );
    }

    paginacion.appendChild(
        crearBotonPagina(
            "Siguiente",
            paginaCategorias + 1,
            paginaCategorias === totalPaginas
        )
    );

    const desde = (paginaCategorias - 1) * tamPagina + 1;
    const hasta = Math.min(paginaCategorias * tamPagina, totalCategorias);

    infoPaginacion.textContent =
        "Mostrando " + desde + "–" + hasta + " de " +
        totalCategorias + " categorías. Página " +
        paginaCategorias + " de " + totalPaginas + ".";
}

// Mostrar categorías recibidas del servidor
function listarCategorias() {
    tablaCategorias.innerHTML = "";

    if (categorias.length === 0) {
        const fila = document.createElement("tr");
        const celda = document.createElement("td");

        celda.colSpan = esAdministrador ? 4 : 3;
        celda.textContent = "No se encontraron categorías.";

        fila.appendChild(celda);
        tablaCategorias.appendChild(fila);
        return;
    }

    categorias.forEach(categoria => {
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

// Obtener categorías del servidor con búsqueda, filtro y paginación
async function cargarCategorias() {
    try {
        const parametros = new URLSearchParams();

        parametros.set("pagina", paginaCategorias);
        parametros.set("tam", tamPagina);

        const texto = buscarCategoria.value.trim();
        if (texto !== "") {
            parametros.set("texto", texto);
        }

        const estado = filtroEstado.value;
        if (estado !== "") {
            parametros.set("activo", estado);
        }

        const respuesta = await API.get(
            "api/categorias?" + parametros.toString()
        );

        totalPaginas = respuesta.paginas || 0;
        totalCategorias = respuesta.total || 0;

        if (totalPaginas > 0 && paginaCategorias > totalPaginas) {
            paginaCategorias = totalPaginas;
            return cargarCategorias();
        }

        if (totalPaginas === 0 && paginaCategorias > 1) {
            paginaCategorias = 1;
            return cargarCategorias();
        }

        categorias = respuesta.items || [];

        listarCategorias();
        mostrarPaginacion();

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

// Registrar o actualizar categoría
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
            paginaCategorias = 1;

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
            paginaCategorias = 1;

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

// Cargar categoría en el formulario
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

// Abrir confirmación de desactivación
function confirmarDesactivacion(id) {
    idCategoriaPendiente = id;
    modalDesactivacion.show();
}

// Desactivar categoría
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

// Confirmar desactivación
btnConfirmarDesactivacion.addEventListener("click", cambiarEstado);

// Limpiar formulario
function limpiarFormulario() {
    formulario.reset();
    idCategoria.value = "";

    tituloFormulario.textContent = "Registrar categoría";
    btnGuardar.textContent = "Guardar categoría";
}

// Buscar categorías con una pequeña espera
buscarCategoria.addEventListener("input", function() {
    clearTimeout(temporizadorBusqueda);

    temporizadorBusqueda = setTimeout(function() {
        paginaCategorias = 1;
        cargarCategorias();
    }, 300);
});

// Aplicar filtro de estado
filtroEstado.addEventListener("change", function() {
    paginaCategorias = 1;
    cargarCategorias();
});

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

// Mostrar listado inicial según el rol
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
