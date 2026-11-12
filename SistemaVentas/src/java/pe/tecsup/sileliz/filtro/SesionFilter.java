package pe.tecsup.sileliz.filtro;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pe.tecsup.sileliz.excepcion.AccesoDenegadoException;
import pe.tecsup.sileliz.excepcion.AutenticacionException;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.util.Json;

/**
 * Portero de toda la API (plan, seccion 6). Revisa cada peticion a /api/*
 * ANTES de que llegue al Servlet, en este orden:
 *
 *   1. Cabeceras de seguridad (clickjacking, tipo MIME, no guardar en cache).
 *   2. POST/PUT/DELETE sin Content-Type application/json -> 415 (CSRF sencillo, CP-16).
 *   3. Rutas publicas (/api/login, /api/logout, /api/salud): pasan sin sesion.
 *   4. Sin sesion -> 401 (api.js lleva a login.html).
 *   5. Rol sin permiso -> 403 (CP-04).
 *
 * Los Servlets ya no tienen que revisar nada de esto: si el codigo llega a
 * ellos, hay sesion y el rol tiene permiso.
 */
@WebFilter("/api/*")
public class SesionFilter implements Filter {

    /** Nombre del atributo de sesion donde LoginServlet guarda el Usuario. */
    public static final String ATRIBUTO_USUARIO = "usuario";

    /** Rutas que no necesitan sesion. */
    private static final Set<String> PUBLICAS = Set.of("/api/login", "/api/logout", "/api/salud");

    /** Metodos que modifican datos. */
    private static final Set<String> ESCRITURA = Set.of("POST", "PUT", "DELETE");

    /** Solo el ADMINISTRADOR, con cualquier metodo (contrato 3.7). */
    private static final List<String> SOLO_ADMIN = List.of("/api/usuarios", "/api/reportes");

    /** Todos leen (GET); solo el ADMINISTRADOR escribe (contrato 3.7). */
    private static final List<String> ADMIN_ESCRIBE = List.of(
            "/api/categorias", "/api/productos", "/api/variantes");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 1. Cabeceras de seguridad en TODAS las respuestas de la API
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "no-store");

        String ruta = rutaDe(req);
        String metodo = req.getMethod();

        // 2. Solo JSON en las escrituras
        if (ESCRITURA.contains(metodo) && !esJson(req.getContentType())) {
            Json.responder(resp, 415, Map.of("error", "La petición debe enviarse en formato JSON."));
            return;
        }

        // 3. Rutas publicas
        if (PUBLICAS.contains(ruta)) {
            chain.doFilter(request, response);
            return;
        }

        // 4. Hay sesion?
        Usuario usuario = usuarioDeSesion(req);
        if (usuario == null) {
            Json.error(resp, new AutenticacionException("Inicie sesión para continuar."));
            return;
        }

        // 5. El rol tiene permiso?
        if (requiereAdministrador(ruta, metodo) && !usuario.esAdministrador()) {
            Json.error(resp, new AccesoDenegadoException());
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Usuario conectado. Usar en los Servlets protegidos, por ejemplo en VentaServlet:
     *   Usuario vendedor = SesionFilter.usuarioActual(req);
     */
    public static Usuario usuarioActual(HttpServletRequest req) {
        Usuario u = usuarioDeSesion(req);
        if (u == null) {
            throw new AutenticacionException("Inicie sesión para continuar.");
        }
        return u;
    }

    /** Reglas de permisos por ruta. Si se agrega un modulo, se agrega aqui. */
    static boolean requiereAdministrador(String ruta, String metodo) {
        if (empiezaCon(ruta, SOLO_ADMIN)) {
            return true;
        }
        if (empiezaCon(ruta, ADMIN_ESCRIBE) && ESCRITURA.contains(metodo)) {
            return true;
        }
        // Desactivar un cliente: solo el administrador (plan 2.1).
        // El vendedor si puede crear y editar clientes (POST y PUT).
        if (empiezaCon(ruta, List.of("/api/clientes")) && "DELETE".equals(metodo)) {
            return true;
        }
        // Anular una venta: solo el administrador (RN-08)
        return ruta.startsWith("/api/ventas/") && ruta.endsWith("/anular");
    }

    /**
     * Ruta dentro de la aplicacion, sin "/sileliz" ni parametros: "/api/salud".
     * getServletPath + getPathInfo es la forma estandar: Tomcat ya la entrega
     * decodificada y sin el context path.
     */
    static String rutaDe(HttpServletRequest req) {
        String servletPath = req.getServletPath() == null ? "" : req.getServletPath();
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        String ruta = servletPath + pathInfo;
        // sin "/" final: "/api/salud/" es lo mismo que "/api/salud"
        while (ruta.length() > 1 && ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        return ruta;
    }

    private static Usuario usuarioDeSesion(HttpServletRequest req) {
        HttpSession sesion = req.getSession(false);   // false: no crear sesiones vacias
        if (sesion == null) {
            return null;
        }
        Object u = sesion.getAttribute(ATRIBUTO_USUARIO);
        return (u instanceof Usuario) ? (Usuario) u : null;
    }

    /** "/api/productos" coincide con "/api/productos" y "/api/productos/5", no con "/api/productosX". */
    private static boolean empiezaCon(String ruta, List<String> prefijos) {
        for (String p : prefijos) {
            if (ruta.equals(p) || ruta.startsWith(p + "/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean esJson(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("application/json");
    }
}
