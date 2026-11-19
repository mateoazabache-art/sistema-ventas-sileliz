package pe.tecsup.sileliz.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Ayudante de las pruebas de reportes: inserta ventas de prueba usando la
 * MISMA conexion de la prueba. La prueba hace rollback al final, asi la base
 * queda limpia (con los triggers de 03 una venta ya no se puede borrar).
 *
 * Las fechas son de marzo de 2020 para no mezclarse con ventas reales.
 */
final class VentasDePrueba {

    private VentasDePrueba() {
    }

    /**
     * Inserta una venta EMITIDA de una sola linea y devuelve su id_venta.
     *
     * @param usuario     "admin" o "vendedor" (02_datos_prueba.sql)
     * @param dniCliente  null = consumidor final ("Varios")
     * @param idVariante  1..60 (02_datos_prueba.sql)
     */
    static int vender(Connection cn, LocalDateTime fechaHora, String usuario, String dniCliente,
                      int idVariante, int cantidad, String precio) throws SQLException {
        BigDecimal total = new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad));
        BigDecimal base = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        int idVenta;
        String sqlVenta = "INSERT INTO venta (fecha_hora, id_cliente, id_usuario, base_imponible, igv, total, metodo_pago) "
                + "VALUES (?, (SELECT id_persona FROM persona WHERE dni = ?), "
                + "        (SELECT id_persona FROM usuario WHERE nombre_usuario = ?), ?, ?, ?, 'EFECTIVO') "
                + "RETURNING id_venta";
        try (PreparedStatement ps = cn.prepareStatement(sqlVenta)) {
            ps.setTimestamp(1, Timestamp.valueOf(fechaHora));
            ps.setString(2, dniCliente);
            ps.setString(3, usuario);
            ps.setBigDecimal(4, base);
            ps.setBigDecimal(5, total.subtract(base));
            ps.setBigDecimal(6, total);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                idVenta = rs.getInt(1);
            }
        }
        String sqlDetalle = "INSERT INTO detalle_venta (id_venta, id_variante, descripcion, cantidad, precio_unitario, subtotal) "
                + "VALUES (?, ?, 'Prenda de prueba', ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sqlDetalle)) {
            ps.setInt(1, idVenta);
            ps.setInt(2, idVariante);
            ps.setInt(3, cantidad);
            ps.setBigDecimal(4, new BigDecimal(precio));
            ps.setBigDecimal(5, total);
            ps.executeUpdate();
        }
        return idVenta;
    }

    /** Anula una venta con motivo (lo unico que permite trg_venta_protegida). */
    static void anular(Connection cn, int idVenta) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE venta SET estado = 'ANULADA', motivo_anulacion = 'Prueba' WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }

    /** SKU de una variante, para comprobar la etiqueta de "mas vendidos". */
    static String sku(Connection cn, int idVariante) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT sku FROM variante_producto WHERE id_variante = ?")) {
            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
