package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.dao.ProductoDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.DuplicadoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.excepcion.PersistenciaException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Paginado;

public class ProductoServiceTest {

    // ---------- DAO falsos ----------

    private static class ProductoDaoFalso extends ProductoDao {

        final Map<Integer, Producto> almacen = new HashMap<>();

        int llamadasInsertar;
        int llamadasActualizar;
        int llamadasDesactivar;
        int llamadasBuscar;
        int llamadasListarPaginado;
        int llamadasStockBajo;

        boolean resultadoActualizar = true;
        boolean resultadoDesactivar = true;

        int ultimoIdDesactivado = -1;
        int ultimaPagina = -1;
        int ultimoTam = -1;
        int ultimaCategoria = -1;
        String ultimoTexto = "<sin llamada>";

        Paginado<Producto> paginadoARetornar;
        List<VarianteProducto> stockBajoARetornar = new ArrayList<>();

        RuntimeException fallaAlInsertar;
        RuntimeException fallaAlBuscar;
        RuntimeException fallaAlListar;
        RuntimeException fallaEnStockBajo;

        int totalLlamadas() {
            return llamadasInsertar + llamadasActualizar
                    + llamadasDesactivar + llamadasBuscar
                    + llamadasListarPaginado + llamadasStockBajo;
        }

        @Override
        public Optional<Producto> buscarPorId(int idProducto) {
            llamadasBuscar++;

            if (fallaAlBuscar != null) {
                throw fallaAlBuscar;
            }

            return Optional.ofNullable(almacen.get(idProducto));
        }

        @Override
        public Producto insertar(Producto producto) {
            llamadasInsertar++;

            if (fallaAlInsertar != null) {
                throw fallaAlInsertar;
            }

            return producto;
        }

        @Override
        public boolean actualizar(Producto producto) {
            llamadasActualizar++;
            return resultadoActualizar;
        }

        @Override
        public boolean desactivar(int idProducto) {
            llamadasDesactivar++;
            ultimoIdDesactivado = idProducto;
            return resultadoDesactivar;
        }

        @Override
        public Paginado<Producto> listarPaginado(
                int pagina, int tam, String texto, int idCategoria) {

            llamadasListarPaginado++;
            ultimaPagina = pagina;
            ultimoTam = tam;
            ultimoTexto = texto;
            ultimaCategoria = idCategoria;

            if (fallaAlListar != null) {
                throw fallaAlListar;
            }

            return paginadoARetornar;
        }

        @Override
        public List<VarianteProducto> listarStockBajo() {
            llamadasStockBajo++;

            if (fallaEnStockBajo != null) {
                throw fallaEnStockBajo;
            }

            return stockBajoARetornar;
        }
    }

    private static class CategoriaDaoFalso extends CategoriaDao {

        final Map<Integer, Categoria> almacen = new HashMap<>();
        int llamadasBuscar;

        @Override
        public Optional<Categoria> buscarPorId(int idCategoria) {
            llamadasBuscar++;
            return Optional.ofNullable(almacen.get(idCategoria));
        }
    }

    // ---------- Utilidades ----------

    private interface Accion {
        void ejecutar() throws Exception;
    }

    private static <T extends SilelizException> T esperar(
            Class<T> tipo, Accion accion) {

        try {
            accion.ejecutar();
        } catch (Throwable t) {
            if (tipo.isInstance(t)) {
                return tipo.cast(t);
            }

            throw new AssertionError(
                    "Se esperaba " + tipo.getSimpleName()
                    + " pero se lanzó " + t, t);
        }

        throw new AssertionError(
                "Se esperaba " + tipo.getSimpleName()
                + " pero no se lanzó ninguna excepción.");
    }

    private static int codigoHttp(SilelizException e) {
        return e.getCodigoHttp();
    }

    private static DuplicadoException nuevaDuplicada() {
        return new DuplicadoException(
                "Código duplicado", new SQLException("Violación UNIQUE"));
    }

    private static PersistenciaException nuevaPersistencia() {
        return new PersistenciaException("Error de persistencia");
    }

    private static String repetir(char c, int veces) {
        return String.valueOf(c).repeat(veces);
    }

    private ProductoDaoFalso productoDao;
    private CategoriaDaoFalso categoriaDao;
    private ProductoService servicio;

