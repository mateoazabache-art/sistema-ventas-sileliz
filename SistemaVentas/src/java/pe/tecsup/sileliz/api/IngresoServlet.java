package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.filtro.SesionFilter;
import pe.tecsup.sileliz.modelo.IngresoSolicitud;
import pe.tecsup.sileliz.servicio.IngresoService;
import pe.tecsup.sileliz.util.Json;

/**
 * POST /api/ingresos  (solo ADMINISTRADOR; el filtro responde 401/403 antes de llegar aqui)
 *   cuerpo:  {"idVariante": 11, "cantidad": 12, "motivo": "Compra a Gamarra, guia 0123"}
 *   201      {"idVariante": 11, "sku": "P-004-2XL-NEG", "cantidad": 12, "stock": 20}
 *   400 datos invalidos · 404 variante inexistente
 *
 * El servlet solo hace tres cosas: leer el JSON, llamar al servicio y responder JSON.
 */
@WebServlet("/api/ingresos")
public class IngresoServlet extends HttpServlet {

    private final IngresoService servicio = new IngresoService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        try {
            IngresoSolicitud solicitud = Json.leer(req, IngresoSolicitud.class);
            Json.responder(resp, 201, servicio.registrar(solicitud, SesionFilter.usuarioActual(req)));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }
}
