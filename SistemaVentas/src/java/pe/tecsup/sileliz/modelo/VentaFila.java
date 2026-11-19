package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del historial de ventas (para la tabla de ventas.html).
 * Incluye el nombre del cliente ("Varios" si no hay) y del vendedor.
 */
public record VentaFila(
        int idVenta,
        String comprobante,
        LocalDateTime fechaHora,
        String cliente,
        String vendedor,
        BigDecimal total,
        MetodoPago metodoPago,
        EstadoVenta estado) {
}
