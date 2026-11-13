package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.PersistenciaException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.filtro.SesionFilter;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.Venta;
import pe.tecsup.sileliz.modelo.VentaSolicitud;
import pe.tecsup.sileliz.servicio.VentaService;
import pe.tecsup.sileliz.util.Json;

/**
 * API de ventas (plan 3.7). Solo lee JSON, llama al servicio y responde JSON.
 *   POST /api/ventas       -> 201 con la venta (idVenta, serie, numero, totales)
 *   GET  /api/ventas/{id}  -> 200 con el comprobante (venta + detalle + nombres)
 */
@WebServlet("/api/ventas/*")
public class VentaServlet extends HttpServlet {

    private final VentaService servicio = new VentaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pathInfo = req.getPathInfo();          // "/115" -> venta 115
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/ventas (historial paginado) va en el siguiente paso.
                throw new DatoInvalidoException("Falta el número de venta en la URL.");
            }
            int id = Integer.parseInt(pathInfo.substring(1));
            Usuario u = SesionFilter.usuarioActual(req);  // para ver si puede mirar esta venta
            Json.responder(resp, 200, servicio.buscarPorId(id, u));

        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException("El número de venta de la URL no es válido."));
        } catch (SilelizException e) {
            Json.error(resp, e);              // 401, 403, 404
        } catch (RuntimeException e) {
            e.printStackTrace();
            Json.error(resp, new PersistenciaException("Error interno del servidor."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // UTF-8 al leer el cuerpo (va ANTES de Json.leer). Json.responder ya responde en UTF-8.
        req.setCharacterEncoding("UTF-8");
        try {
            // PASO 1: leer el carrito. JSON vacio o mal formado -> DatoInvalidoException (400).
            VentaSolicitud solicitud = Json.leer(req, VentaSolicitud.class);

            // PASO 2: el vendedor sale de la SESION, nunca del JSON.
            // Si no hay sesion, usuarioActual LANZA AutenticacionException (401): nunca devuelve null.
            Usuario u = SesionFilter.usuarioActual(req);

            // PASO 3: toda la logica de negocio vive en el servicio, no aqui.
            Venta v = servicio.registrar(solicitud, u.getIdPersona());

            // PASO 4: 201 Created con la venta.
            Json.responder(resp, 201, v);

        } catch (SilelizException e) {
            // Errores previstos: 400, 401, 404, 409... con {"error": "..."}
            Json.error(resp, e);

        } catch (RuntimeException e) {
            // Error NO previsto: 500 en JSON (no la pagina HTML de Tomcat), sin detalles internos.
            // Va DESPUES del de SilelizException, que tambien es RuntimeException.
            e.printStackTrace();
            Json.error(resp, new PersistenciaException("Error interno del servidor."));
        }
    }
}
