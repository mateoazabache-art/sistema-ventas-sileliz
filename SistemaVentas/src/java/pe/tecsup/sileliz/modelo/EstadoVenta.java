package pe.tecsup.sileliz.modelo;

/**
 * Estado de una venta. Los valores coinciden con el CHECK de la columna
 * venta.estado en la base de datos.
 */
public enum EstadoVenta {
    EMITIDA,
    ANULADA
}