package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.dao.MovimientoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.dao.VentaDao;
import pe.tecsup.sileliz.excepcion.AccesoDenegadoException;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.excepcion.StockInsuficienteException;
import pe.tecsup.sileliz.modelo.Comprobante;
import pe.tecsup.sileliz.modelo.DetalleVenta;
import pe.tecsup.sileliz.modelo.EstadoVenta;
import pe.tecsup.sileliz.modelo.MetodoPago;
import pe.tecsup.sileliz.modelo.MovimientoStock;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.Rol;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.modelo.Venta;
import pe.tecsup.sileliz.modelo.VentaSolicitud;

/**
 * Pruebas UNITARIAS de VentaService (reglas de la venta y de la anulacion).
 *
 * Usan DAO falsos: no escriben en la base. Las pruebas que llegan a la
 * transaccion (registrar bien, stock insuficiente y anular bien) abren una
 * conexion real con Conexion.getConnection(), asi que necesitan la base
 * encendida; como los DAO son falsos, no se guarda ni se cambia ningun dato.
 */
public class VentaServiceTest {

    /** DAO de ventas falso: recuerda lo que le pidieron y devuelve el comprobante preparado. */
    private static class VentaDaoFalso extends VentaDao {
        Comprobante comprobante;
        List<DetalleVenta> detallesGuardados = new ArrayList<>();
        boolean anulada = false;

        @Override
        public void insertar(Connection cn, Venta v) {
            v.setIdVenta(200);
            v.setSerie("B001");
            v.setNumero(200);
        }

        @Override
        public void insertarDetalle(Connection cn, int idVenta, DetalleVenta d) {
            detallesGuardados.add(d);
        }

        @Override
        public boolean anular(Connection cn, int idVenta, String motivo) {
            anulada = true;
            return true;
        }

        @Override
        public Optional<Comprobante> buscarPorId(int idVenta) {
            return Optional.ofNullable(comprobante);
        }
    }

    /** DAO de variantes falso: las variantes salen de un mapa y el stock se controla con hayStock. */
    private static class VarianteDaoFalso extends VarianteDao {
        Map<Integer, VarianteProducto> variantes = new HashMap<>();
        boolean hayStock = true;
        List<int[]> descontadas = new ArrayList<>();   // {idVariante, cantidad}
        List<int[]> repuestas = new ArrayList<>();     // {idVariante, cantidad}

        @Override
        public Optional<VarianteProducto> buscarPorId(int idVariante) {
            return Optional.ofNullable(variantes.get(idVariante));
        }

        @Override
        public boolean descontarStock(Connection cn, int idVariante, int cantidad) {
            descontadas.add(new int[]{idVariante, cantidad});
            return hayStock;
        }

        @Override
        public void reponerStock(Connection cn, int idVariante, int cantidad) {
            repuestas.add(new int[]{idVariante, cantidad});
        }
    }

    /** DAO de movimientos falso: solo cuenta cuantos le pidieron guardar. */
    private static class MovimientoDaoFalso extends MovimientoDao {
        int insertados = 0;

        @Override
        public void insertar(Connection cn, MovimientoStock m) {
            insertados++;
        }
    }

    private VentaDaoFalso ventaDao;
    private VarianteDaoFalso varianteDao;
    private MovimientoDaoFalso movimientoDao;
    private VentaService servicio;

    @Before
    public void preparar() {
        ventaDao = new VentaDaoFalso();
        varianteDao = new VarianteDaoFalso();
        movimientoDao = new MovimientoDaoFalso();
        servicio = new VentaService(ventaDao, varianteDao, movimientoDao);

        // Una blusa de prueba de S/ 59.90 (variante 1) con stock.
        Producto blusa = new Producto(1, "P-TEST", "Blusa campesina", "Blusa de prueba",
                null, new BigDecimal("59.90"), true);
        varianteDao.variantes.put(1,
                new VarianteProducto(1, blusa, "XL", "Celeste", "P-TEST-XL-CEL", 10, 2, true));
    }

    // ---------- ayudantes ----------

    /** Arma la solicitud como llegaria en el JSON de POST /api/ventas. */
    private VentaSolicitud solicitud(String tipoDescuento, int idVariante, int cantidad) {
        String json = "{\"metodoPago\":\"EFECTIVO\",\"tipoDescuento\":\"" + tipoDescuento + "\","
                + "\"lineas\":[{\"idVariante\":" + idVariante + ",\"cantidad\":" + cantidad + "}]}";
        return new Gson().fromJson(json, VentaSolicitud.class);
    }

    private Usuario usuario(int id, Rol rol) {
        Usuario u = new Usuario("Prueba", "Usuario", "70000099", "prueba" + id, rol);
        u.setIdPersona(id);
        return u;
    }

    /** Una venta EMITIDA de 2 prendas de la variante 1, hecha por el usuario indicado. */
    private Venta ventaEmitida(int idUsuario) {
        Venta v = new Venta(null, idUsuario, MetodoPago.EFECTIVO);
        v.setIdVenta(115);
        v.setSerie("B001");
        v.setNumero(115);
        v.agregarDetalle(new DetalleVenta(1, "Blusa campesina XL Celeste", 2, new BigDecimal("59.90")));
        return v;
    }

    private Comprobante comprobanteDe(Venta v) {
        return new Comprobante(v, "Varios", null, "Valeria Vendedora");
    }

