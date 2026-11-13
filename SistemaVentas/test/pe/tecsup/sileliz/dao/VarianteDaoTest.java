
package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Conexion;

public class VarianteDaoTest {

    private final VarianteDao dao = new VarianteDao();
    private Connection cn;

    @Before
    public void preparar() throws SQLException {
        cn = Conexion.getConnection();
        cn.setAutoCommit(false);
    }

    @After
    public void deshacer() throws SQLException {
        if (cn != null) {
            try {
                cn.rollback();
            } finally {
                cn.close();
            }
        }
    }

    @Test
    public void buscarPorIdDevuelveVarianteYProducto() throws SQLException {
        int id = idPorSku("P-001-XL-BLA");

        Optional<VarianteProducto> resultado = dao.buscarPorId(cn, id);

        assertTrue(resultado.isPresent());
        assertEquals("P-001-XL-BLA", resultado.get().getSku());
        assertEquals("Blusa manga larga",
                resultado.get().getProducto().getNombre());
        assertEquals(new BigDecimal("59.90"),
                resultado.get().getProducto().getPrecioVenta());
    }

    @Test
    public void buscarPorTextoEncuentraPorSkuYNombre() throws SQLException {
        List<VarianteProducto> porSku =
                dao.buscarPorTexto(cn, "P-001-XL-BLA");

        List<VarianteProducto> porNombre =
                dao.buscarPorTexto(cn, "Blusa manga larga");

        assertEquals(1, porSku.size());
        assertEquals(3, porNombre.size());
    }

    @Test
    public void buscarPorTextoExcluyeInactivos() throws SQLException {
        String skuVariante = "P-001-XL-BLA";

        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE variante_producto SET activo = FALSE WHERE sku = ?")) {
            ps.setString(1, skuVariante);
            ps.executeUpdate();
        }

