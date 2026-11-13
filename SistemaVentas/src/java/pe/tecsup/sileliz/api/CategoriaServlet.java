package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.util.Json;

@WebServlet("/api/categorias")
public class CategoriaServlet extends HttpServlet {

    private final CategoriaDao categoriaDao = new CategoriaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            int pagina = obtenerParametro(req, "pagina", 1);
            int tam = obtenerParametro(req, "tam", 10);
            String texto = req.getParameter("texto");

            Json.responder(resp, 200,
                    categoriaDao.listarPaginado(pagina, tam, texto));
        } catch (NumberFormatException e) {
            Json.error(resp, new DatoInvalidoException(
                    "Los parámetros numéricos deben ser números enteros."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    private int obtenerParametro(HttpServletRequest req, String nombre, int defecto) {
        String valor = req.getParameter(nombre);
        return valor == null || valor.isBlank()
                ? defecto
                : Integer.parseInt(valor);
    }
}
