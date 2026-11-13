
package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.dao.ProductoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.servicio.VarianteService;
import pe.tecsup.sileliz.util.Json;

@WebServlet("/api/variantes/*")
public class VarianteServlet extends HttpServlet {

    private final VarianteService servicio =
            new VarianteService(new VarianteDao(), new ProductoDao());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            Json.responder(resp, 200,
                    servicio.buscarPorTexto(req.getParameter("texto")));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            int id = obtenerId(req);
            VarianteProducto variante = Json.leer(req, VarianteProducto.class);
            variante.setIdVariante(id);

            servicio.actualizar(variante);

            Json.responder(resp, 200,
                    java.util.Map.of("mensaje", "Variante actualizada correctamente."));
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID de la URL debe ser un numero entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            int id = obtenerId(req);
            servicio.desactivar(id);

            Json.responder(resp, 200,
                    java.util.Map.of("mensaje", "Variante desactivada correctamente."));
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID de la URL debe ser un numero entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    private int obtenerId(HttpServletRequest req) {
        String path = req.getPathInfo();

        if (path == null || !path.matches("/\\d+")) {
            throw new NumberFormatException();
        }

        return Integer.parseInt(path.substring(1));
    }
}
