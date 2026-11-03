package pe.tecsup.sileliz.excepcion;

/** El rol no puede usar ese recurso (SesionFilter) -> HTTP 403. */
public class AccesoDenegadoException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public AccesoDenegadoException() {
        super("No tiene permiso para esta acción.", 403);
    }

    public AccesoDenegadoException(String mensaje) {
        super(mensaje, 403);
    }

    public AccesoDenegadoException(String mensaje, Throwable causa) {
        super(mensaje, 403, causa);
    }
}
