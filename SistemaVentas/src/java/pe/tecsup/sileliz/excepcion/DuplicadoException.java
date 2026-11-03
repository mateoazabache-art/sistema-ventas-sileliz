package pe.tecsup.sileliz.excepcion;

/** UNIQUE violado (SQLState 23505) -> HTTP 409. */
public class DuplicadoException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public DuplicadoException() {
        super("Ya existe un registro con ese valor.", 409);
    }

    public DuplicadoException(String mensaje) {
        super(mensaje, 409);
    }

    public DuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, 409, causa);
    }
}
