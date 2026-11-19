package pe.tecsup.sileliz.servicio;

import java.util.Arrays;
import pe.tecsup.sileliz.dao.UsuarioDao;
import pe.tecsup.sileliz.excepcion.AutenticacionException;
import pe.tecsup.sileliz.excepcion.CuentaBloqueadaException;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.util.Clave;

/**
 * Login con bloqueo tras 5 intentos fallidos (RN-11, CU-01).
 * Mensaje generico "Usuario o clave incorrectos": nunca revela si el usuario existe.
 */
public class AuthService {

    /** Hash de relleno: se verifica igual aunque el usuario no exista,
     *  para que la respuesta tarde lo mismo y no delate que usuarios existen. */
    private static final String HASH_FALSO = Clave.generar("relleno-sin-uso".toCharArray());

    private final UsuarioDao usuarioDao;

    public AuthService() {
        this(new UsuarioDao());
    }

    /** Constructor para las pruebas JUnit. */
    public AuthService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public Usuario iniciarSesion(String nombreUsuario, char[] clave) {
        try {
            if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null || clave.length == 0) {
                throw new AutenticacionException();
            }

            // 1. Buscar. Si no existe o esta inactivo: 401 generico
            Usuario u = usuarioDao.buscarPorNombreUsuario(nombreUsuario.trim()).orElse(null);
            if (u == null || !u.isActivo()) {
                Clave.verificar(clave, HASH_FALSO);   // gastar el mismo tiempo
                throw new AutenticacionException();
            }

            // 2. Bloqueado y todavia no vence: 423
            if (u.estaBloqueado()) {
                throw new CuentaBloqueadaException();
            }

            // 4. Clave correcta: borrar intentos (solo si habia) y entrar
            if (Clave.verificar(clave, u.getClaveHash())) {
                if (u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null) {
                    u.reiniciarIntentos();
                    usuarioDao.guardarIntentos(u);
                }
                return u;
            }
            
            // 3. Clave incorrecta  >>> TU TURNO <<<
            //    a) sumar el intento fallido (mira los metodos de Usuario)
            //    b) guardar el cambio en la base
            //    c) si con este intento quedo bloqueado -> 423; si no -> 401
            // 3. Clave incorrecta
            boolean bloqueado = u.registrarIntentoFallido();
            usuarioDao.guardarIntentos(u);

            if (bloqueado) {
                throw new CuentaBloqueadaException();
            } else {
                throw new AutenticacionException();
            }

        } finally {
            Arrays.fill(clave == null ? new char[0] : clave, '\0');   // borrar la clave de la memoria
        }
    }
}
