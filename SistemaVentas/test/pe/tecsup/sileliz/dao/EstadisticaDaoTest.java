package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.modelo.ResumenDia;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Pruebas de EstadisticaDao (Inicio) y de los triggers de 03_vistas_triggers.sql.
 * Todo dentro de una transaccion que se deshace en @After.
 */
public class EstadisticaDaoTest {

    private static final LocalDate DIA = LocalDate.of(2020, 3, 20);

    private final EstadisticaDao dao = new EstadisticaDao();
    private Connection cn;

    @Before
    public void abrir() throws SQLException {
        cn = Conexion.getConnection();
        cn.setAutoCommit(false);
    }

    @After
    public void deshacer() throws SQLException {
        cn.rollback();
        cn.close();
    }

    @Test
    public void unDiaSinVentasDaCeros() throws SQLException {
        ResumenDia r = dao.resumenDe(cn, LocalDate.of(2019, 1, 1));
        assertEquals(0, r.ventas());
        assertEquals(new BigDecimal("0.00"), r.total());
        assertEquals(0, r.anuladas());
    }

    @Test
    public void cuentaVentasDelDiaClientesYStockBajo() throws SQLException {
        VentasDePrueba.vender(cn, DIA.atTime(9, 0), "vendedor", null, 1, 2, "50.00");
        VentasDePrueba.vender(cn, DIA.atTime(20, 30), "admin", "70000002", 2, 1, "39.90");
        int anulada = VentasDePrueba.vender(cn, DIA.atTime(12, 0), "vendedor", null, 3, 1, "10.00");
        VentasDePrueba.anular(cn, anulada);
        VentasDePrueba.vender(cn, DIA.plusDays(1).atStartOfDay(), "admin", null, 1, 1, "50.00"); // otro dia

        ResumenDia r = dao.resumenDe(cn, DIA);
        assertEquals(DIA, r.fecha());
        assertEquals(2, r.ventas());
        assertEquals(new BigDecimal("139.90"), r.total());
        assertEquals(1, r.anuladas());
        assertEquals(10, r.clientes());     // los 10 clientes de 02_datos_prueba.sql
        assertEquals(13, r.stockBajo());    // las 13 variantes con stock <= 2 de 02_datos_prueba.sql
    }

    // ---- Triggers (03_vistas_triggers.sql) ----

    @Test
    public void unaVentaNoSeBorra() throws SQLException {
        int id = VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 21, 10, 0), "admin", null, 1, 1, "50.00");
        esperarRechazo("DELETE FROM venta WHERE id_venta = " + id);
    }

    @Test
    public void unaVentaEmitidaNoCambiaSuTotal() throws SQLException {
        int id = VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 21, 10, 0), "admin", null, 1, 1, "50.00");
        esperarRechazo("UPDATE venta SET total = 1 WHERE id_venta = " + id);
    }

    @Test
    public void elDetalleNoSeModifica() throws SQLException {
        int id = VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 21, 10, 0), "admin", null, 1, 1, "50.00");
        esperarRechazo("UPDATE detalle_venta SET cantidad = 9 WHERE id_venta = " + id);
    }

    @Test
    public void anularConMotivoSiSePuede() throws SQLException {
        int id = VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 21, 10, 0), "admin", null, 1, 1, "50.00");
        VentasDePrueba.anular(cn, id);        // no lanza excepcion
    }

    /** Ejecuta el SQL y comprueba que el trigger lo rechaza con un 400 y mensaje claro. */
    private void esperarRechazo(String sql) throws SQLException {
        Savepoint sp = cn.setSavepoint();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.executeUpdate();
            fail("El trigger debio rechazar: " + sql);
        } catch (SQLException e) {
            cn.rollback(sp);                  // la transaccion sigue usable
            DatoInvalidoException ex = (DatoInvalidoException) ErrorSql.traducir(e);
            assertEquals(400, ex.getCodigoHttp());
            assertEquals(false, ex.getMessage().contains("trg_"));   // al usuario no le llega lo tecnico
        }
    }
}
