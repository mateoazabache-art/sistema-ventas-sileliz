package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del reporte de ventas (vista v_ventas_resumen).
 * Es un record (Java 17): solo datos, sin setters. Gson lo convierte en JSON
 * directamente: {"idVenta":15,"comprobante":"B001-000015","cliente":"Varios",...}
 *
 * cliente = "Varios" cuando la venta fue a consumidor final (RN-06).
 */
public record VentaResumen(
        int idVenta,
        String comprobante,
        LocalDateTime fechaHora,
        String cliente,
        String vendedor,
        String metodoPago,
        BigDecimal descuento,
        BigDecimal total,
        String estado) {
}
