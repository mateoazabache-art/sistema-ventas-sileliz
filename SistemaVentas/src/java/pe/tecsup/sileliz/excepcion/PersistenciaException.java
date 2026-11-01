package pe.tecsup.sileliz.excepcion;

/** Base de datos caida (clase 08) o error inesperado de SQL -> HTTP 500. */
public class PersistenciaException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException() {
        super("No se pudo conectar. Intente de nuevo.", 500);
    }

    public PersistenciaException(String mensaje) {
        super(mensaje, 500);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, 500, causa);
    }
}
