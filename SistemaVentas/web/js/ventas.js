(async function () {
  let paginaActual = 1;
  let esAdmin = false;

  const tbody = document.getElementById("tbodyVentas");
  const mensaje = document.getElementById("mensaje");
  const infoPagina = document.getElementById("infoPagina");
  const btnAnterior = document.getElementById("btnAnterior");
  const btnSiguiente = document.getElementById("btnSiguiente");

  function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = "alert alert-" + tipo;
  }

  function ocultarMensaje() {
    mensaje.className = "alert d-none";
  }

  function celda(texto, clase) {
    const td = document.createElement("td");
    td.textContent = texto;
    if (clase) td.className = clase;
    return td;
  }

  function dibujarFila(v) {
    const tr = document.createElement("tr");
    const fecha = String(v.fechaHora).replace("T", " ").substring(0, 16);

    tr.appendChild(celda(v.comprobante));
    tr.appendChild(celda(fecha));
    tr.appendChild(celda(v.cliente));
    tr.appendChild(celda(v.vendedor));
    tr.appendChild(celda(v.metodoPago));
    tr.appendChild(celda("S/ " + Number(v.total).toFixed(2), "text-end"));
    tr.appendChild(celda(v.estado,
      v.estado === "ANULADA" ? "estado-anulada" : "estado-emitida"));

    const tdAcc = document.createElement("td");

    const ver = document.createElement("a");
    ver.className = "btn btn-sm btn-outline-primary me-1";
    ver.textContent = "Ver";
    ver.href = "comprobante.html?id=" + encodeURIComponent(v.idVenta);
    tdAcc.appendChild(ver);

    if (esAdmin && v.estado === "EMITIDA") {
      const anular = document.createElement("button");
      anular.type = "button";
      anular.className = "btn btn-sm btn-outline-danger";
      anular.textContent = "Anular";
      anular.addEventListener("click", () => anularVenta(v));
      tdAcc.appendChild(anular);
    }

    tr.appendChild(tdAcc);
    return tr;
  }

  async function cargar(pagina) {
    ocultarMensaje();
    const desde = document.getElementById("desde").value;
    const hasta = document.getElementById("hasta").value;

    let ruta = "api/ventas?pagina=" + encodeURIComponent(pagina) + "&tam=10";
    if (desde) ruta += "&desde=" + encodeURIComponent(desde);
    if (hasta) ruta += "&hasta=" + encodeURIComponent(hasta);

    try {
      const datos = await API.get(ruta);
      paginaActual = datos.pagina;

      tbody.replaceChildren();
      if (datos.items.length === 0) {
        const tr = document.createElement("tr");
        const td = celda("No hay ventas para mostrar.", "text-center");
        td.colSpan = 8;
        tr.appendChild(td);
        tbody.appendChild(tr);
      } else {
        datos.items.forEach(v => tbody.appendChild(dibujarFila(v)));
      }

      const totalPaginas = Math.max(datos.paginas, 1);
      infoPagina.textContent = "Página " + datos.pagina + " de " + totalPaginas;
      btnAnterior.disabled = datos.pagina <= 1;
      btnSiguiente.disabled = datos.pagina >= totalPaginas;
    } catch (e) {
      mostrarMensaje(e.message, "danger");
    }
  }

  async function anularVenta(v) {
    const motivo = prompt("Motivo de la anulación de " + v.comprobante + ":");
    if (motivo === null) return;
    if (motivo.trim() === "") {
      mostrarMensaje("El motivo es obligatorio.", "warning");
      return;
    }
    if (motivo.length > 200) {
      mostrarMensaje("El motivo no puede pasar de 200 caracteres.", "warning");
      return;
    }
    try {
      await API.post("api/ventas/" + encodeURIComponent(v.idVenta) + "/anular",
                     { motivo: motivo.trim() });
      await cargar(paginaActual);
      mostrarMensaje("Venta " + v.comprobante + " anulada correctamente.", "success");
    } catch (e) {
      mostrarMensaje(e.message, "danger");
    }
  }

  document.getElementById("btnBuscar").addEventListener("click", () => cargar(1));
  btnAnterior.addEventListener("click", () => cargar(paginaActual - 1));
  btnSiguiente.addEventListener("click", () => cargar(paginaActual + 1));

  try {
    const sesion = await API.auth.obtenerSesion();
    esAdmin = sesion && sesion.rol === "ADMINISTRADOR";
  } catch (e) {
    return;
  }
  cargar(1);
})();


