package pe.tecsup.sileliz.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.Test;
import pe.tecsup.sileliz.excepcion.DuplicadoException;
import pe.tecsup.sileliz.modelo.Rol;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.util.Clave;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Pruebas de INTEGRACION de UsuarioDao contra la base local.
 *
 * Antes de correrlas: ventas_db con 00_roles, 01_crear_tablas y
 * 02_datos_prueba ejecutados, y config.properties con su clave.
 * No dejan basura en la base: lo que cambian lo devuelven a su estado.
 */
public class UsuarioDaoTest {

    private final UsuarioDao dao = new UsuarioDao();

    @Test
    public void encuentraAlAdminDeLosDatosDePrueba() {
        Usuario admin = dao.buscarPorNombreUsuario("admin").orElseThrow();
        assertEquals(Rol.ADMINISTRADOR, admin.getRol());
        assertEquals("70000101", admin.getDni());
        assertTrue(admin.isActivo());
        assertFalse(admin.estaBloqueado());
    }

    @Test
    public void laClaveGuardadaEnLaBaseSeVerifica() {
        Usuario admin = dao.buscarPorNombreUsuario("admin").orElseThrow();
        assertTrue(Clave.verificar("Admin2026".toCharArray(), admin.getClaveHash()));
        assertFalse(Clave.verificar("otra".toCharArray(), admin.getClaveHash()));
    }

    @Test
    public void unUsuarioQueNoExisteDevuelveVacio() {
        Optional<Usuario> nadie = dao.buscarPorNombreUsuario("no_existe_xyz");
        assertFalse(nadie.isPresent());
    }

    @Test
    public void guardaYRestableceLosIntentosFallidos() {
        Usuario v = dao.buscarPorNombreUsuario("vendedor").orElseThrow();
        try {
            v.setIntentosFallidos(3);
            v.setBloqueadoHasta(LocalDateTime.now().plusMinutes(15).truncatedTo(ChronoUnit.SECONDS));
            dao.guardarIntentos(v);

            Usuario leido = dao.buscarPorId(v.getIdPersona()).orElseThrow();
            assertEquals(3, leido.getIntentosFallidos());
            assertTrue(leido.estaBloqueado());
        } finally {
            v.reiniciarIntentos();
            dao.guardarIntentos(v);   // deja la base como estaba
        }
        assertNull(dao.buscarPorId(v.getIdPersona()).orElseThrow().getBloqueadoHasta());
    }

    @Test
    public void listaPaginadaConBuscador() {
        Paginado<Usuario> todos = dao.listarPaginado(1, 10, "");
        assertEquals(2, todos.total());

        Paginado<Usuario> filtrado = dao.listarPaginado(1, 10, "vende");
        assertEquals(1, filtrado.total());
        assertEquals("vendedor", filtrado.items().get(0).getNombreUsuario());
    }

    /**
     * La prueba mas importante: la transaccion.
     * Se intenta crear un usuario con nombre "admin" (ya existe). El INSERT en
     * persona funciona, pero el de usuario falla: el rollback debe borrar
     * tambien la persona. Si no, quedaria una persona "huerfana".
     */
    @Test
    public void siFallaElSegundoInsertNoQuedaUnaPersonaSuelta() throws SQLException {
        String dniPrueba = "79999999";
        Usuario repetido = new Usuario("Prueba", "Transaccion", dniPrueba, "admin", Rol.VENDEDOR);
        repetido.setClaveHash(Clave.generar("Prueba2026".toCharArray()));

        try {
            dao.insertar(repetido);
            fail("Debio lanzar DuplicadoException");
        } catch (DuplicadoException e) {
            assertEquals(409, e.getCodigoHttp());
            assertNotNull(e.getMessage());
        }
        assertEquals("El rollback debio borrar la persona", 0, contarPersonasConDni(dniPrueba));
    }

    private int contarPersonasConDni(String dni) throws SQLException {
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT count(*) FROM persona WHERE dni = ?")) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
