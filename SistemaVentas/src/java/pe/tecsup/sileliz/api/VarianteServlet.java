package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.PersistenciaException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.util.Json;

/**
 * GET /api/variantes?texto=blusa  (todos los roles, contrato 3.7)
 *   200 [ {idVariante, talla, color, sku, stock, producto:{nombre, precioVenta}}, ... ]
 *   Hasta 20 resultados, solo variantes y productos ACTIVOS. Texto vacio -> [].
 *
 * Lo usa venta-nueva.html para buscar la prenda (plan B 9.7: lo hizo Luz porque la venta
 * lo necesitaba; Naomi le agrega despues el POST/PUT/DELETE del catalogo en esta misma clase).
 * Es solo lectura y sin reglas de negocio: llama directo al DAO, igual que EstadisticaServlet.
 * La busqueda escapa los comodines % y _ y usa PreparedStatement (VarianteDao).
 */
@WebServlet("/api/variantes/*")
public class VarianteServlet extends HttpServlet {

    private final VarianteDao dao = new VarianteDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.responder(resp, 200, dao.buscarPorTexto(req.getParameter("texto")));
        } catch (SilelizException e) {
            Json.error(resp, e);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Json.error(resp, new PersistenciaException("Error interno del servidor."));
        }
    }
}
