package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.After;
import org.junit.Test;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.DuplicadoException;
import pe.tecsup.sileliz.modelo.Cliente;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Pruebas de INTEGRACION de ClienteDao contra la base local.
 * Antes: ventas_db con 01_crear_tablas y 02_datos_prueba, y config.properties.
 * Lo que crean lo borran al final (@After): no dejan basura.
 */
public class ClienteDaoTest {

    private static final String DNI_PRUEBA = "79888888";
    private final ClienteDao dao = new ClienteDao();

    @After
    public void limpiar() throws SQLException {
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps1 = cn.prepareStatement(
                     "DELETE FROM cliente WHERE id_persona IN (SELECT id_persona FROM persona WHERE dni = ?)");
             PreparedStatement ps2 = cn.prepareStatement("DELETE FROM persona WHERE dni = ?")) {
            ps1.setString(1, DNI_PRUEBA);
            ps1.executeUpdate();
            ps2.setString(1, DNI_PRUEBA);
            ps2.executeUpdate();
        }
    }

    @Test
    public void buscaPorDniUnClienteDeLosDatosDePrueba() {
        Cliente c = dao.buscarPorDni("70000001").orElseThrow();
        assertEquals("María Elena", c.getNombres());
        assertEquals("Clienta frecuente", c.getObservaciones());
        assertEquals("CLIENTE", c.getTipo());
    }

    @Test
    public void unDniQueNoExisteDevuelveVacio() {
        assertFalse(dao.buscarPorDni("79999990").isPresent());
    }

    @Test
    public void unUsuarioNoApareceComoCliente() {
        // 70000101 es la persona del admin: es persona, pero NO es cliente
        assertFalse(dao.buscarPorDni("70000101").isPresent());
    }

    @Test
    public void listaPaginadaConBuscador() {
        Paginado<Cliente> todos = dao.listarPaginado(1, 4, "");
        assertEquals(10, todos.total());
        assertEquals(4, todos.items().size());
        assertEquals(3, todos.paginas());

        Paginado<Cliente> filtrado = dao.listarPaginado(1, 10, "castillo");
        assertEquals(1, filtrado.total());
        assertEquals("70000001", filtrado.items().get(0).getDni());
    }

    @Test
    public void insertaActualizaYDesactiva() {
        Cliente nuevo = new Cliente("Prueba", "Integración", DNI_PRUEBA);
        nuevo.setTelefono("");          // vacio -> NULL (no falla el CHECK)
        nuevo.setObservaciones("Creado por ClienteDaoTest");
        dao.insertar(nuevo);
        assertTrue(nuevo.getIdPersona() > 0);

        nuevo.setTelefono("987654321");
        nuevo.setObservaciones("Actualizado");
        assertTrue(dao.actualizar(nuevo));

        Cliente leido = dao.buscarPorId(nuevo.getIdPersona()).orElseThrow();
        assertEquals("987654321", leido.getTelefono());
        assertEquals("Actualizado", leido.getObservaciones());

        assertTrue(dao.desactivar(nuevo.getIdPersona()));
        assertFalse(dao.buscarPorId(nuevo.getIdPersona()).orElseThrow().isActivo());
    }

    @Test
    public void dniRepetidoDa409() {
        Cliente repetido = new Cliente("Otra", "Persona", "70000001");
        try {
            dao.insertar(repetido);
            fail("Debio lanzar DuplicadoException");
        } catch (DuplicadoException e) {
            assertEquals(409, e.getCodigoHttp());
            assertEquals("Ya existe una persona con ese DNI.", e.getMessage());
        }
    }

    @Test
    public void telefonoInvalidoDa400() {
        Cliente malo = new Cliente("Tel", "Malo", DNI_PRUEBA);
        malo.setTelefono("812345678");   // no empieza con 9
        try {
            dao.insertar(malo);
            fail("Debio lanzar DatoInvalidoException");
        } catch (DatoInvalidoException e) {
            assertEquals(400, e.getCodigoHttp());
        }
    }

    @Test
    public void actualizarODesactivarUnoQueNoExisteDevuelveFalse() {
        Cliente fantasma = new Cliente("No", "Existe", "79999991");
        fantasma.setIdPersona(999999);
        assertFalse(dao.actualizar(fantasma));
        assertFalse(dao.desactivar(999999));
    }
}
