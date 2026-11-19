package pe.tecsup.sileliz.excepcion;

/** El registro pedido no existe (por ejemplo /api/clientes/999) -> HTTP 404. */
public class NoEncontradoException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public NoEncontradoException() {
        super("No se encontró el registro.", 404);
    }

    public NoEncontradoException(String mensaje) {
        super(mensaje, 404);
    }
}
