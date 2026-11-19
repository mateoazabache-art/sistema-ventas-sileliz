
package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.Paginado;

public class ProductoDaoTest {

    private final ProductoDao dao = new ProductoDao();
    private final String token = UUID.randomUUID().toString().substring(0, 8);
    private final String prefijo = "TST-" + token + "-";
    private int idCategoria;
    private int idProducto;
    private String skuVariante;

    @Before
    public void preparar() throws SQLException {
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO categoria (nombre) VALUES (?) RETURNING id_categoria")) {
            ps.setString(1, "Test " + token);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                idCategoria = rs.getInt(1);
            }
        }

        Producto producto = crearProducto(prefijo + "01", "Producto prueba " + token);
        idProducto = dao.insertar(producto).getIdProducto();

        skuVariante = "SKU-" + token;
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO variante_producto "
                        + "(id_producto, talla, color, sku, stock, stock_minimo) "
                        + "VALUES (?, 'M', 'Negro', ?, 0, 2)")) {
            ps.setInt(1, idProducto);
            ps.setString(2, skuVariante);
            ps.executeUpdate();
        }
    }

    @After
    public void limpiar() throws SQLException {
        if (idCategoria == 0) {
            return;
        }

        try (Connection cn = Conexion.getConnection()) {
            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM variante_producto "
                    + "WHERE id_producto IN "
                    + "(SELECT id_producto FROM producto WHERE codigo LIKE ?)")) {
                ps.setString(1, prefijo + "%");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM producto WHERE codigo LIKE ?")) {
                ps.setString(1, prefijo + "%");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "DELETE FROM categoria WHERE id_categoria = ?")) {
                ps.setInt(1, idCategoria);
                ps.executeUpdate();
            }
        }
    }

    @Test
    public void listarPaginadoFiltraPorTextoYDevuelveTotal() {
        Paginado<Producto> resultado =
                dao.listarPaginado(1, 10, token, idCategoria);

        assertEquals(1, resultado.total());
        assertEquals(1, resultado.items().size());
        assertEquals(prefijo + "01", resultado.items().get(0).getCodigo());
    }

    @Test
    public void listarPaginadoRespetaLaPaginaSolicitada() {
        Paginado<Producto> resultado =
                dao.listarPaginado(2, 1, token, idCategoria);

        assertTrue(resultado.items().isEmpty());
        assertEquals(1, resultado.total());
        assertEquals(2, resultado.pagina());
    }

    @Test
    public void contarDevuelveCantidadConFiltros() {
        assertEquals(1L, dao.contar(token, idCategoria));
        assertEquals(0L, dao.contar("texto inexistente", idCategoria));
    }

    @Test
    public void buscarPorIdDevuelveProductoYCategoria() {
        Optional<Producto> resultado = dao.buscarPorId(idProducto);

        assertTrue(resultado.isPresent());
        assertEquals(prefijo + "01", resultado.get().getCodigo());
        assertEquals(idCategoria,
                resultado.get().getCategoria().getIdCategoria());
    }

    @Test
    public void buscarPorIdInexistenteDevuelveVacio() {
        assertFalse(dao.buscarPorId(-1).isPresent());
    }

    @Test
    public void insertarDevuelveProductoConIdGenerado() {
        Producto nuevo = crearProducto(prefijo + "02", "Producto nuevo " + token);

        Producto resultado = dao.insertar(nuevo);

        assertTrue(resultado.getIdProducto() > 0);
        assertEquals(prefijo + "02", resultado.getCodigo());
        assertTrue(dao.buscarPorId(resultado.getIdProducto()).isPresent());
    }

    @Test
    public void actualizarModificaLosDatosDelProducto() {
        Producto producto = dao.buscarPorId(idProducto).get();
        producto.setNombre("Producto actualizado " + token);
        producto.setDescripcion("Descripcion de prueba");
        producto.setPrecioVenta(new BigDecimal("79.90"));

        assertTrue(dao.actualizar(producto));

        Producto actualizado = dao.buscarPorId(idProducto).get();
        assertEquals("Producto actualizado " + token, actualizado.getNombre());
        assertEquals(new BigDecimal("79.90"), actualizado.getPrecioVenta());
    }

    @Test
    public void desactivarCambiaElEstadoDelProducto() {
        assertTrue(dao.desactivar(idProducto));

        Producto producto = dao.buscarPorId(idProducto).get();
        assertFalse(producto.isActivo());
    }

    @Test
    public void listarStockBajoIncluyeVariantesBajoElMinimo() {
        List<VarianteProducto> resultado = dao.listarStockBajo();

        VarianteProducto variante = resultado.stream()
                .filter(v -> skuVariante.equals(v.getSku()))
                .findFirst()
                .orElse(null);

        assertNotNull(variante);
        assertTrue(variante.estaBajoMinimo());
        assertEquals(0, variante.getStock());
    }

    private Producto crearProducto(String codigo, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(idCategoria);

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setDescripcion("Producto temporal de prueba");
        producto.setCategoria(categoria);
        producto.setPrecioVenta(new BigDecimal("49.90"));
        producto.setActivo(true);
        return producto;
    }
}
