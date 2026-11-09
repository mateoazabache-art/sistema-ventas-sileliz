
document.addEventListener("DOMContentLoaded", () => {

    const formulario = document.getElementById("form-producto");
    const tablaVariantes = document.getElementById("tabla-variantes");
    const botonAgregar = document.getElementById("btn-agregar-variante");
    const selectorCategoria = document.getElementById("categoria");
    const filtroCategoria = document.getElementById("filtro-categoria");

    async function cargarCategorias() {
        try {
            const respuesta = await fetch("mock/categorias.json");

            if (!respuesta.ok) {
                throw new Error("No se pudieron cargar las categorías.");
            }

            const categorias = await respuesta.json();
            const categoriasActivas = categorias.filter(categoria => categoria.activo);

            selectorCategoria.replaceChildren(
                new Option("Seleccionar categoría", "")
            );

            filtroCategoria.replaceChildren(
                new Option("Todas las categorías", "")
            );

            categoriasActivas.forEach(categoria => {
                selectorCategoria.add(
                    new Option(categoria.nombre, categoria.idCategoria)
                );

                filtroCategoria.add(
                    new Option(categoria.nombre, categoria.idCategoria)
                );
            });

        } catch (error) {
            console.error("Error al cargar categorías:", error);
            selectorCategoria.replaceChildren(
                new Option("Error al cargar categorías", "")
            );
            filtroCategoria.replaceChildren(
                new Option("Error al cargar categorías", "")
            );
        }
    }

    cargarCategorias();
    formulario.addEventListener("submit", (evento) => {
        evento.preventDefault();

        const codigo = document.getElementById("codigo").value.trim();
        const nombre = document.getElementById("nombre").value.trim();
        const descripcion = document.getElementById("descripcion").value.trim();
        const categoria = document.getElementById("categoria").value;
        const precio = document.getElementById("precio").value;

        let mensaje = "";

        // Validar datos del producto
        if (!Validaciones.esObligatorio(codigo)) {
            mensaje = "Ingresa el código del producto.";
        } else if (!Validaciones.cumpleLongitudMaxima(codigo, 20)) {
            mensaje = "El código no debe superar los 20 caracteres.";
        } else if (!Validaciones.esObligatorio(nombre)) {
            mensaje = "Ingresa el nombre del producto.";
        } else if (!Validaciones.cumpleLongitudMaxima(nombre, 100)) {
            mensaje = "El nombre no debe superar los 100 caracteres.";
        } else if (!Validaciones.cumpleLongitudMaxima(descripcion, 250)) {
            mensaje = "La descripción no debe superar los 250 caracteres.";
        } else if (!Validaciones.esObligatorio(categoria)) {
            mensaje = "Selecciona una categoría.";
        } else if (!Validaciones.esNumeroPositivo(precio)) {
            mensaje = "El precio debe ser un número mayor que cero.";
        }

        // Obtener las filas de variantes
        const filas = tablaVariantes.querySelectorAll("tr");

        if (mensaje === "" && filas.length === 0) {
            mensaje = "Agrega al menos una variante al producto.";
        }

        const skus = new Set();
        const combinaciones = new Set();

        // Validar cada variante
        filas.forEach((fila, indice) => {

            if (mensaje !== "") {
                return;
            }

            const talla = fila.querySelector('[name="talla"]').value.trim();
            const color = fila.querySelector('[name="color"]').value.trim();
            const sku = fila.querySelector('[name="sku"]').value.trim();
            const stock = fila.querySelector('[name="stock"]').value;
            const stockMinimo = fila.querySelector('[name="stock-minimo"]').value;

            const numero = indice + 1;

            if (!Validaciones.esObligatorio(talla)) {
                mensaje = `Ingresa la talla de la variante ${numero}.`;
            } else if (!Validaciones.cumpleLongitudMaxima(talla, 10)) {
                mensaje = `La talla de la variante ${numero} no debe superar los 10 caracteres.`;
            } else if (!Validaciones.esObligatorio(color)) {
                mensaje = `Ingresa el color de la variante ${numero}.`;
            } else if (!Validaciones.cumpleLongitudMaxima(color, 30)) {
                mensaje = `El color de la variante ${numero} no debe superar los 30 caracteres.`;
            } else if (!Validaciones.esObligatorio(sku)) {
                mensaje = `Ingresa el SKU de la variante ${numero}.`;
            } else if (!Validaciones.cumpleLongitudMaxima(sku, 30)) {
                mensaje = `El SKU de la variante ${numero} no debe superar los 30 caracteres.`;
            } else if (!Validaciones.esNumeroNoNegativo(stock)) {
                mensaje = `El stock de la variante ${numero} debe ser un entero igual o mayor que cero.`;
            } else if (!Validaciones.esNumeroNoNegativo(stockMinimo)) {
                mensaje = `El stock mínimo de la variante ${numero} debe ser un entero igual o mayor que cero.`;
            }

            const skuNormalizado = sku.toUpperCase();
            const combinacion = talla.toUpperCase() + "-" + color.toUpperCase();

            if (mensaje === "" && skus.has(skuNormalizado)) {
                mensaje = `El SKU ${sku} está repetido.`;
            } else if (mensaje === "" && combinaciones.has(combinacion)) {
                mensaje = `La combinación de talla ${talla} y color ${color} está repetida.`;
            }

            skus.add(skuNormalizado);
            combinaciones.add(combinacion);
        });

        if (mensaje !== "") {
            alert(mensaje);
            return;
        }

        alert("Los datos del producto y sus variantes son válidos. El formulario está listo para conectarse con la API.");
    });

    // Agregar una variante
    botonAgregar.addEventListener("click", () => {

        const fila = document.createElement("tr");

        fila.innerHTML = `
            <td>
                <input type="text" name="talla" placeholder="Ej. XL" maxlength="10" required>
            </td>
            <td>
                <input type="text" name="color" placeholder="Ej. Negro" maxlength="30" required>
            </td>
            <td>
                <input type="text" name="sku" placeholder="Ej. POL-001-XL" maxlength="30" required>
            </td>
            <td>
                <input type="number" name="stock" min="0" step="1" value="0" required>
            </td>
            <td>
                <input type="number" name="stock-minimo" min="0" step="1" value="2" required>
            </td>
            <td>
                <button type="button" class="btn btn-secondary btn-eliminar">
                    Eliminar
                </button>
            </td>
        `;

        tablaVariantes.appendChild(fila);
    });

    // Eliminar una variante
    tablaVariantes.addEventListener("click", (evento) => {

        if (evento.target.classList.contains("btn-eliminar")) {
            evento.target.closest("tr").remove();
        }

    });

});
