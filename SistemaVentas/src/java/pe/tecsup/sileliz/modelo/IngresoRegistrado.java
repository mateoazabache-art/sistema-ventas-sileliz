package pe.tecsup.sileliz.modelo;

/**
 * Respuesta de POST /api/ingresos (201):
 *   {"idVariante": 11, "sku": "P-004-2XL-NEG", "cantidad": 12, "stock": 20}
 * stock es el stock DESPUES del ingreso, leido de la base.
 */
public record IngresoRegistrado(int idVariante, String sku, int cantidad, int stock) {
}
