package pe.tecsup.sileliz.excepcion;

/** FOREIGN KEY violada (23503) -> HTTP 409. */
public class ReferenciaException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public ReferenciaException() {
        super("Tiene ventas: se desactivará en lugar de eliminarse.", 409);
    }

    public ReferenciaException(String mensaje) {
        super(mensaje, 409);
    }

    public ReferenciaException(String mensaje, Throwable causa) {
        super(mensaje, 409, causa);
    }
}
