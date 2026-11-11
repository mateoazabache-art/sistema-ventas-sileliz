package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.tecsup.sileliz.modelo.DetalleVenta;
import pe.tecsup.sileliz.modelo.EstadoVenta;
import pe.tecsup.sileliz.modelo.MetodoPago;
import pe.tecsup.sileliz.modelo.Venta;
import pe.tecsup.sileliz.modelo.VentaResumen;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;

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

    /**
     * Una pagina del historial, la mas reciente primero.
     * desde y hasta (ambos incluidos) pueden ser nulos. Si idUsuario no es nulo,
     * solo se listan las ventas de ese usuario (el vendedor ve las suyas).
     */
    public Paginado<VentaResumen> listarPaginado(int pagina, int tam, LocalDate desde,
                                                 LocalDate hasta, Integer idUsuario) {
        int t = Paginado.normalizarTam(tam);
        int p = Math.max(pagina, 1);

        StringBuilder filtro = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (desde != null) {
            filtro.append(" AND v.fecha_hora >= ?");
            params.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (hasta != null) {
            filtro.append(" AND v.fecha_hora < ?");
            params.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        }
        if (idUsuario != null) {
            filtro.append(" AND v.id_usuario = ?");
            params.add(idUsuario);
        }

        String sql = "SELECT v.id_venta, v.serie, v.numero, v.fecha_hora, v.total, v.metodo_pago, v.estado, "
                + "COALESCE(pc.nombres || ' ' || pc.apellidos, 'Varios') AS cliente, "
                + "pu.nombres || ' ' || pu.apellidos AS vendedor "
                + "FROM venta v "
                + "LEFT JOIN persona pc ON pc.id_persona = v.id_cliente "
                + "JOIN persona pu ON pu.id_persona = v.id_usuario"
                + filtro
                + " ORDER BY v.fecha_hora DESC, v.id_venta DESC LIMIT ? OFFSET ?";
        String sqlTotal = "SELECT count(*) FROM venta v" + filtro;

        List<VentaResumen> lista = new ArrayList<>();
        long total;
        try (Connection cn = Conexion.getConnection()) {
            try (PreparedStatement ps = cn.prepareStatement(sqlTotal)) {
                ponerParametros(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                int i = ponerParametros(ps, params);
                ps.setInt(i++, t);
                ps.setInt(i, Paginado.offset(p, t));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new VentaResumen(
                                rs.getInt("id_venta"),
                                String.format("%s-%06d", rs.getString("serie").trim(), rs.getInt("numero")),
                                rs.getTimestamp("fecha_hora").toLocalDateTime(),
                                rs.getString("cliente"),
                                rs.getString("vendedor"),
                                rs.getBigDecimal("total"),
                                MetodoPago.valueOf(rs.getString("metodo_pago")),
                                EstadoVenta.valueOf(rs.getString("estado"))));
                    }
                }
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
        return Paginado.de(lista, p, t, total);
    }

    /** Pone los parametros en orden y devuelve el siguiente indice libre. */
    private int ponerParametros(PreparedStatement ps, List<Object> params) throws SQLException {
        int i = 1;
        for (Object o : params) {
            ps.setObject(i++, o);
        }
        return i;
    }
}
