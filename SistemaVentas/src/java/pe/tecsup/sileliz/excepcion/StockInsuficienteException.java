package pe.tecsup.sileliz.excepcion;

/**
 * El UPDATE condicional de VarianteDao.descontarStock afecto 0 filas
 * (seccion 4.4): no hay stock suficiente -> HTTP 409.
 */
public class StockInsuficienteException extends SilelizException {

    private static final long serialVersionUID = 1L;

    public StockInsuficienteException(String descripcion) {
        super("No hay stock suficiente de " + descripcion + ".", 409);
    }
}
