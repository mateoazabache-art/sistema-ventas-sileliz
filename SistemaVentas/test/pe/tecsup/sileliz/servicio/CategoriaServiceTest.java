package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.excepcion.SilelizException;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.util.Paginado;

public class CategoriaServiceTest {

    private static class CategoriaDaoFalso extends CategoriaDao {

        final Map<Integer, Categoria> almacen = new HashMap<>();

        int llamadasInsertar;
        int llamadasActualizar;
        int llamadasDesactivar;
        int llamadasBuscar;
        int llamadasListar;

        boolean resultadoActualizar = true;
        boolean resultadoDesactivar = true;

        int ultimoIdDesactivado = -1;
        int ultimaPagina = -1;
        int ultimoTam = -1;
        String ultimoTexto;

        Paginado<Categoria> paginadoARetornar;

        @Override
        public Optional<Categoria> buscarPorId(int idCategoria) {
            llamadasBuscar++;
            return Optional.ofNullable(almacen.get(idCategoria));
        }

        @Override
        public Categoria insertar(Categoria categoria) {
            llamadasInsertar++;
            categoria.setIdCategoria(almacen.size() + 1);
            almacen.put(categoria.getIdCategoria(), categoria);
            return categoria;
        }

        @Override
        public boolean actualizar(Categoria categoria) {
            llamadasActualizar++;
            return resultadoActualizar;
        }

        @Override
        public boolean desactivar(int idCategoria) {
            llamadasDesactivar++;
            ultimoIdDesactivado = idCategoria;
            return resultadoDesactivar;
        }

        @Override
        public Paginado<Categoria> listarPaginado(
                int pagina, int tam, String texto) {
            llamadasListar++;
            ultimaPagina = pagina;
            ultimoTam = tam;
            ultimoTexto = texto;
            return paginadoARetornar;
        }

        int totalLlamadas() {
            return llamadasInsertar + llamadasActualizar
                    + llamadasDesactivar + llamadasBuscar
                    + llamadasListar;
        }
    }

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

    private static String repetir(char c, int veces) {
        return String.valueOf(c).repeat(veces);
    }

    private CategoriaDaoFalso dao;
    private CategoriaService servicio;

    @Before
    public void preparar() {
        dao = new CategoriaDaoFalso();
        servicio = new CategoriaService(dao);

        Categoria existente = new Categoria(1, "Polos", true);
        dao.almacen.put(1, existente);
    }

    private Categoria categoriaValida() {
        return new Categoria(0, "Vestidos", true);
    }

    // ---------- Constructor ----------

    @Test(expected = NullPointerException.class)
    public void constructorConDaoNulo_lanzaNullPointer() {
        new CategoriaService(null);
    }

    // ---------- Registro ----------

    @Test
    public void registrarCategoriaValida_insertaYDevuelveLaCategoria() {
        Categoria categoria = categoriaValida();

        Categoria resultado = servicio.registrar(categoria);

        assertSame(categoria, resultado);
        assertEquals(1, dao.llamadasInsertar);
    }

