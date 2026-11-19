
package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.dao.ProductoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.servicio.ProductoService;
import pe.tecsup.sileliz.servicio.VarianteService;
import pe.tecsup.sileliz.util.Json;

@WebServlet("/api/productos/*")
public class ProductoServlet extends HttpServlet {

    private final ProductoService servicio =
            new ProductoService(new ProductoDao(), new CategoriaDao());

    private final VarianteService varianteServicio =
            new VarianteService(new VarianteDao(), new ProductoDao());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                String stockBajo = req.getParameter("stockBajo");

                if ("true".equalsIgnoreCase(stockBajo)) {
                    Json.responder(resp, 200, servicio.listarStockBajo());
                    return;
                }

                int pagina = obtenerParametro(req, "pagina", 1);
                int tam = obtenerParametro(req, "tam", 10);
                int idCategoria = obtenerParametro(req, "idCategoria", 0);
                String texto = req.getParameter("texto");

                Json.responder(resp, 200,
                        servicio.listarPaginado(pagina, tam, texto, idCategoria));
            } else if (pathInfo.matches("/\\d+/variantes")) {
                int idProducto = Integer.parseInt(pathInfo.split("/")[1]);
                Json.responder(resp, 200, varianteServicio.listarPorProducto(idProducto));
            } else {
                int id = Integer.parseInt(pathInfo.substring(1));
                Json.responder(resp, 200, servicio.obtenerPorId(id));
            }
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "Los parámetros numéricos deben ser números enteros."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                Producto producto = Json.leer(req, Producto.class);
                Producto registrado = servicio.registrar(producto);
                Json.responder(resp, 201, registrado);
                return;
            }

            if (!pathInfo.matches("/\\d+/variantes")) {
                throw new DatoInvalidoException(
                        "La ruta para registrar la variante no es válida.");
            }

            int idProducto = Integer.parseInt(
                    pathInfo.split("/")[1]);

            VarianteProducto variante =
                    Json.leer(req, VarianteProducto.class);

            Producto producto = new Producto();
            producto.setIdProducto(idProducto);
            variante.setProducto(producto);

            VarianteProducto registrada =
                    varianteServicio.registrar(variante);

            Json.responder(resp, 201, registrada);

        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID de la URL debe ser un número entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                throw new DatoInvalidoException(
                        "Falta el ID del producto en la URL.");
            }

            int id = Integer.parseInt(pathInfo.substring(1));
            Producto producto = Json.leer(req, Producto.class);
            producto.setIdProducto(id);

            servicio.actualizar(producto);
            Json.responder(resp, 200,
                    Map.of("mensaje", "Producto actualizado correctamente."));
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID del producto debe ser un número entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                throw new DatoInvalidoException(
                        "Falta el ID del producto en la URL.");
            }

            int id = Integer.parseInt(pathInfo.substring(1));
            servicio.desactivar(id);

            Json.responder(resp, 200,
                    Map.of("mensaje", "Producto desactivado correctamente."));
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID del producto debe ser un número entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    private int obtenerParametro(
            HttpServletRequest req, String nombre, int defecto) {
        String valor = req.getParameter(nombre);
        return valor == null || valor.isBlank()
                ? defecto
                : Integer.parseInt(valor);
    }
}
