package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Acceso a datos de variante_producto.
 * Las consultas abren su propia conexion.
 * Los metodos de stock usan la conexion de la transaccion de venta.
 */
public class VarianteDao {

    private static final int MAX_RESULTADOS = 20;
    private static final int MAX_TEXTO = 100;

    private static final String SQL_POR_ID
            = "SELECT v.id_variante, v.id_producto, v.talla, v.color, v.sku, "
            + "v.stock, v.stock_minimo, v.activo, "
            + "p.nombre, p.precio_venta, p.activo AS producto_activo "
            + "FROM variante_producto v "
            + "JOIN producto p ON p.id_producto = v.id_producto "
            + "WHERE v.id_variante = ?";

    private static final String SQL_POR_TEXTO
            = "SELECT v.id_variante, v.id_producto, v.talla, v.color, v.sku, "
            + "v.stock, v.stock_minimo, v.activo, "
            + "p.nombre, p.precio_venta, p.activo AS producto_activo "
            + "FROM variante_producto v "
            + "JOIN producto p ON p.id_producto = v.id_producto "
            + "WHERE v.activo = TRUE AND p.activo = TRUE "
            + "AND (v.sku ILIKE ? ESCAPE '\\' OR p.nombre ILIKE ? ESCAPE '\\') "
            + "ORDER BY p.nombre, v.sku "
            + "LIMIT ?";

    private static final String SQL_DESCONTAR
            = "UPDATE variante_producto "
            + "SET stock = stock - ? "
            + "WHERE id_variante = ? AND stock >= ?";

    private static final String SQL_REPONER
            = "UPDATE variante_producto "
            + "SET stock = stock + ? "
            + "WHERE id_variante = ?";

    public Optional<VarianteProducto> buscarPorId(int idVariante) {
        try (Connection cn = Conexion.getConnection()) {
            return buscarPorId(cn, idVariante);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    public List<VarianteProducto> buscarPorTexto(String texto) {
        try (Connection cn = Conexion.getConnection()) {
            return buscarPorTexto(cn, texto);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    Optional<VarianteProducto> buscarPorId(Connection cn, int idVariante)
            throws SQLException {

        try (PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, idVariante);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        }
    }

    List<VarianteProducto> buscarPorTexto(Connection cn, String texto)
            throws SQLException {

        List<VarianteProducto> resultado = new ArrayList<>();

        if (texto == null) {
            return resultado;
        }

        String limpio = texto.trim();

        if (limpio.isEmpty() || limpio.length() > MAX_TEXTO) {
            return resultado;
        }

        String patron = "%" + escaparComodines(limpio) + "%";

        try (PreparedStatement ps = cn.prepareStatement(SQL_POR_TEXTO)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setInt(3, MAX_RESULTADOS);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        }

        return resultado;
    }

    public boolean descontarStock(Connection cn, int idVariante, int cantidad)
            throws SQLException {

        validarCantidad(cantidad);

        try (PreparedStatement ps = cn.prepareStatement(SQL_DESCONTAR)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idVariante);
            ps.setInt(3, cantidad);

            return ps.executeUpdate() == 1;
        }
    }

    public void reponerStock(Connection cn, int idVariante, int cantidad)
            throws SQLException {

        validarCantidad(cantidad);

        try (PreparedStatement ps = cn.prepareStatement(SQL_REPONER)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idVariante);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No existe la variante que se intenta reponer."
                );
            }
        }
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que 0."
            );
        }
    }

    private static String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private VarianteProducto mapear(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        producto.setActivo(rs.getBoolean("producto_activo"));

        VarianteProducto variante = new VarianteProducto();
        variante.setIdVariante(rs.getInt("id_variante"));
        variante.setProducto(producto);
        variante.setTalla(rs.getString("talla"));
        variante.setColor(rs.getString("color"));
        variante.setSku(rs.getString("sku"));
        variante.setStock(rs.getInt("stock"));
        variante.setStockMinimo(rs.getInt("stock_minimo"));
        variante.setActivo(rs.getBoolean("activo"));

        return variante;
    }
}
