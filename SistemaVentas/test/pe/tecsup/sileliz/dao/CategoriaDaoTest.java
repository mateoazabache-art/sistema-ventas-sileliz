package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pe.tecsup.sileliz.excepcion.DuplicadoException;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.Paginado;

public class CategoriaDaoTest {

    private final CategoriaDao dao = new CategoriaDao();
    private Connection cn;

    @Before
    public void preparar() throws SQLException {
        cn = Conexion.getConnection();
        cn.setAutoCommit(false);
    }

    @After
    public void deshacer() throws SQLException {
        if (cn != null) {
            try {
                cn.rollback();
            } finally {
                cn.close();
            }
        }
    }

    @Test
    public void buscarPorIdDevuelveCategoriaExistente() throws SQLException {
        int id = idPorNombre("Jeans");

        var resultado = dao.buscarPorId(cn, id);

        assertTrue(resultado.isPresent());
        assertEquals("Jeans", resultado.get().getNombre());
        assertTrue(resultado.get().isActivo());
    }

    @Test
    public void buscarPorIdDevuelveVacioCuandoNoExiste() throws SQLException {
        var resultado = dao.buscarPorId(cn, -1);

        assertFalse(resultado.isPresent());
    }

    @Test
    public void listarPaginadoDevuelveCategoriasOrdenadas() throws SQLException {
        Paginado<Categoria> resultado = dao.listarPaginado(cn, 1, 50, "");

        List<Categoria> categorias = resultado.items();

        assertTrue(categorias.size() >= 5);

        for (int i = 1; i < categorias.size(); i++) {
            String anterior = categorias.get(i - 1).getNombre();
            String actual = categorias.get(i).getNombre();

            assertTrue(anterior.compareToIgnoreCase(actual) <= 0);
        }
    }

    @Test
    public void listarPaginadoBuscaSinDistinguirMayusculas() throws SQLException {
        Paginado<Categoria> resultado =
                dao.listarPaginado(cn, 1, 50, "jEaNs");

        assertEquals(1L, resultado.total());
        assertEquals("Jeans", resultado.items().get(0).getNombre());
    }

    @Test
    public void listarPaginadoDivideResultadosEnPaginas() throws SQLException {
        Paginado<Categoria> primera =
                dao.listarPaginado(cn, 1, 2, "");

        Paginado<Categoria> segunda =
                dao.listarPaginado(cn, 2, 2, "");

        assertEquals(1, primera.pagina());
        assertEquals(2, primera.tam());
        assertEquals(2, primera.items().size());

        assertEquals(2, segunda.pagina());
        assertEquals(2, segunda.items().size());

        assertTrue(primera.total() >= 5);
        assertEquals(primera.total(), segunda.total());

        assertTrue(
                primera.items().get(1).getIdCategoria()
                != segunda.items().get(0).getIdCategoria()
        );
    }

    @Test
    public void insertarGeneraIdYGuardaCategoria() throws SQLException {
        Categoria categoria =
                new Categoria(0, "Categoria de prueba", true);

        dao.insertar(cn, categoria);

        assertTrue(categoria.getIdCategoria() > 0);

        var resultado =
                dao.buscarPorId(cn, categoria.getIdCategoria());

        assertTrue(resultado.isPresent());
        assertEquals(
                "Categoria de prueba",
                resultado.get().getNombre()
        );
        assertTrue(resultado.get().isActivo());
    }

    @Test
    public void actualizarModificaNombre() throws SQLException {
        int id = idPorNombre("Jeans");
        Categoria categoria = dao.buscarPorId(cn, id).get();

        categoria.setNombre("Jeans actualizados");

        assertTrue(dao.actualizar(cn, categoria));

        var resultado = dao.buscarPorId(cn, id);

        assertTrue(resultado.isPresent());
        assertEquals(
                "Jeans actualizados",
                resultado.get().getNombre()
        );
    }

    @Test
    public void actualizarDevuelveFalseCuandoNoExiste() throws SQLException {
        Categoria categoria = new Categoria(-1, "Categoria inexistente", true);

        assertFalse(dao.actualizar(cn, categoria));
    }

    @Test
    public void desactivarCambiaEstadoSinEliminarCategoria() throws SQLException {
        int id = idPorNombre("Jeans");

        assertTrue(dao.desactivar(cn, id));

        var resultado = dao.buscarPorId(cn, id);

        assertTrue(resultado.isPresent());
        assertFalse(resultado.get().isActivo());
    }

    @Test
    public void desactivarDevuelveFalseCuandoNoExiste() throws SQLException {
        assertFalse(dao.desactivar(cn, -1));
    }

    @Test
    public void insertarCategoriaDuplicadaLanzaDuplicadoException() {
        Categoria categoria = new Categoria(0, "Jeans", true);

        try {
            dao.insertar(categoria);
            fail("Debio lanzar DuplicadoException.");
        } catch (DuplicadoException e) {
            assertTrue(e.getMessage().contains("Ya existe una categoría"));
        }
    }

    private int idPorNombre(String nombre) throws SQLException {
        String sql = "SELECT id_categoria FROM categoria WHERE nombre = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_categoria");
                }
            }
        }

        throw new AssertionError("No existe la categoria: " + nombre);
    }
}
