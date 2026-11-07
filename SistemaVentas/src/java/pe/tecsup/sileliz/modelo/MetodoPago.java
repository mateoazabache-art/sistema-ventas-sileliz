package pe.tecsup.sileliz.modelo;

/**
 * Metodo de pago de una venta. Los valores coinciden con el CHECK de la
 * columna venta.metodo_pago en la base de datos (seccion 4.1 del Plan).
 */
public enum MetodoPago {
    EFECTIVO,
    YAPE,
    PLIN,
    TARJETA,
    TRANSFERENCIA
}
