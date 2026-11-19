package pe.tecsup.sileliz.modelo;

/**
 * Lo que necesita el comprobante (GET /api/ventas/{id}): la venta con su
 * detalle y los nombres que la Venta no guarda (solo guarda los ids).
 * cliente = "Varios" y dniCliente = null cuando es consumidor final (RN-06).
 *
 * JSON: {"venta":{...,"detalles":[...]},"cliente":"Varios","dniCliente":null,"vendedor":"Valeria Vendedora"}
 */
public record Comprobante(
        Venta venta,
        String cliente,
        String dniCliente,
        String vendedor) {
}
