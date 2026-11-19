package pe.tecsup.sileliz.excepcion;

/** Login fallido. Mensaje generico a proposito: no revela si existe el usuario -> HTTP 401. */
public class AutenticacionException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public AutenticacionException() {
        super("Usuario o clave incorrectos.", 401);
    }

    public AutenticacionException(String mensaje) {
        super(mensaje, 401);
    }

    public AutenticacionException(String mensaje, Throwable causa) {
        super(mensaje, 401, causa);
    }
}
