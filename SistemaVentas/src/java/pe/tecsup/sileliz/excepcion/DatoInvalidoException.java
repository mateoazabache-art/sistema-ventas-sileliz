package pe.tecsup.sileliz.excepcion;

/** CHECK o NOT NULL violado (23514, 23502) o dato mal formado -> HTTP 400. */
public class DatoInvalidoException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public DatoInvalidoException() {
        super("Hay datos que no cumplen las reglas. Revise el formulario.", 400);
    }

    public DatoInvalidoException(String mensaje) {
        super(mensaje, 400);
    }

    public DatoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, 400, causa);
    }
}
