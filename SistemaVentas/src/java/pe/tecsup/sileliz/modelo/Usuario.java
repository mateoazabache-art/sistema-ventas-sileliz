package pe.tecsup.sileliz.modelo;

import java.time.LocalDateTime;

/**
 * Usuario del sistema (administrador o vendedor). Hereda de Persona y
 * comparte id_persona (tablas persona + usuario).
 *
 * claveHash es transient: Gson lo ignora, asi el hash NUNCA sale en un JSON
 * (por ejemplo en /api/sesion o /api/usuarios).
 */
public class Usuario extends Persona {

    /** RN-11: intentos fallidos seguidos antes de bloquear. */
    public static final int MAX_INTENTOS = 5;
    /** RN-11: minutos que dura el bloqueo. */
    public static final int MINUTOS_BLOQUEO = 15;

    private String nombreUsuario;
    private transient String claveHash;
    private Rol rol;
    private int intentosFallidos;
    private LocalDateTime bloqueadoHasta;

    public Usuario() {
    }

    public Usuario(String nombres, String apellidos, String dni, String nombreUsuario, Rol rol) {
        super(nombres, apellidos, dni);
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
    }

    @Override
    public String getTipo() {
        return "USUARIO";
    }

    /** true si tiene un bloqueo que todavia no vence. */
    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    public boolean esAdministrador() {
        return rol == Rol.ADMINISTRADOR;
    }

    /**
     * Suma un intento fallido. Al llegar a MAX_INTENTOS fija el bloqueo
     * y devuelve true. AuthService guarda el cambio con UsuarioDao.
     */
    public boolean registrarIntentoFallido() {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueadoHasta = LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO);
            intentosFallidos = 0;
            return true;
        }
        return false;
    }

    /** Login correcto: se borran los intentos y el bloqueo. */
    public void reiniciarIntentos() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public void setClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }
}