    @Test
    public void registrarCategoriaNula_lanzaDatoInvalido() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.registrar(null));

        assertEquals(400, e.getCodigoHttp());
        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void registrarNombreNulo_noLlamaAlDao() {
        Categoria categoria = categoriaValida();
        categoria.setNombre(null);

        esperar(DatoInvalidoException.class,
                () -> servicio.registrar(categoria));

        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void registrarNombreVacio_noLlamaAlDao() {
        Categoria categoria = categoriaValida();
        categoria.setNombre("   ");

        esperar(DatoInvalidoException.class,
                () -> servicio.registrar(categoria));

        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void registrarNombreDe60Caracteres_esValido() {
        Categoria categoria = categoriaValida();
        categoria.setNombre(repetir('C', 60));

        servicio.registrar(categoria);

        assertEquals(1, dao.llamadasInsertar);
    }

    @Test
    public void registrarNombreDe61Caracteres_noLlamaAlDao() {
        Categoria categoria = categoriaValida();
        categoria.setNombre(repetir('C', 61));

        esperar(DatoInvalidoException.class,
                () -> servicio.registrar(categoria));

        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void registrarCategoriaInactiva_laFuerzaComoActiva() {
        Categoria categoria = categoriaValida();
        categoria.setActivo(false);

        servicio.registrar(categoria);

        assertTrue(categoria.isActivo());
        assertEquals(1, dao.llamadasInsertar);
    }

    // ---------- Actualización ----------

    @Test
    public void actualizarCategoriaValida_llamaAlDao() {
        Categoria categoria = new Categoria(1, "Polos actualizados", true);

        servicio.actualizar(categoria);

        assertEquals(1, dao.llamadasBuscar);
        assertEquals(1, dao.llamadasActualizar);
    }

    @Test
    public void actualizarCategoriaNula_lanzaDatoInvalido() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.actualizar(null));

        assertEquals(400, e.getCodigoHttp());
        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void actualizarNombreVacio_noLlamaAlDao() {
        Categoria categoria = new Categoria(1, " ", true);

        esperar(DatoInvalidoException.class,
                () -> servicio.actualizar(categoria));

        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void actualizarNombreDe61Caracteres_noLlamaAlDao() {
        Categoria categoria = new Categoria(
                1, repetir('C', 61), true);

        esperar(DatoInvalidoException.class,
                () -> servicio.actualizar(categoria));

        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void actualizarConIdNoPositivo_noConsultaElDao() {
        Categoria categoria = new Categoria(0, "Vestidos", true);

        esperar(DatoInvalidoException.class,
                () -> servicio.actualizar(categoria));

        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void actualizarCategoriaInexistente_lanzaNoEncontrado404() {
        Categoria categoria = new Categoria(99, "Vestidos", true);

        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.actualizar(categoria));

        assertEquals(404, e.getCodigoHttp());
        assertEquals(0, dao.llamadasActualizar);
    }

    @Test
    public void actualizarCuandoDaoNoAfectaFilas_lanzaNoEncontrado() {
        dao.resultadoActualizar = false;
        Categoria categoria = new Categoria(1, "Polos nuevos", true);

        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.actualizar(categoria));

        assertEquals(404, e.getCodigoHttp());
        assertEquals(1, dao.llamadasActualizar);
    }

    // ---------- Desactivación lógica ----------

    @Test
    public void desactivarCategoriaExistente_llamaAlDao() {
        servicio.desactivar(1);

        assertEquals(1, dao.llamadasDesactivar);
        assertEquals(1, dao.ultimoIdDesactivado);
    }

    @Test
    public void desactivarConIdNoPositivo_noLlamaAlDao() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.desactivar(0));

        assertEquals(400, e.getCodigoHttp());
        assertEquals(0, dao.totalLlamadas());
    }

    @Test
    public void desactivarCategoriaInexistente_lanzaNoEncontrado404() {
        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.desactivar(99));

        assertEquals(404, e.getCodigoHttp());
        assertEquals(0, dao.llamadasDesactivar);
    }

    @Test
    public void desactivarCuandoDaoNoAfectaFilas_lanzaNoEncontrado() {
        dao.resultadoDesactivar = false;

        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.desactivar(1));

        assertEquals(404, e.getCodigoHttp());
        assertEquals(1, dao.llamadasDesactivar);
    }

    // ---------- Consulta por ID ----------

    @Test
    public void obtenerPorIdExistente_devuelveLaCategoria() {
        Categoria resultado = servicio.obtenerPorId(1);

        assertSame(dao.almacen.get(1), resultado);
        assertEquals(1, dao.llamadasBuscar);
    }

    @Test
    public void obtenerPorIdInexistente_lanzaNoEncontrado404() {
        NoEncontradoException e = esperar(
                NoEncontradoException.class,
                () -> servicio.obtenerPorId(99));

        assertEquals(404, e.getCodigoHttp());
    }

    @Test
    public void obtenerPorIdNoPositivo_noConsultaElDao() {
        DatoInvalidoException e = esperar(
                DatoInvalidoException.class,
                () -> servicio.obtenerPorId(-1));

        assertEquals(400, e.getCodigoHttp());
        assertEquals(0, dao.totalLlamadas());
    }

    // ---------- Listado paginado ----------

    @Test
    public void listarPaginado_enviaParametrosAlDao() {
        servicio.listarPaginado(2, 15, "polo");

        assertEquals(1, dao.llamadasListar);
        assertEquals(2, dao.ultimaPagina);
        assertEquals(15, dao.ultimoTam);
        assertEquals("polo", dao.ultimoTexto);
    }

    @Test
    public void listarPaginado_devuelveElResultadoDelDao() {
        Paginado<Categoria> esperado = Paginado.de(
                List.of(dao.almacen.get(1)), 1, 10, 1L);
        dao.paginadoARetornar = esperado;

        Paginado<Categoria> resultado =
                servicio.listarPaginado(1, 10, null);

        assertSame(esperado, resultado);
    }

    @Test
    public void listarPaginado_permiteTextoNulo() {
        servicio.listarPaginado(1, 10, null);

        assertEquals(1, dao.llamadasListar);
        assertEquals(null, dao.ultimoTexto);
    }
}