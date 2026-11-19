package pe.tecsup.sileliz.excepcion;

/** Quinto intento fallido seguido (RN-11) -> HTTP 423. */
public class CuentaBloqueadaException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public CuentaBloqueadaException() {
        super("Cuenta bloqueada por 15 minutos.", 423);
    }

    public CuentaBloqueadaException(String mensaje) {
        super(mensaje, 423);
    }

    public CuentaBloqueadaException(String mensaje, Throwable causa) {
        super(mensaje, 423, causa);
    }
}
