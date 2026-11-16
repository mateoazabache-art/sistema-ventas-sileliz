package pe.tecsup.sileliz.servicio;

import java.util.Arrays;
import pe.tecsup.sileliz.dao.UsuarioDao;
import pe.tecsup.sileliz.excepcion.AccesoDenegadoException;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Rol;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.UsuarioSolicitud;
import pe.tecsup.sileliz.util.Clave;
import pe.tecsup.sileliz.util.Paginado;
import pe.tecsup.sileliz.util.Validador;

/**
 * Reglas de negocio de usuarios (CU-02, tarea 9 del plan): alta, edicion,
 * desactivacion y restablecimiento de clave. Solo el administrador.
 *
 *   - SesionFilter ya responde 403 al vendedor en /api/usuarios; aqui se vuelve
 *     a comprobar (defensa en profundidad, igual que IngresoService).
 *   - RN-04: DNI de 8 digitos y telefono de 9 que empieza con 9 (Persona.validar).
 *   - RN-05: un usuario no se borra, se desactiva (sus ventas lo siguen nombrando).
 *   - RN-11: la clave tiene al menos 8 caracteres con letras y numeros
 *     (Clave.cumplePolitica). Se guarda solo el hash PBKDF2 (Clave.generar).
 *   - El administrador no puede desactivarse ni quitarse el rol a si mismo:
 *     asi la tienda nunca se queda sin un administrador activo.
 */
public class UsuarioService {

    /** Largos de las columnas (01_crear_tablas.sql). */
    private static final int LARGO_NOMBRES = 80;
    private static final int LARGO_CORREO = 120;
    private static final int LARGO_DIRECCION = 150;

    /** Nombre de usuario: 4 a 30 letras sin tilde, numeros, punto o guion bajo. */
    private static final String PATRON_USUARIO = "[A-Za-z0-9._]{4,30}";

    private final UsuarioDao dao;

    public UsuarioService() {
        this(new UsuarioDao());
    }

    /** Constructor para pruebas JUnit (se le pasa un DAO falso). */
    public UsuarioService(UsuarioDao dao) {
        this.dao = dao;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Paginado<Usuario> listar(int pagina, int tam, String texto, Usuario actual) {
        exigirAdministrador(actual);
        return dao.listarPaginado(pagina, tam, texto);
    }

    public Usuario buscarPorId(int id, Usuario actual) {
        exigirAdministrador(actual);
        return buscar(id);
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    /** Valida, genera el hash de la clave y guarda un usuario nuevo (activo). */
    public Usuario registrar(UsuarioSolicitud s, Usuario actual) {
        exigirAdministrador(actual);
        if (s == null) {
            throw new DatoInvalidoException("Faltan los datos del usuario.");
        }
        Usuario u = armar(s);
        String hash = hashDe(s.getClave());
        u.setClaveHash(hash);
        u.setActivo(true);
        return dao.insertar(u);
    }

    /** Valida y actualiza datos, nombre de usuario y rol (no la clave). El id viene de la URL. */
    public Usuario actualizar(int id, UsuarioSolicitud s, Usuario actual) {
        exigirAdministrador(actual);
        if (s == null) {
            throw new DatoInvalidoException("Faltan los datos del usuario.");
        }
        Usuario u = armar(s);
        u.setIdPersona(id);
        if (id == actual.getIdPersona() && u.getRol() != Rol.ADMINISTRADOR) {
            throw new DatoInvalidoException("No puede quitarse a usted mismo el rol de administrador.");
        }
        if (!dao.actualizar(u)) {
            throw new NoEncontradoException("No existe el usuario " + id + ".");
        }
        return buscar(id);
    }

    /** CU-02: el administrador pone una clave nueva. De paso se quita el bloqueo (RN-11). */
    public void restablecerClave(int id, String clave, Usuario actual) {
        exigirAdministrador(actual);
        String hash = hashDe(clave);
        if (!dao.cambiarClave(id, hash)) {
            throw new NoEncontradoException("No existe el usuario " + id + ".");
        }
    }

    /** RN-05: desactiva en lugar de borrar. Desde ese momento no puede ingresar (AuthService). */
    public void desactivar(int id, Usuario actual) {
        exigirAdministrador(actual);
        if (id == actual.getIdPersona()) {
            throw new DatoInvalidoException("No puede desactivar su propio usuario.");
        }
        if (!dao.desactivar(id)) {
            throw new NoEncontradoException("No existe el usuario " + id + ".");
        }
    }

    /** Vuelve a activar un usuario desactivado por error. */
    public void activar(int id, Usuario actual) {
        exigirAdministrador(actual);
        if (!dao.activar(id)) {
            throw new NoEncontradoException("No existe el usuario " + id + ".");
        }
    }

    // ------------------------------------------------------------------
    // Validacion del servidor
    // ------------------------------------------------------------------

    private static void exigirAdministrador(Usuario actual) {
        if (actual == null || !actual.esAdministrador()) {
            throw new AccesoDenegadoException("No tiene permiso para esta acción.");
        }
    }

    private Usuario buscar(int id) {
        return dao.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id + "."));
    }

