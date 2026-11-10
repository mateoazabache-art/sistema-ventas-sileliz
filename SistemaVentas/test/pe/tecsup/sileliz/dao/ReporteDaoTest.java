package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.modelo.FilaReporte;
import pe.tecsup.sileliz.modelo.VentaResumen;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Pruebas de INTEGRACION de ReporteDao contra la base local.
 * Antes: ventas_db con 01, 02 y 03_vistas_triggers.sql, y config.properties.
 * Todo ocurre dentro de una transaccion que se deshace en @After.
 *
 * Ventas de prueba (marzo 2020):
 *   A  10/03 09:00  admin     cliente 70000001  variante 1  x2 a 50.00 = 100.00
 *   B  10/03 18:00  vendedor  Varios            variante 2  x1 a 80.00 =  80.00
 *   C  12/03 23:59  vendedor  Varios            variante 1  x3 a 50.00 = 150.00
 *   D  12/03 10:00  vendedor  Varios            variante 3  x9 a 10.00 =  90.00  ANULADA
 *   E  13/03 00:00  admin     Varios            variante 2  x1 a 80.00 =  80.00  (fuera del rango 10..12)
 */
public class ReporteDaoTest {

    private static final LocalDate DESDE = LocalDate.of(2020, 3, 10);
    private static final LocalDate HASTA = LocalDate.of(2020, 3, 12);

    private final ReporteDao dao = new ReporteDao();
    private Connection cn;
    private int ventaC;

    @Before
    public void prepararVentas() throws SQLException {
        cn = Conexion.getConnection();
        cn.setAutoCommit(false);
        VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 10, 9, 0), "admin", "70000001", 1, 2, "50.00");
        VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 10, 18, 0), "vendedor", null, 2, 1, "80.00");
        ventaC = VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 12, 23, 59), "vendedor", null, 1, 3, "50.00");
        int d = VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 12, 10, 0), "vendedor", null, 3, 9, "10.00");
        VentasDePrueba.anular(cn, d);
        VentasDePrueba.vender(cn, LocalDateTime.of(2020, 3, 13, 0, 0), "admin", null, 2, 1, "80.00");
    }

    @After
    public void deshacer() throws SQLException {
        cn.rollback();
        cn.close();
    }

    @Test
    public void unRangoSinVentasDevuelveVacioYTotalCero() throws SQLException {
        LocalDate dia = LocalDate.of(2019, 1, 1);
        assertEquals(0, dao.ventasPorRango(cn, dia, dia, 1, 10).total());
        FilaReporte total = dao.totalPorRango(cn, dia, dia);
        assertEquals(0, total.cantidad());
        assertEquals(new BigDecimal("0.00"), total.monto());
        assertTrue(dao.masVendidos(cn, dia, dia, 5).isEmpty());
        assertTrue(dao.ventasPorVendedor(cn, dia, dia).isEmpty());
    }

    @Test
    public void ventasPorRangoIncluyeLosDosDiasExtremosYLasAnuladas() throws SQLException {
        Paginado<VentaResumen> p = dao.ventasPorRango(cn, DESDE, HASTA, 1, 10);
        assertEquals(4, p.total());                 // A, B, C y D (anulada); E queda fuera
        VentaResumen primera = p.items().get(0);    // la mas reciente primero
        assertEquals(ventaC, primera.idVenta());
        assertEquals("Varios", primera.cliente());  // RN-06
        assertTrue(primera.comprobante().startsWith("B001-"));
        assertEquals(1, p.items().stream().filter(v -> v.estado().equals("ANULADA")).count());
    }

    @Test
    public void ventasPorRangoPagina() throws SQLException {
        Paginado<VentaResumen> p = dao.ventasPorRango(cn, DESDE, HASTA, 2, 3);
        assertEquals(4, p.total());
        assertEquals(2, p.paginas());
        assertEquals(1, p.items().size());          // pagina 2 de 3 en 3: solo queda 1
    }

    @Test
    public void totalDelRangoNoSumaLasAnuladas() throws SQLException {
        FilaReporte total = dao.totalPorRango(cn, DESDE, HASTA);
        assertEquals(3, total.cantidad());                      // A, B y C
        assertEquals(new BigDecimal("330.00"), total.monto());  // 100 + 80 + 150
    }

    @Test
    public void masVendidosOrdenaPorUnidadesYNoCuentaAnuladas() throws SQLException {
        List<FilaReporte> top = dao.masVendidos(cn, DESDE, HASTA, 10);
        assertEquals(2, top.size());                // variante 3 solo estaba en la venta anulada
        assertTrue(top.get(0).etiqueta().contains(VentasDePrueba.sku(cn, 1)));
        assertEquals(5, top.get(0).cantidad());     // 2 + 3 unidades
        assertEquals(new BigDecimal("250.00"), top.get(0).monto());
        assertEquals(1, top.get(1).cantidad());
    }

    @Test
    public void ventasPorVendedorAgrupaYOrdenaPorMonto() throws SQLException {
        List<FilaReporte> filas = dao.ventasPorVendedor(cn, DESDE, HASTA);
        assertEquals(2, filas.size());
        assertEquals(2, filas.get(0).cantidad());                    // vendedor: B y C (D anulada)
        assertEquals(new BigDecimal("230.00"), filas.get(0).monto());
        assertEquals(1, filas.get(1).cantidad());                    // admin: A
        assertEquals(new BigDecimal("100.00"), filas.get(1).monto());
    }
}
