package pe.tecsup.sileliz.filtro;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