    @Before
    public void preparar() {
        productoDao = new ProductoDaoFalso();
        categoriaDao = new CategoriaDaoFalso();
        servicio = new ProductoService(productoDao, categoriaDao);

        Categoria activa = new Categoria();
        activa.setIdCategoria(1);
        activa.setNombre("Polos");
        activa.setActivo(true);
        categoriaDao.almacen.put(1, activa);

        Categoria inactiva = new Categoria();
        inactiva.setIdCategoria(2);
        inactiva.setNombre("Descontinuada");
        inactiva.setActivo(false);
        categoriaDao.almacen.put(2, inactiva);
    }

    private Producto productoValido() {
        Producto p = new Producto();
        p.setIdProducto(10);
        p.setCodigo("POL-001");
        p.setNombre("Polo básico");
        p.setDescripcion("Polo de algodón");
        p.setCategoria(categoriaDao.almacen.get(1));
        p.setPrecioVenta(new BigDecimal("49.90"));
        p.setActivo(true);
        return p;
    }

    private void guardarProductoExistente() {
        productoDao.almacen.put(10, productoValido());
    }

    private void assertDatoInvalidoSinLlamadasAlDao(Producto producto) {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.registrar(producto));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    // ---------- Constructor ----------

    @Test(expected = NullPointerException.class)
    public void constructorConProductoDaoNulo_lanzaNullPointer() {
        new ProductoService(null, categoriaDao);
    }

    @Test(expected = NullPointerException.class)
    public void constructorConCategoriaDaoNulo_lanzaNullPointer() {
        new ProductoService(productoDao, null);
    }

    // ---------- Registro ----------

    @Test
    public void registrarProductoValido_insertaYDevuelveElProducto() {
        Producto p = productoValido();

        Producto resultado = servicio.registrar(p);

        assertSame(p, resultado);
        assertEquals(1, productoDao.llamadasInsertar);
    }

    @Test
    public void registrarProductoNulo_lanzaDatoInvalido() {
        assertDatoInvalidoSinLlamadasAlDao(null);
    }

