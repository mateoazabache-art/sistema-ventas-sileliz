package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.filtro.SesionFilter;
import pe.tecsup.sileliz.util.Json;

/**
 * GET /api/sesion -> 200 con los datos del usuario conectado.
 * Si no hay sesion, SesionFilter ya respondio 401 antes de llegar aqui,
 * y api.js redirige a login.html (plan 6.1).
 * layout.js lo usa para armar el menu segun el rol.
 */
@WebServlet("/api/sesion")
public class SesionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Json.responder(resp, 200, SesionDto.de(SesionFilter.usuarioActual(req)));
    }
}
