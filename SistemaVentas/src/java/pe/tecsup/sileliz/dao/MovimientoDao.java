package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import pe.tecsup.sileliz.modelo.MovimientoStock;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Acceso a datos del kardex (tabla movimiento_stock).
 * Participa de la transaccion de la venta: recibe la Connection y NO la cierra.
 */
public class MovimientoDao {

    private static final String SQL_INSERT =
            "INSERT INTO movimiento_stock (id_variante, id_venta, id_usuario, tipo, cantidad, motivo) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    /** Registra un movimiento de stock dentro de la transaccion de cn. */
    public void insertar(Connection cn, MovimientoStock m) {
        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, m.getIdVariante());
            if (m.getIdVenta() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, m.getIdVenta());
            }
            ps.setInt(3, m.getIdUsuario());
            ps.setString(4, m.getTipo().name());
            ps.setInt(5, m.getCantidad());
            ps.setString(6, m.getMotivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }
}
