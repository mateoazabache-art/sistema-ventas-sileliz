package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import pe.tecsup.sileliz.modelo.ResumenDia;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Numeros de la pagina de Inicio (GET /api/estadisticas/hoy, todos los roles).
 * Tarea 12 de Rafael; lo usa Cesia en su tarea 10 (index.html).
 *
 * Una sola consulta con subconsultas: un viaje a la base para las 5 tarjetas.
 */
public class EstadisticaDao {

    /** Resumen del dia de hoy (fecha del servidor). */
    public ResumenDia resumenHoy() {
        return resumenDe(LocalDate.now());
    }

    /** Resumen de un dia cualquiera. */
    public ResumenDia resumenDe(LocalDate dia) {
        try (Connection cn = Conexion.getConnection()) {
            return resumenDe(cn, dia);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    ResumenDia resumenDe(Connection cn, LocalDate dia) throws SQLException {
        String sql = "SELECT "
                + " (SELECT count(*) FROM venta ve WHERE ve.estado = 'EMITIDA' AND " + rango() + ") AS ventas, "
                + " (SELECT COALESCE(SUM(ve.total), 0) FROM venta ve "
                + "   WHERE ve.estado = 'EMITIDA' AND " + rango() + ") AS total, "
                + " (SELECT count(*) FROM venta ve WHERE ve.estado = 'ANULADA' AND " + rango() + ") AS anuladas, "
                + " (SELECT count(*) FROM cliente c JOIN persona p ON p.id_persona = c.id_persona "
                + "   WHERE p.activo) AS clientes, "
                + " (SELECT count(*) FROM v_stock_bajo) AS stock_bajo";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            for (int k = 0; k < 3; k++) {          // tres subconsultas con rango de fechas
                i = ReporteDao.ponerRango(ps, i, dia, dia);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ResumenDia(
                        dia,
                        rs.getLong("ventas"),
                        ReporteDao.dinero(rs.getBigDecimal("total")),
                        rs.getLong("anuladas"),
                        rs.getLong("clientes"),
                        rs.getLong("stock_bajo"));
            }
        }
    }

    private static String rango() {
        return "ve.fecha_hora >= ? AND ve.fecha_hora < ?";
    }
}
