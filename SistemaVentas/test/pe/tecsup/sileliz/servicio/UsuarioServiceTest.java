package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.dao.UsuarioDao;
import pe.tecsup.sileliz.excepcion.AccesoDenegadoException;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Rol;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.UsuarioSolicitud;
import pe.tecsup.sileliz.util.Clave;

/**
 * Pruebas UNITARIAS de UsuarioService (CU-02): validacion, RN-11 y los
 * permisos. No usan la base: un DAO falso recuerda lo que recibio
 * (UsuarioDaoTest ya prueba el SQL de verdad).
 */
public class UsuarioServiceTest {

    /** DAO falso: no guarda nada, solo recuerda lo que recibio. */
    private static class UsuarioDaoFalso extends UsuarioDao {
        Usuario recibido;
        String hashRecibido;
        Integer desactivado;
        boolean existe = true;

        @Override
        public Usuario insertar(Usuario u) {
            recibido = u;
            u.setIdPersona(99);
            return u;
        }

        @Override
        public boolean actualizar(Usuario u) {
            recibido = u;
            return existe;
        }

        @Override
        public Optional<Usuario> buscarPorId(int id) {
            return existe ? Optional.ofNullable(recibido) : Optional.empty();
        }

        @Override
        public boolean cambiarClave(int id, String hash) {
            hashRecibido = hash;
            return existe;
        }

        @Override
        public boolean desactivar(int id) {
            desactivado = id;
            return existe;
        }

        @Override
        public boolean activar(int id) {
            return existe;
        }
    }

    private UsuarioDaoFalso dao;
    private UsuarioService servicio;
    private Usuario admin;
    private Usuario vendedor;

    @Before
    public void preparar() {
        dao = new UsuarioDaoFalso();
        servicio = new UsuarioService(dao);
        admin = new Usuario("Sofía", "Administradora", "70000011", "admin", Rol.ADMINISTRADOR);
        admin.setIdPersona(11);
        vendedor = new Usuario("Valeria", "Vendedora", "70000012", "vendedor", Rol.VENDEDOR);
        vendedor.setIdPersona(12);
    }

    /** Una vendedora nueva correcta; cada prueba le cambia un dato. */
    private UsuarioSolicitud valida() {
        UsuarioSolicitud s = new UsuarioSolicitud();
        s.setNombres("  Rosa ");
        s.setApellidos("Quispe Ñique");
        s.setDni("71234567");
        s.setTelefono("912345678");
        s.setCorreo("");
        s.setNombreUsuario(" rosa.q ");
        s.setRol("vendedor");
        s.setClave("Tienda2026");
        return s;
    }

    /** registrar() debe rechazar con 400 sin llegar al DAO. */
    private void debeRechazar(UsuarioSolicitud s, String parteDelMensaje) {
        try {
            servicio.registrar(s, admin);
            fail("Debía rechazar el usuario");
        } catch (DatoInvalidoException e) {
            assertEquals(400, e.getCodigoHttp());
            assertTrue("Mensaje inesperado: " + e.getMessage(), e.getMessage().contains(parteDelMensaje));
            assertNull("No debía llegar al DAO", dao.recibido);
        }
    }

    // ---------- registrar ----------

    @Test
    public void registraUnaVendedoraConHashActivaYDatosLimpios() {
        Usuario u = servicio.registrar(valida(), admin);
        assertEquals(99, u.getIdPersona());
        assertEquals(Rol.VENDEDOR, u.getRol());
        assertTrue(u.isActivo());
        assertEquals("Rosa", u.getNombres());
        assertEquals("rosa.q", u.getNombreUsuario());
        assertNull("Opcional vacío debe quedar null", u.getCorreo());
        // RN-11: se guarda el hash PBKDF2, nunca la clave en texto
        assertFalse(u.getClaveHash().contains("Tienda2026"));
        assertTrue(Clave.verificar("Tienda2026".toCharArray(), u.getClaveHash()));
    }

