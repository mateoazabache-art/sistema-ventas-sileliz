package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import pe.tecsup.sileliz.modelo.DetalleVenta;
import pe.tecsup.sileliz.modelo.Venta;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Acceso a datos de ventas (tablas venta y detalle_venta).
 * Los metodos de la transaccion reciben la Connection y NO la cierran:
 * la abre y cierra VentaService.
 */
public class VentaDao {

    /** serie, numero y fecha_hora los pone la base (DEFAULT 'B001', seq_correlativo y now()). */
    private static final String SQL_INSERT_VENTA =
            "INSERT INTO venta (id_cliente, id_usuario, base_imponible, descuento, igv, total, metodo_pago) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) "
            + "RETURNING id_venta, serie, numero, fecha_hora";

    private static final String SQL_INSERT_DETALLE =
            "INSERT INTO detalle_venta (id_venta, id_variante, descripcion, cantidad, precio_unitario, subtotal) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * Guarda la venta (con totales ya calculados) y le copia el id, la serie,
     * el numero y la fecha que asigno la base.
     */
    public void insertar(Connection cn, Venta v) {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT_VENTA)) {
            if (v.getIdCliente() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, v.getIdCliente());
            }
            ps.setInt(2, v.getIdUsuario());
            ps.setBigDecimal(3, v.getBaseImponible());
            ps.setBigDecimal(4, v.getDescuento());
            ps.setBigDecimal(5, v.getIgv());
            ps.setBigDecimal(6, v.getTotal());
            ps.setString(7, v.getMetodoPago().name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                v.setIdVenta(rs.getInt("id_venta"));
                v.setSerie(rs.getString("serie").trim());
                v.setNumero(rs.getInt("numero"));
                v.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /** Guarda una linea de la venta con su descripcion y precio del momento (RN-09). */
    public void insertarDetalle(Connection cn, int idVenta, DetalleVenta d) {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT_DETALLE)) {
            ps.setInt(1, idVenta);
            ps.setInt(2, d.getIdVariante());
            ps.setString(3, d.getDescripcion());
            ps.setInt(4, d.getCantidad());
            ps.setBigDecimal(5, d.getPrecioUnitario());
            ps.setBigDecimal(6, d.subtotal());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }
    
    /**
     * Pasa una venta de EMITIDA a ANULADA con su motivo (RN-08).
     * Devuelve false si la venta no existe o ya estaba anulada.
     */
    public boolean anular(Connection cn, int idVenta, String motivo) {
        String sql = "UPDATE venta SET estado = 'ANULADA', motivo_anulacion = ? "
                + "WHERE id_venta = ? AND estado = 'EMITIDA'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setInt(2, idVenta);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }
}
