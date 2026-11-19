package pe.tecsup.sileliz.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.filtro.SesionFilter;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.UsuarioSolicitud;
import pe.tecsup.sileliz.servicio.UsuarioService;
import pe.tecsup.sileliz.util.Json;

/**
 * /api/usuarios (CU-02, contrato 3.7). Solo ADMINISTRADOR (SesionFilter y UsuarioService).
 *
 *   GET    /api/usuarios?pagina=&tam=&texto=   lista paginada
 *   GET    /api/usuarios/{id}                  un usuario
 *   POST   /api/usuarios                       crear (con clave inicial)   -> 201
 *   PUT    /api/usuarios/{id}                  editar datos, usuario y rol
 *   DELETE /api/usuarios/{id}                  desactivar (no borra)
 *   POST   /api/usuarios/{id}/clave            restablecer clave {"clave": "..."}
 *   POST   /api/usuarios/{id}/activar          volver a activar
 *
 * Como todo Servlet del proyecto: lee el JSON, llama al servicio y responde.
 * El hash de la clave nunca sale en el JSON (Usuario.claveHash es transient).
 */
@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends HttpServlet {

    private final UsuarioService servicio = new UsuarioService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Usuario actual = SesionFilter.usuarioActual(req);
            String[] partes = partes(req);
            if (partes.length == 0) {
                int pagina = entero(req.getParameter("pagina"), 1);
                int tam = entero(req.getParameter("tam"), 10);
                Json.responder(resp, 200, servicio.listar(pagina, tam, req.getParameter("texto"), actual));
            } else if (partes.length == 1) {
                Json.responder(resp, 200, servicio.buscarPorId(id(partes[0]), actual));
            } else {
                throw new NoEncontradoException("La ruta no existe.");
            }
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Usuario actual = SesionFilter.usuarioActual(req);
            String[] partes = partes(req);
            if (partes.length == 0) {
                UsuarioSolicitud s = Json.leer(req, UsuarioSolicitud.class);
                Json.responder(resp, 201, servicio.registrar(s, actual));
            } else if (partes.length == 2 && partes[1].equals("clave")) {
                UsuarioSolicitud s = Json.leer(req, UsuarioSolicitud.class);
                servicio.restablecerClave(id(partes[0]), s.getClave(), actual);
                Json.responder(resp, 200, Map.of("mensaje", "Clave restablecida. El usuario ya puede ingresar con la clave nueva."));
            } else if (partes.length == 2 && partes[1].equals("activar")) {
                servicio.activar(id(partes[0]), actual);
                Json.responder(resp, 200, Map.of("mensaje", "Usuario activado."));
            } else {
                throw new NoEncontradoException("La ruta no existe.");
            }
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Usuario actual = SesionFilter.usuarioActual(req);
            String[] partes = partes(req);
            if (partes.length != 1) {
                throw new DatoInvalidoException("Falta el id del usuario en la URL.");
            }
            UsuarioSolicitud s = Json.leer(req, UsuarioSolicitud.class);
            Json.responder(resp, 200, servicio.actualizar(id(partes[0]), s, actual));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Usuario actual = SesionFilter.usuarioActual(req);
            String[] partes = partes(req);
            if (partes.length != 1) {
                throw new DatoInvalidoException("Falta el id del usuario en la URL.");
            }
            servicio.desactivar(id(partes[0]), actual);
            Json.responder(resp, 200, Map.of("mensaje", "Usuario desactivado."));
        } catch (SilelizException e) {
            Json.error(resp, e);
        }
    }

    // ------------------------------------------------------------------
    // Ayudantes de la URL (sin reglas de negocio)
    // ------------------------------------------------------------------

    /** "/5/clave" -> ["5", "clave"]; null o "/" -> []. */
    private static String[] partes(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) {
            return new String[0];
        }
        return info.substring(1).split("/");
    }

    private static int id(String texto) {
        if (!texto.matches("\\d{1,9}")) {
            throw new DatoInvalidoException("El id del usuario debe ser un número entero.");
        }
        return Integer.parseInt(texto);
    }

    private static int entero(String texto, int porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        if (!texto.trim().matches("\\d{1,6}")) {
            throw new DatoInvalidoException("Los parámetros pagina y tam deben ser números enteros.");
        }
        return Integer.parseInt(texto.trim());
    }
}
