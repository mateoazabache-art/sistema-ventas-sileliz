(async function () {
  let paginaActual = 1;
  let esAdmin = false;

  const tbody = document.getElementById("tbodyVentas");
  const mensaje = document.getElementById("mensaje");
  const infoPagina = document.getElementById("infoPagina");
  const paginacion = document.getElementById("paginacion");

  // Los métodos de pago llegan como EFECTIVO, YAPE...; en pantalla se ven con formato normal.
  const NOMBRE_PAGO = {
    EFECTIVO: "Efectivo",
    YAPE: "Yape",
    PLIN: "Plin",
    TARJETA: "Tarjeta",
    TRANSFERENCIA: "Transferencia"
  };

  // Estas dos columnas se ocultan en celular (en la tabla también: d-none d-md-table-cell).
  const SOLO_PANTALLA_GRANDE = "d-none d-md-table-cell";

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

  // 2026-11-14T10:05:00 -> 14/11/2026 10:05
  function fechaLegible(iso) {
    const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(iso));
    if (!m) return String(iso);
    return m[3] + "/" + m[2] + "/" + m[1] + " " + m[4] + ":" + m[5];
  }

  function nombreDelPago(codigo) {
    return NOMBRE_PAGO[codigo] || String(codigo);
  }

  function dibujarFila(v) {
    const tr = document.createElement("tr");

    tr.appendChild(celda(v.comprobante));
    tr.appendChild(celda(fechaLegible(v.fechaHora)));
    tr.appendChild(celda(v.cliente));
    tr.appendChild(celda(v.vendedor, SOLO_PANTALLA_GRANDE));
    tr.appendChild(celda(nombreDelPago(v.metodoPago), SOLO_PANTALLA_GRANDE));
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

  function itemPagina(texto, destino, activo, desactivado) {
    const li = document.createElement("li");
    li.className = "page-item" + (activo ? " active" : "") + (desactivado ? " disabled" : "");
    if (activo) li.setAttribute("aria-current", "page");
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "page-link";
    boton.textContent = texto;
    if (desactivado) {
      boton.disabled = true;
    } else {
      boton.addEventListener("click", () => cargar(destino));
    }
    li.appendChild(boton);
    return li;
  }

  // Anterior, hasta 5 números alrededor de la página actual y Siguiente.
  function dibujarPaginacion(pagina, totalPaginas) {
    paginacion.replaceChildren();
    paginacion.appendChild(itemPagina("Anterior", pagina - 1, false, pagina <= 1));
    let inicio = Math.max(1, pagina - 2);
    const fin = Math.min(totalPaginas, inicio + 4);
    inicio = Math.max(1, fin - 4);
    for (let n = inicio; n <= fin; n++) {
      paginacion.appendChild(itemPagina(String(n), n, n === pagina, false));
    }
    paginacion.appendChild(itemPagina("Siguiente", pagina + 1, false, pagina >= totalPaginas));
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
      if (!datos) return;   // sesión vencida: api.js ya mandó al login

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
      infoPagina.textContent = "Página " + datos.pagina + " de " + totalPaginas
        + " (" + datos.total + " ventas)";
      dibujarPaginacion(datos.pagina, totalPaginas);
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

  try {
    const sesion = await API.auth.obtenerSesion();
    esAdmin = sesion && sesion.rol === "ADMINISTRADOR";
  } catch (e) {
    return;
  }
  cargar(1);
})();