    @Test
    public void registrarCodigoNulo_noLlamaAlDao() {
        Producto p = productoValido();
        p.setCodigo(null);
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarCodigoVacio_noLlamaAlDao() {
        Producto p = productoValido();
        p.setCodigo("   ");
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarCodigoDe21Caracteres_noLlamaAlDao() {
        Producto p = productoValido();
        p.setCodigo(repetir('A', 21));
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarCodigoDe20Caracteres_esValido() {
        Producto p = productoValido();
        p.setCodigo(repetir('A', 20));

        servicio.registrar(p);

        assertEquals(1, productoDao.llamadasInsertar);
    }

    @Test
    public void registrarNombreVacio_noLlamaAlDao() {
        Producto p = productoValido();
        p.setNombre("");
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarNombreDe101Caracteres_noLlamaAlDao() {
        Producto p = productoValido();
        p.setNombre(repetir('N', 101));
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarNombreDe100Caracteres_esValido() {
        Producto p = productoValido();
        p.setNombre(repetir('N', 100));

        servicio.registrar(p);

        assertEquals(1, productoDao.llamadasInsertar);
    }

    @Test
    public void registrarDescripcionDe251Caracteres_noLlamaAlDao() {
        Producto p = productoValido();
        p.setDescripcion(repetir('D', 251));
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarDescripcionNula_esValida() {
        Producto p = productoValido();
        p.setDescripcion(null);

        servicio.registrar(p);

        assertEquals(1, productoDao.llamadasInsertar);
    }

    @Test
    public void registrarDescripcionDe250Caracteres_esValida() {
        Producto p = productoValido();
        p.setDescripcion(repetir('D', 250));

        servicio.registrar(p);

        assertEquals(1, productoDao.llamadasInsertar);
    }

    @Test
    public void registrarPrecioNulo_noLlamaAlDao() {
        Producto p = productoValido();
        p.setPrecioVenta(null);
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarPrecioCero_noLlamaAlDao() {
        Producto p = productoValido();
        p.setPrecioVenta(BigDecimal.ZERO);
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarPrecioNegativo_noLlamaAlDao() {
        Producto p = productoValido();
        p.setPrecioVenta(new BigDecimal("-1.00"));
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    @Test
    public void registrarSinCategoria_noLlamaAlDao() {
        Producto p = productoValido();
        p.setCategoria(null);

        assertDatoInvalidoSinLlamadasAlDao(p);
        assertEquals(0, categoriaDao.llamadasBuscar);
    }

    @Test
    public void registrarCategoriaConIdNoPositivo_noConsultaDaos() {
        Producto p = productoValido();
        Categoria c = new Categoria();
        c.setIdCategoria(0);
        p.setCategoria(c);

        assertDatoInvalidoSinLlamadasAlDao(p);
        assertEquals(0, categoriaDao.llamadasBuscar);
    }

    @Test
    public void registrarCategoriaInexistente_lanzaNoEncontrado404() {
        Producto p = productoValido();
        Categoria c = new Categoria();
        c.setIdCategoria(99);
        p.setCategoria(c);

        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.registrar(p));

        assertEquals(404, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    @Test
    public void registrarCategoriaInactiva_lanzaDatoInvalido400() {
        Producto p = productoValido();
        p.setCategoria(categoriaDao.almacen.get(2));

        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.registrar(p));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    @Test
    public void registrarProductoNuevoInactivo_noLlamaAlDao() {
        Producto p = productoValido();
        p.setActivo(false);
        assertDatoInvalidoSinLlamadasAlDao(p);
    }

    // ---------- Actualización ----------

    @Test
    public void actualizarProductoValido_llamaAlDao() {
        guardarProductoExistente();

        servicio.actualizar(productoValido());

        assertEquals(1, productoDao.llamadasActualizar);
    }

    @Test
    public void actualizarConIdNoPositivo_noLlamaAlDao() {
        Producto p = productoValido();
        p.setIdProducto(0);

        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.actualizar(p));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    @Test
    public void actualizarConDatosInvalidos_noLlamaAlDao() {
        Producto p = productoValido();
        p.setNombre("");

        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.actualizar(p));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    @Test
    public void actualizarProductoInexistente_lanzaNoEncontrado404() {
        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.actualizar(productoValido()));

        assertEquals(404, codigoHttp(e));
        assertEquals(0, productoDao.llamadasActualizar);
    }

    @Test
    public void actualizarConCategoriaInactiva_lanzaDatoInvalido() {
        guardarProductoExistente();

        Producto p = productoValido();
        p.setCategoria(categoriaDao.almacen.get(2));

        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.actualizar(p));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.llamadasActualizar);
    }

    @Test
    public void actualizarCuandoElDaoNoAfectaFilas_lanzaNoEncontrado() {
        guardarProductoExistente();
        productoDao.resultadoActualizar = false;

        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.actualizar(productoValido()));

        assertEquals(404, codigoHttp(e));
    }

    // ---------- Desactivación lógica ----------

    @Test
    public void desactivarProductoExistente_usaDesactivarLogico() {
        guardarProductoExistente();

        servicio.desactivar(10);

        assertEquals(1, productoDao.llamadasDesactivar);
        assertEquals(10, productoDao.ultimoIdDesactivado);
    }

    @Test
    public void desactivarConIdNoPositivo_noLlamaAlDao() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.desactivar(-5));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    @Test
    public void desactivarProductoInexistente_lanzaNoEncontrado404() {
        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.desactivar(10));

        assertEquals(404, codigoHttp(e));
        assertEquals(0, productoDao.llamadasDesactivar);
    }

    @Test
    public void desactivarCuandoElDaoNoAfectaFilas_lanzaNoEncontrado() {
        guardarProductoExistente();
        productoDao.resultadoDesactivar = false;

        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.desactivar(10));

        assertEquals(404, codigoHttp(e));
    }

    // ---------- Consulta por ID ----------

    @Test
    public void obtenerPorIdExistente_devuelveElProducto() {
        guardarProductoExistente();

        Producto p = servicio.obtenerPorId(10);

        assertNotNull(p);
        assertTrue(p.isActivo());
    }

    @Test
    public void obtenerPorIdInexistente_lanzaNoEncontrado() {
        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.obtenerPorId(10));

        assertEquals(404, codigoHttp(e));
    }

    @Test
    public void obtenerPorIdNoPositivo_noLlamaAlDao() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.obtenerPorId(0));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    // ---------- Listado paginado ----------

    @Test
    public void listarPaginado_enviaAlDaoLosParametrosRecibidos() {
        servicio.listarPaginado(2, 15, "polo", 1);

        assertEquals(1, productoDao.llamadasListarPaginado);
        assertEquals(2, productoDao.ultimaPagina);
        assertEquals(15, productoDao.ultimoTam);
        assertEquals("polo", productoDao.ultimoTexto);
        assertEquals(1, productoDao.ultimaCategoria);
    }

    @Test
    public void listarPaginado_conservaElObjetoPaginadoDelDao() {
        List<Producto> items = new ArrayList<>();
        items.add(productoValido());

        Paginado<Producto> esperado = Paginado.de(items, 1, 10, 1L);
        productoDao.paginadoARetornar = esperado;

        Paginado<Producto> resultado =
                servicio.listarPaginado(1, 10, null, 0);

        assertSame(esperado, resultado);
    }

    @Test
    public void listarPaginado_aceptaTextoNuloYCategoriaCero() {
        servicio.listarPaginado(1, 10, null, 0);

        assertNull(productoDao.ultimoTexto);
        assertEquals(0, productoDao.ultimaCategoria);
    }

    @Test
    public void listarPaginado_delegaPaginaNoPositivaAlDao() {
        servicio.listarPaginado(0, 10, null, 0);

        assertEquals(1, productoDao.llamadasListarPaginado);
        assertEquals(0, productoDao.ultimaPagina);
    }

    @Test
    public void listarPaginado_delegaTamanoNoPositivoAlDao() {
        servicio.listarPaginado(1, 0, null, 0);

        assertEquals(1, productoDao.llamadasListarPaginado);
        assertEquals(0, productoDao.ultimoTam);
    }

    @Test
    public void listarPaginadoConCategoriaNegativa_noLlamaAlDao() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.listarPaginado(1, 10, null, -1));

