package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.Cliente;
import pe.tecsup.sileliz.servicio.ClienteService;
import pe.tecsup.sileliz.util.Json;

@WebServlet("/api/clientes/*")
public class ClienteServlet extends HttpServlet {

    private final ClienteService servicio = new ClienteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                String texto = req.getParameter("texto");
                String paginaStr = req.getParameter("pagina");
                String tamStr = req.getParameter("tam");
                
                int pagina = (paginaStr != null) ? Integer.parseInt(paginaStr) : 1;
                int tam = (tamStr != null) ? Integer.parseInt(tamStr) : 10;
                
                Json.responder(resp, 200, servicio.listar(pagina, tam, texto));
            } else if (pathInfo.startsWith("/dni/")) {
                String dni = pathInfo.substring(5);
                Json.responder(resp, 200, servicio.buscarPorDni(dni));
            } else {
                int id = Integer.parseInt(pathInfo.substring(1));
                Json.responder(resp, 200, servicio.buscarPorId(id));
            }
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException("Formato de número inválido en la URL."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Cliente cliente = Json.leer(req, Cliente.class);
            Cliente registrado = servicio.registrar(cliente);
            Json.responder(resp, 201, registrado);
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                throw new DatoInvalidoException("Falta el ID del cliente en la URL.");
            }
            int id = Integer.parseInt(pathInfo.substring(1));
            Cliente cliente = Json.leer(req, Cliente.class);
            Cliente actualizado = servicio.actualizar(id, cliente);
            Json.responder(resp, 200, actualizado);
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException("El ID de la URL debe ser un número entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                throw new DatoInvalidoException("Falta el ID del cliente en la URL.");
            }
            int id = Integer.parseInt(pathInfo.substring(1));
            servicio.desactivar(id);
            Json.responder(resp, 200, Map.of("mensaje", "Cliente desactivado."));
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException("El ID de la URL debe ser un número entero."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }
}