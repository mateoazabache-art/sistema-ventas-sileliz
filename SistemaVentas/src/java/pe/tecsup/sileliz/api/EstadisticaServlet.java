package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.dao.EstadisticaDao;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.util.Json;

/**
 * GET /api/estadisticas/hoy  (todos los roles, contrato 3.7)
 *   200 {"fecha":"2026-11-11","ventas":3,"total":629.10,"anuladas":0,"clientes":10,"stockBajo":2}
 *   401 sin sesion (lo responde SesionFilter antes de llegar aqui)
 *   500 {"error": "..."} si falla la base
 *
 * Lo usa index.html (tarea 10 de Cesia) con API.estadisticas.hoy() de api.js.
 * Es solo lectura y no tiene reglas de negocio, por eso llama directo al DAO
 * (no hace falta un Service que solo repita la llamada).
 */
@WebServlet("/api/estadisticas/hoy")
public class EstadisticaServlet extends HttpServlet {

    private final EstadisticaDao estadisticas = new EstadisticaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Json.responder(resp, 200, estadisticas.resumenHoy());
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }
}