        assertEquals(400, codigoHttp(e));
        assertEquals(0, productoDao.totalLlamadas());
    }

    @Test
    public void listarPaginado_noConsultaElDaoDeCategorias() {
        servicio.listarPaginado(1, 10, "polo", 1);

        assertEquals(0, categoriaDao.llamadasBuscar);
    }

    // ---------- Stock bajo ----------

    @Test
    public void listarStockBajo_devuelveLaListaDelDao() {
        List<VarianteProducto> esperada = new ArrayList<>();
        productoDao.stockBajoARetornar = esperada;

        List<VarianteProducto> resultado = servicio.listarStockBajo();

        assertSame(esperada, resultado);
        assertEquals(1, productoDao.llamadasStockBajo);
    }

    // ---------- Propagación de excepciones ----------

    @Test
    public void registrar_propagaDuplicadoExceptionSinConvertirla() {
        DuplicadoException original = nuevaDuplicada();
        productoDao.fallaAlInsertar = original;

        DuplicadoException e = esperar(
                DuplicadoException.class,
                () -> servicio.registrar(productoValido()));

        assertSame(original, e);
        assertEquals(409, codigoHttp(e));
    }

    @Test
    public void registrar_propagaPersistenciaExceptionSinConvertirla() {
        PersistenciaException original = nuevaPersistencia();
        productoDao.fallaAlInsertar = original;

        PersistenciaException e = esperar(
                PersistenciaException.class,
                () -> servicio.registrar(productoValido()));

        assertSame(original, e);
        assertEquals(500, codigoHttp(e));
    }

    @Test
    public void desactivar_propagaPersistenciaExceptionDeBuscarPorId() {
        PersistenciaException original = nuevaPersistencia();
        productoDao.fallaAlBuscar = original;

        PersistenciaException e = esperar(
                PersistenciaException.class,
                () -> servicio.desactivar(10));

        assertSame(original, e);
        assertEquals(0, productoDao.llamadasDesactivar);
    }

    @Test
    public void listarPaginado_propagaPersistenciaException() {
        PersistenciaException original = nuevaPersistencia();
        productoDao.fallaAlListar = original;

        PersistenciaException e = esperar(
                PersistenciaException.class,
                () -> servicio.listarPaginado(1, 10, null, 0));

        assertSame(original, e);
    }

    @Test
    public void listarStockBajo_propagaPersistenciaException() {
        PersistenciaException original = nuevaPersistencia();
        productoDao.fallaEnStockBajo = original;

        PersistenciaException e = esperar(
                PersistenciaException.class,
                () -> servicio.listarStockBajo());

        assertSame(original, e);
    }
}
