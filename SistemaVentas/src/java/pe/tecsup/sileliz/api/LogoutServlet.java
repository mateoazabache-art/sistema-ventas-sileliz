package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import pe.tecsup.sileliz.util.Json;

/**
 * POST /api/logout -> 200 {"mensaje":"Sesión cerrada."}
 * Responde 200 aunque no hubiera sesion: cerrar dos veces no es un error.
 */
@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sesion = req.getSession(false);   // false: no crear una si no hay
        if (sesion != null) {
            sesion.invalidate();
        }
        Json.responder(resp, 200, Map.of("mensaje", "Sesión cerrada."));
    }
}
