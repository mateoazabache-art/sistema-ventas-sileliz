package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.Json;

/**
 * Prueba de humo (seccion 3.5): GET /api/salud
 *   {"estado":"ok","bd":"conectada","proyecto":"Sileliz","version":"v3.1.2"}
 * Si la base no responde, "bd" dice "desconectada" y el detalle va al log.
 */
@WebServlet(name = "ApiSaludServlet", urlPatterns = {"/api/salud"})
public class ApiSaludServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> salud = new LinkedHashMap<>();
        salud.put("estado", "ok");
        salud.put("bd", Conexion.probar() ? "conectada" : "desconectada");
        salud.put("proyecto", "Sileliz");
        salud.put("version", "v3.1.2");
        Json.responder(response, 200, salud);
    }
}
