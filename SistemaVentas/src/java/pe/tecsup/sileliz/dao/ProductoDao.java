package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;

/**
 * Acceso a datos de la tabla producto.
 * Cada metodo publico abre y cierra su propia conexion.
 */
public class ProductoDao {

    private static final int TAM_POR_DEFECTO = 10;
    private static final int TAM_MAXIMO = Paginado.TAM_MAXIMO;
    private static final int MAX_TEXTO = 100;

    private static final String SELECT_PRODUCTO
            = "SELECT p.id_producto, p.codigo, p.nombre, p.descripcion, "
            + "p.id_categoria, p.precio_venta, p.activo, "
            + "c.nombre AS nombre_categoria "
            + "FROM producto p "
            + "JOIN categoria c ON c.id_categoria = p.id_categoria ";

    private static final String FILTRO
            = "WHERE (p.nombre ILIKE ? ESCAPE '\\' OR p.codigo ILIKE ? ESCAPE '\\') "
            + "AND (? <= 0 OR p.id_categoria = ?) ";

    private static final String SQL_CONTAR
            = "SELECT COUNT(*) FROM producto p " + FILTRO;

    private static final String SQL_LISTAR
            = SELECT_PRODUCTO + FILTRO
            + "ORDER BY p.nombre, p.id_producto LIMIT ? OFFSET ?";

    private static final String SQL_POR_ID
            = SELECT_PRODUCTO + "WHERE p.id_producto = ?";

    private static final String SQL_INSERTAR
            = "INSERT INTO producto "
            + "(codigo, nombre, descripcion, id_categoria, precio_venta, activo) "
            + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_producto";

    private static final String SQL_ACTUALIZAR
            = "UPDATE producto SET codigo = ?, nombre = ?, descripcion = ?, "
            + "id_categoria = ?, precio_venta = ? WHERE id_producto = ?";

    private static final String SQL_DESACTIVAR
            = "UPDATE producto SET activo = FALSE WHERE id_producto = ?";

    private static final String SQL_STOCK_BAJO
            = "SELECT b.id_variante, b.sku, b.talla, b.color, "
            + "b.stock, b.stock_minimo, "
            + "p.id_producto, p.codigo, p.nombre, p.precio_venta, "
            + "c.id_categoria, c.nombre AS nombre_categoria "
            + "FROM v_stock_bajo b "
            + "JOIN producto p ON p.id_producto = b.id_producto "
            + "JOIN categoria c ON c.id_categoria = p.id_categoria "
            + "ORDER BY b.producto, b.sku";

    /**
     * Lista productos (activos e inactivos) por nombre o codigo.
     * idCategoria menor o igual a 0 significa "sin filtro de categoria".
     * La pagina empieza en 1.
     */
    public Paginado<Producto> listarPaginado(int pagina, int tam, String texto, int idCategoria) {
        int paginaValida = Math.max(pagina, 1);
        int tamValido = tam < 1 ? TAM_POR_DEFECTO : Math.min(tam, TAM_MAXIMO);
        String patron = crearPatron(texto);
        if (patron == null) {
            return Paginado.de(new ArrayList<>(), paginaValida, tamValido, 0L);
        }
        try (Connection cn = Conexion.getConnection()) {
            long total = contar(cn, patron, idCategoria);
            List<Producto> items = listar(cn, patron, idCategoria, paginaValida, tamValido);
            return Paginado.de(items, paginaValida, tamValido, total);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /** Cuenta los productos que coinciden con los mismos filtros de listarPaginado. */
    public long contar(String texto, int idCategoria) {
        String patron = crearPatron(texto);
        if (patron == null) {
            return 0L;
        }
        try (Connection cn = Conexion.getConnection()) {
            return contar(cn, patron, idCategoria);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    public Optional<Producto> buscarPorId(int idProducto) {
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /** Inserta el producto, asigna el id generado al mismo objeto y lo devuelve. */
    public Producto insertar(Producto producto) {
        validarProducto(producto);
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion());
            ps.setInt(4, producto.getCategoria().getIdCategoria());
            ps.setBigDecimal(5, producto.getPrecioVenta());
            ps.setBoolean(6, producto.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    producto.setIdProducto(rs.getInt(1));
                    return producto;
                }
            }
            throw new IllegalStateException("No se obtuvo el id generado del producto.");
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /** Actualiza codigo, nombre, descripcion, categoria y precio. El estado solo cambia con desactivar. */
    public boolean actualizar(Producto producto) {
        validarProducto(producto);
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion());
            ps.setInt(4, producto.getCategoria().getIdCategoria());
            ps.setBigDecimal(5, producto.getPrecioVenta());
            ps.setInt(6, producto.getIdProducto());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /** Desactivacion logica: nunca elimina la fila. */
    public boolean desactivar(int idProducto) {
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(SQL_DESACTIVAR)) {
            ps.setInt(1, idProducto);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * Lista las variantes activas cuyo stock esta en el minimo o por debajo.
     */
    public List<VarianteProducto> listarStockBajo() {
        List<VarianteProducto> resultado = new ArrayList<>();

        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(SQL_STOCK_BAJO);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.add(mapearVariante(rs));
            }
            return resultado;

        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    private long contar(Connection cn, String patron, int idCategoria) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_CONTAR)) {
            enlazarFiltros(ps, patron, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private List<Producto> listar(Connection cn, String patron, int idCategoria,
            int pagina, int tam) throws SQLException {
        List<Producto> resultado = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SQL_LISTAR)) {
            int siguiente = enlazarFiltros(ps, patron, idCategoria) + 1;
            ps.setInt(siguiente, tam);
            ps.setLong(siguiente + 1, (long) (pagina - 1) * tam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }
        return resultado;
    }

    private static int enlazarFiltros(PreparedStatement ps, String patron, int idCategoria)
            throws SQLException {
        ps.setString(1, patron);
        ps.setString(2, patron);
        ps.setInt(3, idCategoria);
        ps.setInt(4, idCategoria);
        return 4;
    }

    private static void validarProducto(Producto producto) {
        Objects.requireNonNull(producto, "El producto no puede ser nulo.");
        if (producto.getCategoria() == null) {
            throw new IllegalArgumentException("El producto debe tener una categoria.");
        }
    }

    private static String crearPatron(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() > MAX_TEXTO) {
            return null;
        }
        String escapado = limpio.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }

    private static Producto mapear(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getInt("id_categoria"));
        categoria.setNombre(rs.getString("nombre_categoria"));

        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCodigo(rs.getString("codigo"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setCategoria(categoria);
        producto.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        producto.setActivo(rs.getBoolean("activo"));
        return producto;
    }

    private static VarianteProducto mapearVariante(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getInt("id_categoria"));
        categoria.setNombre(rs.getString("nombre_categoria"));
        categoria.setActivo(true);

        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCodigo(rs.getString("codigo"));
        producto.setNombre(rs.getString("nombre"));
        producto.setCategoria(categoria);
        producto.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        producto.setActivo(true);

        VarianteProducto variante = new VarianteProducto();
        variante.setIdVariante(rs.getInt("id_variante"));
        variante.setProducto(producto);
        variante.setSku(rs.getString("sku"));
        variante.setTalla(rs.getString("talla"));
        variante.setColor(rs.getString("color"));
        variante.setStock(rs.getInt("stock"));
        variante.setStockMinimo(rs.getInt("stock_minimo"));
        variante.setActivo(true);

        return variante;
    }
}