        assertTrue(dao.buscarPorTexto(cn, skuVariante).isEmpty());

        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE producto SET activo = FALSE WHERE codigo = ?")) {
            ps.setString(1, "P-001");
            ps.executeUpdate();
        }

        assertTrue(dao.buscarPorTexto(cn, "P-001-2XL-BLA").isEmpty());
    }

    @Test
    public void buscarPorTextoDevuelveMaximoVeinte() throws SQLException {
        List<VarianteProducto> resultado = dao.buscarPorTexto(cn, "P-");

        assertEquals(20, resultado.size());
    }

    @Test
    public void descontarStockCuandoHayExistencias() throws SQLException {
        int id = idPorSku("P-001-XL-BLA");

        assertTrue(dao.descontarStock(cn, id, 2));
        assertEquals(10, stockPorSku("P-001-XL-BLA"));
    }

    @Test
    public void descontarStockCuandoNoHayExistencias() throws SQLException {
        int id = idPorSku("P-003-XL-CEL");

        assertFalse(dao.descontarStock(cn, id, 1));
        assertEquals(0, stockPorSku("P-003-XL-CEL"));
    }

    @Test
    public void reponerStockAumentaLasExistencias() throws SQLException {
        int id = idPorSku("P-001-XL-BLA");

        dao.reponerStock(cn, id, 3);

        assertEquals(15, stockPorSku("P-001-XL-BLA"));
    }

    @Test(expected = SQLException.class)
    public void reponerStockConVarianteInexistenteLanzaExcepcion()
            throws SQLException {
        dao.reponerStock(cn, -1, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void descontarStockConCantidadCeroLanzaExcepcion()
            throws SQLException {
        dao.descontarStock(cn, 1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reponerStockConCantidadNegativaLanzaExcepcion()
            throws SQLException {
        dao.reponerStock(cn, 1, -2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void descontarStockConCantidadNegativaLanzaExcepcion()
            throws SQLException {
        dao.descontarStock(cn, 1, -2);
    }

    @Test
    public void buscarPorTextoConNullDevuelveListaVacia() throws SQLException {
        assertTrue(dao.buscarPorTexto(cn, null).isEmpty());
    }

    @Test
    public void buscarPorTextoVacioDevuelveListaVacia() throws SQLException {
        assertTrue(dao.buscarPorTexto(cn, "   ").isEmpty());
    }

    @Test
    public void buscarPorTextoSinCoincidenciasDevuelveListaVacia()
            throws SQLException {
        assertTrue(dao.buscarPorTexto(cn, "PRODUCTO-QUE-NO-EXISTE").isEmpty());
    }

    @Test
    public void buscarPorTextoMayorAlLimiteDevuelveListaVacia()
            throws SQLException {
        String texto = "A".repeat(101);

        assertTrue(dao.buscarPorTexto(cn, texto).isEmpty());
    }

    @Test
    public void buscarPorTextoEscapaComodines() throws SQLException {
        assertTrue(dao.buscarPorTexto(cn, "%").isEmpty());
        assertTrue(dao.buscarPorTexto(cn, "_").isEmpty());
    }

    @Test(expected = SQLException.class)
    public void reponerStockConDesbordamientoLanzaExcepcion()
            throws SQLException {
        int id = idPorSku("P-001-XL-BLA");

        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE variante_producto SET stock = 2147483647 WHERE id_variante = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        dao.reponerStock(cn, id, 1);
    }

    @Test
    public void insertarVarianteGuardaSusDatos() throws SQLException {
        VarianteProducto variante = nuevaVariante();

        VarianteProducto resultado = dao.insertar(cn, variante);

        assertTrue(resultado.getIdVariante() > 0);
        assertEquals("TEST-VAR-INSERTAR", resultado.getSku());
        assertEquals("4XL", resultado.getTalla());
        assertEquals("Prueba", resultado.getColor());
        assertEquals(0, resultado.getStock());
        assertEquals(2, resultado.getStockMinimo());
        assertTrue(resultado.isActivo());
    }

    @Test
    public void actualizarVarianteModificaSusDatos() throws SQLException {
        int id = idPorSku("P-001-XL-BLA");
        VarianteProducto variante = dao.buscarPorId(cn, id).get();

        variante.setTalla("5XL");
        variante.setColor("Prueba");
        variante.setSku("TEST-VAR-ACTUALIZAR");
        variante.setStockMinimo(4);

        assertTrue(dao.actualizar(cn, variante));

        VarianteProducto actualizada = dao.buscarPorId(cn, id).get();

        assertEquals("5XL", actualizada.getTalla());
        assertEquals("Prueba", actualizada.getColor());
        assertEquals("TEST-VAR-ACTUALIZAR", actualizada.getSku());
        assertEquals(4, actualizada.getStockMinimo());
    }

    @Test
    public void actualizarVarianteInexistenteDevuelveFalse()
            throws SQLException {
        VarianteProducto variante = nuevaVariante();
        variante.setIdVariante(999999);

        assertFalse(dao.actualizar(cn, variante));
    }

    @Test
    public void desactivarVarianteCambiaSuEstado() throws SQLException {
        int id = idPorSku("P-001-XL-BLA");

        assertTrue(dao.desactivar(cn, id));

        VarianteProducto variante = dao.buscarPorId(cn, id).get();

        assertFalse(variante.isActivo());
    }

    @Test
    public void desactivarVarianteInexistenteDevuelveFalse()
            throws SQLException {
        assertFalse(dao.desactivar(cn, 999999));
    }

    private VarianteProducto nuevaVariante() throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(idProductoPorCodigo("P-001"));

        VarianteProducto variante = new VarianteProducto();
        variante.setProducto(producto);
        variante.setTalla("4XL");
        variante.setColor("Prueba");
        variante.setSku("TEST-VAR-INSERTAR");
        variante.setStockMinimo(2);

        return variante;
    }

    private int idProductoPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT id_producto FROM producto WHERE codigo = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_producto");
                }
            }
        }

        throw new SQLException("No existe el producto de prueba: " + codigo);
    }

    private int idPorSku(String sku) throws SQLException {
        String sql = "SELECT id_variante FROM variante_producto WHERE sku = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, sku);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_variante");
                }
            }
        }

        throw new SQLException("No existe la variante de prueba: " + sku);
    }

    private int stockPorSku(String sku) throws SQLException {
        String sql = "SELECT stock FROM variante_producto WHERE sku = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, sku);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        }

        throw new SQLException("No existe la variante de prueba: " + sku);
    }
}