    // ---------- registrar: casos correctos (usan la conexion real) ----------

    @Test
    public void registraVentaNormalConTotalBaseEIgvCorrectos() {
        Venta v = servicio.registrar(solicitud("SIN", 1, 1), 5);
        assertEquals(new BigDecimal("59.90"), v.getTotal());
        assertEquals(new BigDecimal("50.76"), v.getBaseImponible());
        assertEquals(new BigDecimal("9.14"), v.getIgv());
        assertEquals(new BigDecimal("0.00"), v.getDescuento());
        assertEquals("Blusa campesina XL Celeste", v.getDetalles().get(0).getDescripcion());
        assertEquals(1, ventaDao.detallesGuardados.size());
        assertEquals(1, movimientoDao.insertados);
        assertEquals(1, varianteDao.descontadas.get(0)[0]);   // idVariante
        assertEquals(1, varianteDao.descontadas.get(0)[1]);   // cantidad
    }

    @Test
    public void porCantidadConTresPrendasAplicaDiezPorCiento() {
        Venta v = servicio.registrar(solicitud("POR_CANTIDAD", 1, 3), 5);
        assertEquals(new BigDecimal("17.97"), v.getDescuento());
        assertEquals(new BigDecimal("161.73"), v.getTotal());
        assertEquals(new BigDecimal("137.06"), v.getBaseImponible());
        assertEquals(new BigDecimal("24.67"), v.getIgv());
    }

    // ---------- registrar: datos invalidos (no llegan a la base) ----------

    @Test(expected = DatoInvalidoException.class)
    public void rechazaTipoDeDescuentoInvalido() {
        servicio.registrar(solicitud("XYZ", 1, 1), 5);
    }

    @Test(expected = DatoInvalidoException.class)
    public void rechazaCantidadCero() {
        servicio.registrar(solicitud("SIN", 1, 0), 5);
    }

    @Test(expected = DatoInvalidoException.class)
    public void rechazaListaDeLineasVacia() {
        String json = "{\"metodoPago\":\"EFECTIVO\",\"tipoDescuento\":\"SIN\",\"lineas\":[]}";
        servicio.registrar(new Gson().fromJson(json, VentaSolicitud.class), 5);
    }

    @Test(expected = NoEncontradoException.class)
    public void rechazaVarianteInexistente() {
        servicio.registrar(solicitud("SIN", 999, 1), 5);
    }

    // ---------- registrar: sin stock ----------

    @Test
    public void stockInsuficienteLanzaErrorYNoRegistraMovimientos() {
        varianteDao.hayStock = false;
        try {
            servicio.registrar(solicitud("SIN", 1, 1), 5);
            org.junit.Assert.fail("Debia lanzar StockInsuficienteException");
        } catch (StockInsuficienteException e) {
            assertEquals(0, movimientoDao.insertados);
        }
    }

    // ---------- buscarPorId: quien puede ver que ----------

    @Test(expected = AccesoDenegadoException.class)
    public void vendedorNoPuedeVerLaVentaDeOtro() {
        ventaDao.comprobante = comprobanteDe(ventaEmitida(5));          // venta del usuario 5
        servicio.buscarPorId(115, usuario(6, Rol.VENDEDOR));            // la pide el usuario 6
    }

    @Test
    public void vendedorPuedeVerSuPropiaVenta() {
        ventaDao.comprobante = comprobanteDe(ventaEmitida(5));
        Comprobante c = servicio.buscarPorId(115, usuario(5, Rol.VENDEDOR));
        assertEquals(115, c.venta().getIdVenta());
    }

    // ---------- anular ----------

    @Test(expected = AccesoDenegadoException.class)
    public void vendedorNoPuedeAnular() {
        ventaDao.comprobante = comprobanteDe(ventaEmitida(5));
        servicio.anular(115, "Prueba", usuario(5, Rol.VENDEDOR));
    }

    @Test(expected = DatoInvalidoException.class)
    public void anularSinMotivoSeRechaza() {
        ventaDao.comprobante = comprobanteDe(ventaEmitida(5));
        servicio.anular(115, "   ", usuario(1, Rol.ADMINISTRADOR));
    }

    @Test(expected = DatoInvalidoException.class)
    public void anularDosVecesSeRechaza() {
        Venta yaAnulada = ventaEmitida(5);
        yaAnulada.anular("Primera anulacion");
        ventaDao.comprobante = comprobanteDe(yaAnulada);
        servicio.anular(115, "Segunda vez", usuario(1, Rol.ADMINISTRADOR));
    }

    @Test
    public void adminAnulaRepuestaElStockYAnotaElMovimiento() {
        ventaDao.comprobante = comprobanteDe(ventaEmitida(5));          // 2 prendas de la variante 1
        Comprobante c = servicio.anular(115, "Error de digitacion", usuario(1, Rol.ADMINISTRADOR));
        assertEquals(EstadoVenta.ANULADA, c.venta().getEstado());
        assertTrue(ventaDao.anulada);
        assertEquals(1, varianteDao.repuestas.size());
        assertEquals(1, varianteDao.repuestas.get(0)[0]);   // idVariante
        assertEquals(2, varianteDao.repuestas.get(0)[1]);   // cantidad repuesta
        assertEquals(1, movimientoDao.insertados);
    }
}