    @Test
    public void rechazaClaveCortaOSinNumeros() {
        UsuarioSolicitud s = valida();
        s.setClave("Tienda");
        debeRechazar(s, "al menos 8 caracteres");
        s.setClave("SoloLetras");
        debeRechazar(s, "letras y números");
        s.setClave(null);
        debeRechazar(s, "Escriba la clave");
    }

    @Test
    public void rechazaRolQueNoExiste() {
        UsuarioSolicitud s = valida();
        s.setRol("JEFE");
        debeRechazar(s, "rol válido");
        s.setRol(null);
        debeRechazar(s, "rol válido");
    }

    @Test
    public void rechazaNombreDeUsuarioInvalido() {
        UsuarioSolicitud s = valida();
        s.setNombreUsuario("ro sa");
        debeRechazar(s, "4 a 30 caracteres");
        s.setNombreUsuario("ros");
        debeRechazar(s, "4 a 30 caracteres");
        s.setNombreUsuario("rosá");
        debeRechazar(s, "4 a 30 caracteres");
    }

    @Test
    public void rechazaDniTelefonoYCorreoInvalidos() {
        UsuarioSolicitud s = valida();
        s.setDni("123");
        debeRechazar(s, "DNI");
        s = valida();
        s.setTelefono("812345678");
        debeRechazar(s, "teléfono");
        s = valida();
        s.setCorreo("rosa@");
        debeRechazar(s, "correo");
    }

    @Test
    public void elVendedorNoPuedeCrearUsuarios() {
        try {
            servicio.registrar(valida(), vendedor);
            fail("Debía responder 403");
        } catch (AccesoDenegadoException e) {
            assertEquals(403, e.getCodigoHttp());
            assertNull(dao.recibido);
        }
    }

    // ---------- actualizar ----------

    @Test
    public void elAdminCambiaElRolDeOtroUsuario() {
        UsuarioSolicitud s = valida();
        s.setRol("ADMINISTRADOR");
        Usuario u = servicio.actualizar(12, s, admin);
        assertEquals(12, u.getIdPersona());
        assertEquals(Rol.ADMINISTRADOR, u.getRol());
    }

    @Test
    public void elAdminNoPuedeQuitarseSuPropioRol() {
        UsuarioSolicitud s = valida();
        s.setRol("VENDEDOR");
        try {
            servicio.actualizar(admin.getIdPersona(), s, admin);
            fail("Debía rechazar");
        } catch (DatoInvalidoException e) {
            assertTrue(e.getMessage().contains("rol de administrador"));
            assertNull(dao.recibido);
        }
    }

    @Test(expected = NoEncontradoException.class)
    public void actualizarUnUsuarioQueNoExisteDa404() {
        dao.existe = false;
        servicio.actualizar(500, valida(), admin);
    }

    // ---------- clave, desactivar, activar ----------

    @Test
    public void restablecerClaveGuardaUnHashNuevo() {
        servicio.restablecerClave(12, "Nueva2026", admin);
        assertTrue(Clave.verificar("Nueva2026".toCharArray(), dao.hashRecibido));
    }

    @Test
    public void restablecerClaveExigeLaPolitica() {
        try {
            servicio.restablecerClave(12, "12345678", admin);
            fail("Debía rechazar una clave sin letras");
        } catch (DatoInvalidoException e) {
            assertNull(dao.hashRecibido);
        }
    }

    @Test
    public void elAdminNoPuedeDesactivarseASiMismo() {
        try {
            servicio.desactivar(admin.getIdPersona(), admin);
            fail("Debía rechazar");
        } catch (DatoInvalidoException e) {
            assertTrue(e.getMessage().contains("propio usuario"));
            assertNull(dao.desactivado);
        }
    }

    @Test
    public void desactivaAOtroUsuario() {
        servicio.desactivar(12, admin);
        assertEquals(Integer.valueOf(12), dao.desactivado);
    }

    @Test(expected = NoEncontradoException.class)
    public void activarUnUsuarioQueNoExisteDa404() {
        dao.existe = false;
        servicio.activar(500, admin);
    }
}
