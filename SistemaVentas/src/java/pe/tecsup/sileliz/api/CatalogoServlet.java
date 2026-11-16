package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import pe.tecsup.sileliz.util.Json;
import pe.tecsup.sileliz.util.Validador;

@WebServlet("/api/catalogos/tallas")
public class CatalogoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Json.responder(resp, 200, Validador.obtenerTallasPermitidas());
    }
}