    /**
     * Limpia espacios, convierte los opcionales vacios en null y valida.
     * Responde 400 con un mensaje claro antes de que la base rechace el dato.
     */
    private Usuario armar(UsuarioSolicitud s) {
        Usuario u = new Usuario();
        u.setNombres(limpiar(s.getNombres()));
        u.setApellidos(limpiar(s.getApellidos()));
        u.setDni(limpiar(s.getDni()));
        u.setTelefono(opcional(s.getTelefono()));
        u.setCorreo(opcional(s.getCorreo()));
        u.setDireccion(opcional(s.getDireccion()));
        u.setNombreUsuario(limpiar(s.getNombreUsuario()));
        u.setRol(rolDe(s.getRol()));

        u.validar();   // RN-04: nombres, DNI y telefono

        maximo(u.getNombres(), LARGO_NOMBRES, "Nombres");
        maximo(u.getApellidos(), LARGO_NOMBRES, "Apellidos");
        maximo(u.getCorreo(), LARGO_CORREO, "Correo");
        maximo(u.getDireccion(), LARGO_DIRECCION, "Dirección");
        if (u.getCorreo() != null && !Validador.esCorreo(u.getCorreo())) {
            throw new DatoInvalidoException("El correo no es válido. Ejemplo: nombre@correo.com");
        }
        if (u.getNombreUsuario() == null || !u.getNombreUsuario().matches(PATRON_USUARIO)) {
            throw new DatoInvalidoException(
                    "El usuario debe tener de 4 a 30 caracteres: letras sin tilde, números, punto o guion bajo.");
        }
        return u;
    }

    /** "VENDEDOR" o "ADMINISTRADOR" (sin importar mayusculas). Otro valor: 400. */
    private static Rol rolDe(String texto) {
        if (texto != null) {
            for (Rol r : Rol.values()) {
                if (r.name().equalsIgnoreCase(texto.trim())) {
                    return r;
                }
            }
        }
        throw new DatoInvalidoException("Elija un rol válido: Administrador o Vendedor.");
    }

    /** RN-11 y hash PBKDF2. El arreglo con la clave se borra al terminar. */
    private static String hashDe(String clave) {
        if (clave == null) {
            throw new DatoInvalidoException("Escriba la clave.");
        }
        char[] letras = clave.toCharArray();
        try {
            if (!Clave.cumplePolitica(letras)) {
                throw new DatoInvalidoException("La clave debe tener al menos 8 caracteres, con letras y números.");
            }
            return Clave.generar(letras);
        } finally {
            Arrays.fill(letras, '\0');
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String opcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static void maximo(String valor, int largo, String campo) {
        if (valor != null && !Validador.cumpleLongitudMaxima(valor, largo)) {
            throw new DatoInvalidoException("El campo " + campo + " admite como máximo " + largo + " caracteres.");
        }
    }
}
