package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.servicio.ReporteService;
import pe.tecsup.sileliz.util.Json;

/**
 * Reportes (solo ADMINISTRADOR, contrato 3.7). Parametros comunes: desde y hasta (AAAA-MM-DD).
 *
 *   GET /api/reportes/ventas?desde=&hasta=&pagina=&tam=   lista paginada + total acumulado
 *   GET /api/reportes/mas-vendidos?desde=&hasta=&limite=  [{"etiqueta","cantidad","monto"}]
 *   GET /api/reportes/por-vendedor?desde=&hasta=          [{"etiqueta","cantidad","monto"}]
 *
 *   400 fechas o numeros mal escritos (ReporteService)
 *   401 sin sesion y 403 si es vendedor (SesionFilter, antes de llegar aqui)
 *   404 reporte que no existe
 *
 * El Servlet solo elige el reporte segun la direccion, pasa los parametros
 * tal como llegan y responde JSON. Las validaciones estan en ReporteService.
 */
@WebServlet("/api/reportes/*")
public class ReporteServlet extends HttpServlet {

    private final ReporteService reportes = new ReporteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String desde = req.getParameter("desde");
        String hasta = req.getParameter("hasta");
        String reporte = req.getPathInfo() == null ? "" : req.getPathInfo();
        try {
            switch (reporte) {
                case "/ventas" -> Json.responder(resp, 200,
                        reportes.ventas(desde, hasta, req.getParameter("pagina"), req.getParameter("tam")));
                case "/mas-vendidos" -> Json.responder(resp, 200,
                        reportes.masVendidos(desde, hasta, req.getParameter("limite")));
                case "/por-vendedor" -> Json.responder(resp, 200,
                        reportes.porVendedor(desde, hasta));
                default -> Json.responder(resp, 404, Map.of("error", "El reporte solicitado no existe."));
            }
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }
}
