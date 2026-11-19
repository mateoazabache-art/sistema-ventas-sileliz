package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;

/**
 * Fila generica de los reportes agrupados (plan 5.3: record para reportes).
 *
 *   Mas vendidos:        etiqueta = "Blusa manga corta XL Negro (P-001-XL-NEG)",
 *                        cantidad = unidades vendidas, monto = importe.
 *   Ventas por vendedor: etiqueta = nombre del vendedor,
 *                        cantidad = numero de ventas, monto = total vendido.
 *   Total del rango:     etiqueta = "Total", cantidad = ventas, monto = suma.
 *
 * Solo cuentan las ventas EMITIDAS (las anuladas no suman).
 */
public record FilaReporte(String etiqueta, long cantidad, BigDecimal monto) {
}
