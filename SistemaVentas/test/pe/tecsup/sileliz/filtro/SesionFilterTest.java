package pe.tecsup.sileliz.filtro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import org.junit.Test;

/**
 * Reglas de permisos del SesionFilter (CP-04 y contrato 3.7).
 * true = solo el ADMINISTRADOR puede.
 */
public class SesionFilterTest {

    @Test
    public void usuariosYReportesSoloAdministrador() {
        assertTrue(SesionFilter.requiereAdministrador("/api/usuarios", "GET"));
        assertTrue(SesionFilter.requiereAdministrador("/api/usuarios/3", "PUT"));
        assertTrue(SesionFilter.requiereAdministrador("/api/reportes/ventas", "GET"));
    }

    @Test
    public void catalogoLoLeenTodosPeroSoloEscribeElAdministrador() {
        assertFalse(SesionFilter.requiereAdministrador("/api/productos", "GET"));
        assertFalse(SesionFilter.requiereAdministrador("/api/categorias", "GET"));
        assertTrue(SesionFilter.requiereAdministrador("/api/productos", "POST"));
        assertTrue(SesionFilter.requiereAdministrador("/api/productos/5", "PUT"));
        assertTrue(SesionFilter.requiereAdministrador("/api/variantes/9", "DELETE"));
    }

    @Test
    public void elVendedorPuedeVenderYAtenderClientes() {
        assertFalse(SesionFilter.requiereAdministrador("/api/ventas", "POST"));
        assertFalse(SesionFilter.requiereAdministrador("/api/ventas/15", "GET"));
        assertFalse(SesionFilter.requiereAdministrador("/api/clientes", "POST"));
        assertFalse(SesionFilter.requiereAdministrador("/api/clientes/dni/70000001", "GET"));
        assertFalse(SesionFilter.requiereAdministrador("/api/sesion", "GET"));
    }

    @Test
    public void soloElAdministradorDesactivaClientes() {
        assertTrue(SesionFilter.requiereAdministrador("/api/clientes/5", "DELETE"));
        // crear, editar y consultar siguen permitidos al vendedor
        assertFalse(SesionFilter.requiereAdministrador("/api/clientes/5", "PUT"));
        assertFalse(SesionFilter.requiereAdministrador("/api/clientes", "GET"));
    }

    @Test
    public void soloElAdministradorAnulaVentas() {
        assertTrue(SesionFilter.requiereAdministrador("/api/ventas/15/anular", "POST"));
    }

    @Test
    public void unaRutaParecidaNoSeConfunde() {
        // "/api/usuariosX" no es "/api/usuarios"
        assertFalse(SesionFilter.requiereAdministrador("/api/usuariosX", "GET"));
    }

    // ---- Red de seguridad: errores no previstos (plan 6, "Errores que revelan datos") ----

    @Test
    public void unErrorInesperadoResponde500EnJsonSinLaTraza() throws Exception {
        RespuestaFalsa r = new RespuestaFalsa(false);
        FilterChain falla = (req, resp) -> {
            throw new IllegalStateException("detalle interno que el usuario no debe ver");
        };

        SesionFilter.continuar(falla, peticion("POST", "/api/clientes"), r.proxy());

        assertEquals(500, r.estado);
        assertTrue(r.tipo.startsWith("application/json"));
        assertTrue(r.cuerpo.toString().contains(SesionFilter.MENSAJE_INESPERADO));
        assertFalse(r.cuerpo.toString().contains("IllegalStateException"));
        assertFalse(r.cuerpo.toString().contains("detalle interno"));
    }

    @Test
    public void sinErrorElFiltroNoTocaLaRespuesta() throws Exception {
        RespuestaFalsa r = new RespuestaFalsa(false);
        FilterChain bien = (req, resp) -> { };

        SesionFilter.continuar(bien, peticion("GET", "/api/clientes"), r.proxy());

        assertEquals(-1, r.estado);
        assertEquals("", r.cuerpo.toString());
    }

    @Test
    public void siLaRespuestaYaSalioNoSeEscribeEncima() throws Exception {
        RespuestaFalsa r = new RespuestaFalsa(true);     // ya se enviaron datos al navegador
        FilterChain falla = (req, resp) -> {
            throw new IllegalArgumentException("x");
        };

        SesionFilter.continuar(falla, peticion("GET", "/api/ventas"), r.proxy());

        assertEquals(-1, r.estado);
        assertEquals("", r.cuerpo.toString());
    }

    /** Peticion de mentira: solo responde metodo y ruta. */
    private static HttpServletRequest peticion(String metodo, String ruta) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                SesionFilterTest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (p, m, args) -> switch (m.getName()) {
                    case "getMethod" -> metodo;
                    case "getServletPath" -> ruta;
                    default -> null;
                });
    }

    /** Respuesta de mentira que anota lo que el filtro escribe. */
    private static final class RespuestaFalsa {
        final boolean yaEnviada;
        int estado = -1;
        String tipo = "";
        final StringWriter cuerpo = new StringWriter();

        RespuestaFalsa(boolean yaEnviada) {
            this.yaEnviada = yaEnviada;
        }

        HttpServletResponse proxy() {
            PrintWriter escritor = new PrintWriter(cuerpo, true);
            return (HttpServletResponse) Proxy.newProxyInstance(
                    SesionFilterTest.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (p, m, args) -> {
                        switch (m.getName()) {
                            case "isCommitted": return yaEnviada;
                            case "setStatus": estado = (Integer) args[0]; return null;
                            case "setContentType": tipo = (String) args[0]; return null;
                            case "getWriter": return escritor;
                            default: return null;
                        }
                    });
        }
    }
}
