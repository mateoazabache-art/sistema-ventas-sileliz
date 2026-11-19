package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.dao.ClienteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Cliente;

/**
 * Pruebas UNITARIAS de ClienteService: la validacion del servidor.
 *
 * No usan la base: un DAO falso recuerda que cliente le pasaron
 * (ClienteDaoTest ya prueba el SQL de verdad).
 */
public class ClienteServiceTest {

    /** DAO falso: no guarda nada, solo recuerda lo que recibio. */
    private static class ClienteDaoFalso extends ClienteDao {
        Cliente recibido;
        boolean existe = true;

        @Override
        public Cliente insertar(Cliente c) {
            recibido = c;
            c.setIdPersona(99);
            return c;
        }

        @Override
        public boolean actualizar(Cliente c) {
            recibido = c;
            return existe;
        }
    }

    private ClienteDaoFalso dao;
    private ClienteService servicio;

    @Before
    public void preparar() {
        dao = new ClienteDaoFalso();
        servicio = new ClienteService(dao);
    }

    /** Un cliente correcto; cada prueba le cambia un dato. */
    private Cliente valido() {
        Cliente c = new Cliente();
        c.setNombres("Ana");
        c.setApellidos("Ñañez");
        c.setDni("79111111");
        c.setTelefono("987654321");
        c.setCorreo("ana@correo.com");
        return c;
    }

    /** Comprueba que registrar() rechace el cliente con DatoInvalidoException y no llame al DAO. */
    private void debeRechazar(Cliente c, String parteDelMensaje) {
        try {
            servicio.registrar(c);
            fail("Debía rechazar el cliente");
        } catch (DatoInvalidoException e) {
            assertTrue("Mensaje inesperado: " + e.getMessage(), e.getMessage().contains(parteDelMensaje));
            assertNull("No debía llegar al DAO", dao.recibido);
        }
    }

    @Test
    public void registraUnClienteValidoActivo() {
        Cliente r = servicio.registrar(valido());
        assertEquals(99, r.getIdPersona());
        assertTrue(r.isActivo());
    }

    @Test
    public void quitaEspaciosYConvierteOpcionalesVaciosEnNull() {
        Cliente c = valido();
        c.setNombres("  Ana  ");
        c.setDni(" 79111111 ");
        c.setTelefono("   ");
        c.setCorreo("");
        c.setDireccion("  ");
        servicio.registrar(c);
        assertEquals("Ana", dao.recibido.getNombres());
        assertEquals("79111111", dao.recibido.getDni());
        assertNull(dao.recibido.getTelefono());
        assertNull(dao.recibido.getCorreo());
        assertNull(dao.recibido.getDireccion());
    }

    @Test
    public void rechazaCorreoInvalido() {
        Cliente c = valido();
        c.setCorreo("ana@");
        debeRechazar(c, "correo");
    }

    @Test
    public void rechazaNombresDemasiadoLargos() {
        Cliente c = valido();
        c.setNombres("A".repeat(81));
        debeRechazar(c, "Nombres");
    }

    @Test
    public void aceptaElLargoMaximoExacto() {
        Cliente c = valido();
        c.setObservaciones("x".repeat(200));
        servicio.registrar(c);
        assertEquals(200, dao.recibido.getObservaciones().length());
    }

    @Test
    public void rechazaObservacionesDemasiadoLargas() {
        Cliente c = valido();
        c.setObservaciones("x".repeat(201));
        debeRechazar(c, "Observaciones");
    }

    @Test
    public void rechazaDniDeSieteDigitos() {
        Cliente c = valido();
        c.setDni("7911111");
        debeRechazar(c, "8 dígitos");
    }

    @Test
    public void rechazaNombresSoloConEspacios() {
        Cliente c = valido();
        c.setNombres("   ");
        debeRechazar(c, "nombres");
    }

    @Test
    public void rechazaClienteNulo() {
        debeRechazar(null, "Faltan");
    }

    @Test
    public void actualizarUsaElIdDeLaUrlYValida() {
        Cliente c = valido();
        c.setIdPersona(5);          // lo que venga en el JSON no cuenta
        c.setCorreo("malo");
        try {
            servicio.actualizar(7, c);
            fail("Debía rechazar el correo");
        } catch (DatoInvalidoException e) {
            assertNull(dao.recibido);
        }
    }

    @Test(expected = NoEncontradoException.class)
    public void actualizarClienteQueNoExiste() {
        dao.existe = false;
        servicio.actualizar(7, valido());
    }
}
