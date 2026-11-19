package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.dao.MovimientoDao;
import pe.tecsup.sileliz.dao.UsuarioDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.IngresoRegistrado;
import pe.tecsup.sileliz.modelo.IngresoSolicitud;
import pe.tecsup.sileliz.modelo.MovimientoStock;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.util.Conexion;

/**
 * Pruebas de IngresoService contra la base real, SIN dejar datos:
 * el servicio recibe una conexion "de prueba" en la que commit() y close() no hacen nada,
 * y en @After se hace rollback de todo lo que la prueba cambio.
 *
 * Usa P-004-2XL-NEG (stock 12 en 02_datos_prueba.sql), que no usan las demas pruebas.
 */
public class IngresoServiceTest {

    private static final String SKU = "P-004-2XL-NEG";

    private Connection cn;              // conexion real de la prueba
    private Usuario admin;
    private Usuario vendedor;

    @Before
    public void abrir() throws SQLException {
        cn = Conexion.getConnection();
        cn.setAutoCommit(false);
        UsuarioDao usuarios = new UsuarioDao();
        admin = usuarios.buscarPorNombreUsuario("admin").get();
        vendedor = usuarios.buscarPorNombreUsuario("vendedor").get();
    }

    @After
    public void deshacer() throws SQLException {
        cn.rollback();                  // nada de lo que hizo la prueba queda en la base
        cn.close();
    }

    /** Servicio que trabaja dentro de la transaccion de la prueba. */
    private IngresoService servicio(MovimientoDao movimientos) {
        return new IngresoService(new VarianteDao(), movimientos, this::conexionDePrueba);
    }

    private IngresoService servicio() {
        return servicio(new MovimientoDao());
    }

    /** La misma conexion, pero commit() y close() no hacen nada (el rollback de @After manda). */
    private Connection conexionDePrueba() {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {Connection.class}, (p, m, args) -> {
                    if (m.getName().equals("commit") || m.getName().equals("close")) {
                        return null;
                    }
                    return m.invoke(cn, args);
                });
    }

    // ---------------- Caso feliz ----------------

    @Test
    public void ingresoSubeElStockYLoAnotaEnElKardex() throws SQLException {
        int id = idPorSku(SKU);
        int antes = stock(id);

        IngresoRegistrado r = servicio().registrar(
                new IngresoSolicitud(id, 12, "  Compra a Gamarra, guía 0123  "), admin);

        assertEquals(antes + 12, stock(id));
        assertEquals(antes + 12, r.stock());          // la respuesta trae el stock real
        assertEquals(SKU, r.sku());
        assertEquals(12, r.cantidad());

        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT tipo, cantidad, id_venta, id_usuario, motivo FROM movimiento_stock "
                + "WHERE id_variante = ? ORDER BY id_movimiento DESC LIMIT 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("INGRESO", rs.getString("tipo"));
                assertEquals(12, rs.getInt("cantidad"));
                assertNull(rs.getObject("id_venta"));
                assertEquals(admin.getIdPersona(), rs.getInt("id_usuario"));
                assertEquals("Compra a Gamarra, guía 0123", rs.getString("motivo"));   // sin espacios
            }
        }
    }

    // ---------------- Todo o nada ----------------

    @Test
    public void siFallaElKardexElStockNoSube() throws SQLException {
        int id = idPorSku(SKU);
        int antes = stock(id);
        MovimientoDao kardexQueFalla = new MovimientoDao() {
            @Override
            public void insertar(Connection c, MovimientoStock m) {
                throw new IllegalStateException("Simulación: se cayó la base al anotar el kárdex");
            }
        };

        try {
            servicio(kardexQueFalla).registrar(new IngresoSolicitud(id, 5, "Compra"), admin);
            fail("Debió fallar");
        } catch (IllegalStateException e) {
            // esperado
        }
        assertEquals("El rollback debe deshacer la suma del stock", antes, stock(id));
    }

    // ---------------- Permisos y validaciones ----------------

    @Test
    public void elVendedorNoRegistraIngresos() throws SQLException {
        int id = idPorSku(SKU);
        esperar(403, () -> servicio().registrar(new IngresoSolicitud(id, 5, "Compra"), vendedor));
        esperar(403, () -> servicio().registrar(new IngresoSolicitud(id, 5, "Compra"), null));
    }

    @Test
    public void cantidadFueraDeRangoEs400() throws SQLException {
        int id = idPorSku(SKU);
        int antes = stock(id);
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, null, "Compra"), admin));
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 0, "Compra"), admin));
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, -5, "Compra"), admin));
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 1000, "Compra"), admin));
        assertEquals(antes, stock(id));
        servicio().registrar(new IngresoSolicitud(id, IngresoService.CANTIDAD_MAXIMA, "Compra grande"), admin);
        assertEquals(antes + IngresoService.CANTIDAD_MAXIMA, stock(id));
    }

    @Test
    public void motivoVacioOMuyLargoEs400() throws SQLException {
        int id = idPorSku(SKU);
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 5, null), admin));
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 5, "   "), admin));
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 5, "x".repeat(201)), admin));
        esperar(400, () -> servicio().registrar(null, admin));
    }

    @Test
    public void varianteInexistenteEs404() {
        esperar(404, () -> servicio().registrar(new IngresoSolicitud(999999, 5, "Compra"), admin));
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(0, 5, "Compra"), admin));
    }

    @Test
    public void prendaDesactivadaEs400() throws SQLException {
        int id = idPorSku(SKU);
        int antes = stock(id);
        ejecutar("UPDATE variante_producto SET activo = FALSE WHERE id_variante = " + id);
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 5, "Compra"), admin));

        ejecutar("UPDATE variante_producto SET activo = TRUE WHERE id_variante = " + id);
        ejecutar("UPDATE producto SET activo = FALSE WHERE id_producto = "
                + "(SELECT id_producto FROM variante_producto WHERE id_variante = " + id + ")");
        esperar(400, () -> servicio().registrar(new IngresoSolicitud(id, 5, "Compra"), admin));
        assertEquals(antes, stock(id));
    }

    // ---------------- Ayudantes ----------------

    private void esperar(int codigo, Runnable accion) {
        try {
            accion.run();
            fail("Debió responder " + codigo);
        } catch (SilelizException e) {
            assertEquals(e.getMessage(), codigo, e.getCodigoHttp());
        }
    }

    private int idPorSku(String sku) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_variante FROM variante_producto WHERE sku = ?")) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Falta " + sku + " en 02_datos_prueba.sql", rs.next());
                return rs.getInt(1);
            }
        }
    }

    private int stock(int idVariante) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT stock FROM variante_producto WHERE id_variante = ?")) {
            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void ejecutar(String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
