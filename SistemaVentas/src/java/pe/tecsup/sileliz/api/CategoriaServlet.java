package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.servicio.CategoriaService;
import pe.tecsup.sileliz.util.Json;

@WebServlet("/api/categorias/*")
public class CategoriaServlet extends HttpServlet {

    private final CategoriaService servicio =
            new CategoriaService(new CategoriaDao());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                int pagina = obtenerParametro(req, "pagina", 1);
                int tam = obtenerParametro(req, "tam", 10);
                String texto = req.getParameter("texto");
                Boolean activo = obtenerEstado(req);

                Json.responder(resp, 200,
                        servicio.listarPaginado(pagina, tam, texto, activo));
                return;
            }

            int id = obtenerId(pathInfo);
            Json.responder(resp, 200, servicio.obtenerPorId(id));

        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "Los parámetros numéricos de la consulta no son válidos."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo != null && !pathInfo.equals("/")) {
                throw new DatoInvalidoException(
                        "La ruta para registrar la categoría no es válida.");
            }

            Categoria categoria = Json.leer(req, Categoria.class);
            Categoria registrada = servicio.registrar(categoria);

            Json.responder(resp, 201, registrada);

        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                throw new DatoInvalidoException(
                        "Falta el ID de la categoría en la URL.");
            }

            int id = obtenerId(pathInfo);
            Categoria categoria = Json.leer(req, Categoria.class);
            categoria.setIdCategoria(id);

            servicio.actualizar(categoria);

            Json.responder(resp, 200,
                    Map.of("mensaje", "Categoría actualizada correctamente."));

        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID de la categoría debe ser un número entero válido."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                throw new DatoInvalidoException(
                        "Falta el ID de la categoría en la URL.");
            }

            int id = obtenerId(pathInfo);
            servicio.desactivar(id);

            Json.responder(resp, 200,
                    Map.of("mensaje", "Categoría desactivada correctamente."));

        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "El ID de la categoría debe ser un número entero válido."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    private int obtenerId(String pathInfo) {
        if (!pathInfo.matches("/\\d+")) {
            throw new DatoInvalidoException(
                    "La ruta de la categoría no es válida.");
        }

        return Integer.parseInt(pathInfo.substring(1));
    }

    private int obtenerParametro(
            HttpServletRequest req, String nombre, int defecto) {

        String valor = req.getParameter(nombre);

        if (valor == null || valor.isBlank()) {
            return defecto;
        }

        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException(
                    "El parámetro " + nombre + " debe ser un número entero.");
        }
    }

    private Boolean obtenerEstado(HttpServletRequest req) {
        String valor = req.getParameter("activo");

        if (valor == null || valor.isBlank()) {
            return null;
        }

        if ("true".equalsIgnoreCase(valor)) {
            return true;
        }

        if ("false".equalsIgnoreCase(valor)) {
            return false;
        }

        throw new DatoInvalidoException(
                "El parámetro activo debe ser true o false.");
    }
}
