package pe.tecsup.sileliz.excepcion;

/**
 * Padre de todas las excepciones del sistema (seccion 5.5 del plan).
 * Cada hija lleva su codigo HTTP; Json.error(resp, e) responde
 * {"error": getMessage()} con ese codigo.
 * El mensaje es para el usuario: claro y sin detalles tecnicos.
 */
public abstract class SilelizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int codigoHttp;

    protected SilelizException(String mensaje, int codigoHttp) {
        super(mensaje);
        this.codigoHttp = codigoHttp;
    }

    protected SilelizException(String mensaje, int codigoHttp, Throwable causa) {
        super(mensaje, causa);
        this.codigoHttp = codigoHttp;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }
}
