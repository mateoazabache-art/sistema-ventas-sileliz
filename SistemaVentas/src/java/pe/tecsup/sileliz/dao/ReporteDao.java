package pe.tecsup.sileliz.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.tecsup.sileliz.modelo.FilaReporte;
import pe.tecsup.sileliz.modelo.VentaResumen;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Consultas de los reportes (solo administrador: /api/reportes/...). Tarea 12 de Rafael.
 *
 * Rango de fechas: "desde" y "hasta" son DIAS completos e incluidos.
 * Del 01/11 al 07/11 = fecha_hora >= 01/11 00:00 y fecha_hora < 08/11 00:00.
 * Asi se aprovecha el indice idx_venta_fecha_hora.
 *
 * Solo lee: no valida las fechas (eso lo hace ReporteService, tarea 13).
 * Cada metodo publico abre su conexion; la version con Connection existe
 * para que las pruebas trabajen dentro de una transaccion y hagan rollback.
 */
public class ReporteDao {

    /** Filtro de rango sobre la columna fecha_hora de la venta (alias ve). */
    private static final String RANGO = "ve.fecha_hora >= ? AND ve.fecha_hora < ? ";

    // ------------------------------------------------------------------
    // Ventas por rango (lista paginada, incluye anuladas para verlas en rojo)
    // ------------------------------------------------------------------

    public Paginado<VentaResumen> ventasPorRango(LocalDate desde, LocalDate hasta, int pagina, int tam) {
        try (Connection cn = Conexion.getConnection()) {
            return ventasPorRango(cn, desde, hasta, pagina, tam);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    Paginado<VentaResumen> ventasPorRango(Connection cn, LocalDate desde, LocalDate hasta,
                                          int pagina, int tam) throws SQLException {
        int t = Paginado.normalizarTam(tam);
        String sql = "SELECT ve.id_venta, ve.comprobante, ve.fecha_hora, ve.cliente, ve.vendedor, "
                + "       ve.metodo_pago, ve.descuento, ve.total, ve.estado "
                + "FROM v_ventas_resumen ve "
                + "WHERE " + RANGO
                + "ORDER BY ve.fecha_hora DESC, ve.id_venta DESC "
                + "LIMIT ? OFFSET ?";
        List<VentaResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = ponerRango(ps, 1, desde, hasta);
            ps.setInt(i++, t);
            ps.setInt(i, Paginado.offset(pagina, t));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new VentaResumen(
                            rs.getInt("id_venta"),
                            rs.getString("comprobante"),
                            rs.getTimestamp("fecha_hora").toLocalDateTime(),
                            rs.getString("cliente"),
                            rs.getString("vendedor"),
                            rs.getString("metodo_pago"),
                            rs.getBigDecimal("descuento"),
                            rs.getBigDecimal("total"),
                            rs.getString("estado")));
                }
            }
        }
        return Paginado.de(lista, Math.max(pagina, 1), t, contarVentas(cn, desde, hasta));
    }

    private long contarVentas(Connection cn, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT count(*) FROM venta ve WHERE " + RANGO;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ponerRango(ps, 1, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // Total acumulado del rango (solo EMITIDAS)
    // ------------------------------------------------------------------

    /** Cuantas ventas emitidas hubo en el rango y cuanto suman. */
    public FilaReporte totalPorRango(LocalDate desde, LocalDate hasta) {
        try (Connection cn = Conexion.getConnection()) {
            return totalPorRango(cn, desde, hasta);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    FilaReporte totalPorRango(Connection cn, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT count(*), COALESCE(SUM(ve.total), 0) "
                + "FROM venta ve "
                + "WHERE ve.estado = 'EMITIDA' AND " + RANGO;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ponerRango(ps, 1, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new FilaReporte("Total", rs.getLong(1), dinero(rs.getBigDecimal(2)));
            }
        }
    }

    // ------------------------------------------------------------------
    // Mas vendidos (por variante, solo EMITIDAS)
    // ------------------------------------------------------------------

    /** Las variantes con mas unidades vendidas en el rango (las primeras "limite"). */
    public List<FilaReporte> masVendidos(LocalDate desde, LocalDate hasta, int limite) {
        try (Connection cn = Conexion.getConnection()) {
            return masVendidos(cn, desde, hasta, limite);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    List<FilaReporte> masVendidos(Connection cn, LocalDate desde, LocalDate hasta, int limite)
            throws SQLException {
        String sql = "SELECT p.nombre, v.talla, v.color, v.sku, "
                + "       SUM(d.cantidad) AS unidades, SUM(d.subtotal) AS importe "
                + "FROM detalle_venta d "
                + "JOIN venta ve             ON ve.id_venta  = d.id_venta "
                + "JOIN variante_producto v  ON v.id_variante = d.id_variante "
                + "JOIN producto p           ON p.id_producto = v.id_producto "
                + "WHERE ve.estado = 'EMITIDA' AND " + RANGO
                + "GROUP BY p.nombre, v.talla, v.color, v.sku "
                + "ORDER BY unidades DESC, importe DESC, v.sku "
                + "LIMIT ?";
        List<FilaReporte> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = ponerRango(ps, 1, desde, hasta);
            ps.setInt(i, Paginado.normalizarTam(limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String etiqueta = rs.getString("nombre") + " " + rs.getString("talla") + " "
                            + rs.getString("color") + " (" + rs.getString("sku") + ")";
                    lista.add(new FilaReporte(etiqueta, rs.getLong("unidades"),
                            dinero(rs.getBigDecimal("importe"))));
                }
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Ventas por vendedor (solo EMITIDAS)
    // ------------------------------------------------------------------

    /** Numero de ventas y total vendido por cada vendedor en el rango. */
    public List<FilaReporte> ventasPorVendedor(LocalDate desde, LocalDate hasta) {
        try (Connection cn = Conexion.getConnection()) {
            return ventasPorVendedor(cn, desde, hasta);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    List<FilaReporte> ventasPorVendedor(Connection cn, LocalDate desde, LocalDate hasta)
            throws SQLException {
        String sql = "SELECT ve.vendedor, count(*) AS ventas, SUM(ve.total) AS monto "
                + "FROM v_ventas_resumen ve "
                + "WHERE ve.estado = 'EMITIDA' AND " + RANGO
                + "GROUP BY ve.id_usuario, ve.vendedor "
                + "ORDER BY monto DESC, ve.vendedor";
        List<FilaReporte> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ponerRango(ps, 1, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new FilaReporte(rs.getString("vendedor"), rs.getLong("ventas"),
                            dinero(rs.getBigDecimal("monto"))));
                }
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    /**
     * Pone "desde 00:00" y "dia siguiente a hasta 00:00" en dos "?" seguidos.
     * Devuelve el siguiente indice libre.
     */
    static int ponerRango(PreparedStatement ps, int i, LocalDate desde, LocalDate hasta) throws SQLException {
        ps.setDate(i, Date.valueOf(desde));
        ps.setDate(i + 1, Date.valueOf(hasta.plusDays(1)));
        return i + 2;
    }

    /** Montos siempre con 2 decimales (0 -> 0.00). */
    static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
