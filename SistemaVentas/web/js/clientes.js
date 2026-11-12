let numeroPagina = 1;

document.addEventListener("DOMContentLoaded", () => {
    cargarClientes();
});

async function cargarClientes(texto = "") {
    try {
        const datos = await API.get(`api/clientes?pagina=${numeroPagina}&tam=10&texto=${encodeURIComponent(texto)}`);
        const tbody = document.querySelector("#tablaClientes tbody");
        tbody.innerHTML = ""; // Limpiar tabla, es seguro si no inyectamos datos directo aquí

        datos.items.forEach(c => {
            const tr = document.createElement("tr");

            const tdDni = document.createElement("td");
            tdDni.textContent = c.dni;

            const tdNombre = document.createElement("td");
            tdNombre.textContent = `${c.nombres} ${c.apellidos}`;

            const tdTel = document.createElement("td");
            tdTel.textContent = c.telefono || "-";

            const tdAcciones = document.createElement("td");
            const btnDesactivar = document.createElement("button");
            btnDesactivar.textContent = "Desactivar";
            btnDesactivar.className = "btn btn-sm btn-danger";
            btnDesactivar.onclick = () => desactivarCliente(c.idPersona);

            tdAcciones.appendChild(btnDesactivar);
            tr.append(tdDni, tdNombre, tdTel, tdAcciones);
            tbody.appendChild(tr);
        });

        // Aquí iría la lógica para dibujar los botones de paginación
    } catch (error) {
        mostrarError(error.message);
    }
}

async function desactivarCliente(id) {
    if (confirm("¿Seguro que deseas desactivar este cliente?")) {
        try {
            await API.delete(`api/clientes/${id}`);
            cargarClientes();
        } catch (error) {
            mostrarError(error.message);
        }
    }
}

function buscarClientes() {
    const texto = document.getElementById("txtBuscar").value;
    numeroPagina = 1;
    cargarClientes(texto);
}

function mostrarError(msj) {
    const div = document.getElementById("mensaje-error");
    div.textContent = msj;
    div.classList.remove("d-none");
    setTimeout(() => div.classList.add("d-none"), 4000);
}