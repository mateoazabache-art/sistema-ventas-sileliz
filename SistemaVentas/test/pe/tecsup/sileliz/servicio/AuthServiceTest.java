package pe.tecsup.sileliz.servicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import pe.tecsup.sileliz.dao.UsuarioDao;
import pe.tecsup.sileliz.excepcion.AutenticacionException;
import pe.tecsup.sileliz.excepcion.CuentaBloqueadaException;
import pe.tecsup.sileliz.modelo.Rol;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.util.Clave;

/**
 * Pruebas UNITARIAS de AuthService (casos CP-01, CP-02 y CP-03 del plan).
 *
 * No usan la base de datos: usan un "DAO falso" que guarda los usuarios
 * en un Map. Asi la prueba solo depende de la logica del login, es rapida
 * y funciona en cualquier PC. (UsuarioDaoTest ya prueba la base de verdad.)
 */
public class AuthServiceTest {

    /** DAO falso: hereda de UsuarioDao y reemplaza solo los 2 metodos que usa el login. */
    private static class UsuarioDaoFalso extends UsuarioDao {
        final Map<String, Usuario> usuarios = new HashMap<>();
        int vecesGuardado = 0;

        @Override
        public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
            return Optional.ofNullable(usuarios.get(nombreUsuario));
        }

        @Override
        public void guardarIntentos(Usuario u) {
            vecesGuardado++;   // en memoria el objeto ya esta cambiado
        }
    }

    private UsuarioDaoFalso dao;
    private AuthService auth;
    private Usuario ana;

    @Before
    public void preparar() {
        dao = new UsuarioDaoFalso();
        ana = new Usuario("Ana", "Ruiz", "70000101", "ana", Rol.VENDEDOR);
        ana.setIdPersona(1);
        ana.setClaveHash(Clave.generar("Venta2026".toCharArray()));
        dao.usuarios.put("ana", ana);
        auth = new AuthService(dao);
    }

    private char[] c(String texto) {
        return texto.toCharArray();
    }

    @Test
    public void cp01ClaveCorrectaEntra() {
        Usuario u = auth.iniciarSesion("ana", c("Venta2026"));
        assertNotNull(u);
        assertEquals("ana", u.getNombreUsuario());
    }

    @Test
    public void cp02ClaveIncorrectaDa401YSumaUnIntento() {
        try {
            auth.iniciarSesion("ana", c("mala1234"));
            fail("Debio lanzar AutenticacionException");
        } catch (AutenticacionException e) {
            assertEquals(401, e.getCodigoHttp());
        }
        assertEquals(1, ana.getIntentosFallidos());
        assertEquals("El intento debe guardarse en la base", 1, dao.vecesGuardado);
    }

    @Test
    public void usuarioQueNoExisteDaElMismoMensajeQueClaveIncorrecta() {
        String msgNoExiste = null;
        String msgClaveMala = null;
        try {
            auth.iniciarSesion("nadie", c("Venta2026"));
        } catch (AutenticacionException e) {
            msgNoExiste = e.getMessage();
        }
        try {
            auth.iniciarSesion("ana", c("mala1234"));
        } catch (AutenticacionException e) {
            msgClaveMala = e.getMessage();
        }
        assertEquals("No debe revelar si el usuario existe", msgClaveMala, msgNoExiste);
    }

    @Test
    public void cp03QuintoIntentoBloqueaCon423() {
        for (int i = 1; i <= 4; i++) {
            try {
                auth.iniciarSesion("ana", c("mala1234"));
                fail();
            } catch (AutenticacionException e) {
                // intentos 1 a 4: 401
            }
        }
        try {
            auth.iniciarSesion("ana", c("mala1234"));
            fail("El quinto intento debio bloquear");
        } catch (CuentaBloqueadaException e) {
            assertEquals(423, e.getCodigoHttp());
        }
        assertTrue(ana.estaBloqueado());
    }

    @Test
    public void bloqueadoNoEntraNiConLaClaveCorrecta() {
        ana.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        try {
            auth.iniciarSesion("ana", c("Venta2026"));
            fail("Un usuario bloqueado no debe entrar");
        } catch (CuentaBloqueadaException e) {
            assertEquals(423, e.getCodigoHttp());
        }
    }

    @Test
    public void cuandoVenceElBloqueoPuedeEntrarYSeLimpia() {
        ana.setIntentosFallidos(3);
        ana.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));   // ya vencio
        Usuario u = auth.iniciarSesion("ana", c("Venta2026"));
        assertEquals(0, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
    }

    @Test
    public void usuarioInactivoNoEntra() {
        ana.setActivo(false);
        try {
            auth.iniciarSesion("ana", c("Venta2026"));
            fail("Un usuario desactivado no debe entrar");
        } catch (AutenticacionException e) {
            assertEquals(401, e.getCodigoHttp());
        }
    }

    @Test
    public void laClaveSeBorraDeLaMemoria() {
        char[] clave = c("Venta2026");
        auth.iniciarSesion("ana", clave);
        for (char ch : clave) {
            assertEquals('\0', ch);
        }
    }
}
