package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.dao.ReporteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.modelo.FilaReporte;
import pe.tecsup.sileliz.modelo.ReporteVentas;
import pe.tecsup.sileliz.modelo.VentaResumen;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Pruebas UNITARIAS de ReporteService: validacion del rango de fechas y de
 * los numeros que llegan del navegador.
 *
 * No usan la base: un DAO falso anota con que valores lo llamaron
 * (ReporteDaoTest ya prueba las consultas de verdad). El reloj esta
 * detenido en el 11/11/2026 para que "hoy" sea siempre el mismo dia.
 */
public class ReporteServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

    /** DAO falso: no consulta nada, solo recuerda los parametros recibidos. */
    private static class ReporteDaoFalso extends ReporteDao {
        LocalDate desde;
        LocalDate hasta;
        int pagina;
        int tam;
        int limite;
        int llamadas = 0;

        @Override
        public Paginado<VentaResumen> ventasPorRango(LocalDate d, LocalDate h, int pag, int t) {
            anotar(d, h);
            pagina = pag;
            tam = t;
            return Paginado.de(List.of(), pag, Paginado.normalizarTam(t), 3);
        }

        @Override
        public FilaReporte totalPorRango(LocalDate d, LocalDate h) {
            anotar(d, h);
            return new FilaReporte("Total", 2, new BigDecimal("150.00"));
        }

        @Override
        public List<FilaReporte> masVendidos(LocalDate d, LocalDate h, int lim) {
            anotar(d, h);
            limite = lim;
            return List.of();
        }

        @Override
        public List<FilaReporte> ventasPorVendedor(LocalDate d, LocalDate h) {
            anotar(d, h);
            return List.of();
        }

        private void anotar(LocalDate d, LocalDate h) {
            desde = d;
            hasta = h;
            llamadas++;
        }
    }

    private ReporteDaoFalso dao;
    private ReporteService servicio;

    @Before
    public void preparar() {
        dao = new ReporteDaoFalso();
        ZoneId zona = ZoneId.of("America/Lima");
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        servicio = new ReporteService(dao, reloj);
    }

    // ---------------- Rango de fechas ----------------

    @Test
    public void rangoCompletoSeRespeta() {
        ReporteService.Rango r = servicio.validarRango("2026-11-01", "2026-11-07");
        assertEquals(LocalDate.of(2026, 11, 1), r.desde());
        assertEquals(LocalDate.of(2026, 11, 7), r.hasta());
    }

    @Test
    public void sinFechasUsaDelDiaUnoHastaHoy() {
        ReporteService.Rango r = servicio.validarRango(null, "  ");
        assertEquals(LocalDate.of(2026, 11, 1), r.desde());
        assertEquals(HOY, r.hasta());
    }

    @Test
    public void sinDesdeUsaElDiaUnoDelMesDeHasta() {
        ReporteService.Rango r = servicio.validarRango("", "2026-10-20");
        assertEquals(LocalDate.of(2026, 10, 1), r.desde());
        assertEquals(LocalDate.of(2026, 10, 20), r.hasta());
    }

    @Test
    public void unSoloDiaEsValido() {
        ReporteService.Rango r = servicio.validarRango("2026-11-05", "2026-11-05");
        assertEquals(r.desde(), r.hasta());
    }

    @Test
    public void desdePosteriorAHastaEs400() {
        esperar400(() -> servicio.validarRango("2026-11-08", "2026-11-07"), "posterior");
    }

    @Test
    public void fechaMalEscritaEs400() {
        esperar400(() -> servicio.validarRango("01/11/2026", "2026-11-07"), "desde");
        esperar400(() -> servicio.validarRango("2026-11-01", "2026-02-30"), "hasta");
        esperar400(() -> servicio.validarRango("ayer", null), "AAAA-MM-DD");
    }

    @Test
    public void rangoDeUnAnoEsValidoYMasLargoEs400() {
        servicio.validarRango("2025-11-11", "2026-11-11");                 // 366 dias: pasa
        esperar400(() -> servicio.validarRango("2025-11-10", "2026-11-11"), "366");
    }

    // ---------------- Reportes ----------------

    @Test
    public void ventasUneElTotalConLaPagina() {
        ReporteVentas r = servicio.ventas("2026-11-01", "2026-11-07", "2", "5");
        assertEquals(LocalDate.of(2026, 11, 1), r.desde());
        assertEquals(LocalDate.of(2026, 11, 7), r.hasta());
        assertEquals(2, r.ventas());
        assertEquals(new BigDecimal("150.00"), r.total());
        assertEquals(3, r.pagina().total());        // la lista incluye la anulada
        assertEquals(2, dao.pagina);
        assertEquals(5, dao.tam);
    }

    @Test
    public void ventasSinPaginaNiTamUsaLosValoresPorDefecto() {
        servicio.ventas(null, null, null, "");
        assertEquals(1, dao.pagina);
        assertEquals(Paginado.TAM_POR_DEFECTO, dao.tam);
        assertEquals(HOY, dao.hasta);
    }

    @Test
    public void paginaCeroONegativaSeTomaComoLaPrimera() {
        servicio.ventas(null, null, "0", null);
        assertEquals(1, dao.pagina);
        servicio.ventas(null, null, "-4", null);
        assertEquals(1, dao.pagina);
    }

    @Test
    public void numerosConLetrasSon400() {
        esperar400(() -> servicio.ventas(null, null, "dos", null), "página");
        esperar400(() -> servicio.ventas(null, null, "1", "10x"), "tamaño");
        esperar400(() -> servicio.masVendidos(null, null, "muchos"), "límite");
    }

    @Test
    public void masVendidosUsaDiezPorDefecto() {
        servicio.masVendidos("2026-11-01", "2026-11-07", null);
        assertEquals(ReporteService.LIMITE_POR_DEFECTO, dao.limite);
        servicio.masVendidos("2026-11-01", "2026-11-07", "3");
        assertEquals(3, dao.limite);
    }

    @Test
    public void porVendedorPasaElRangoValidado() {
        servicio.porVendedor("2026-11-01", "2026-11-07");
        assertEquals(LocalDate.of(2026, 11, 1), dao.desde);
        assertEquals(LocalDate.of(2026, 11, 7), dao.hasta);
    }

    @Test
    public void conRangoInvalidoNoSeConsultaLaBase() {
        esperar400(() -> servicio.ventas("2026-11-08", "2026-11-07", null, null), "posterior");
        esperar400(() -> servicio.masVendidos("x", null, null), "desde");
        esperar400(() -> servicio.porVendedor(null, "x"), "hasta");
        assertEquals(0, dao.llamadas);
    }

    // ---------------- Ayudante ----------------

    /** La accion debe lanzar DatoInvalidoException (400) y el mensaje debe contener el texto. */
    private static void esperar400(Runnable accion, String textoEsperado) {
        try {
            accion.run();
            fail("Debio lanzar DatoInvalidoException");
        } catch (DatoInvalidoException e) {
            assertEquals(400, e.getCodigoHttp());
            assertTrue("Mensaje: " + e.getMessage(), e.getMessage().contains(textoEsperado));
        }
    }
}
