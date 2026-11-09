package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.filtro.SesionFilter;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.servicio.AuthService;
import pe.tecsup.sileliz.util.Json;

/**
 * POST /api/login  con  {"usuario":"admin","clave":"Admin2026"}
 *   200 {"id":..,"nombre":..,"usuario":..,"rol":..,"rolEtiqueta":..}
 *   401 Usuario o clave incorrectos  |  423 Cuenta bloqueada
 *
 * El Servlet solo hace 3 cosas (plan 5.6): leer el JSON, llamar al servicio
 * y responder JSON. Toda la logica del login esta en AuthService.
 */
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private final AuthService auth = new AuthService();

    /** Lo que llega del navegador. */
    private static class Credenciales {
        String usuario;
        String clave;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Credenciales c = Json.leer(req, Credenciales.class);
            char[] clave = c.clave == null ? null : c.clave.toCharArray();
            Usuario u = auth.iniciarSesion(c.usuario, clave);

            HttpSession sesion = req.getSession(true);
            req.changeSessionId();      // nuevo id de sesion al ingresar (evita fijacion de sesion)
            u.setClaveHash(null);       // en la sesion nunca se guarda el hash
            sesion.setAttribute(SesionFilter.ATRIBUTO_USUARIO, u);

            Json.responder(resp, 200, SesionDto.de(u));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }
}
