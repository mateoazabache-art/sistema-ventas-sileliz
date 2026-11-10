package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tarjetas de la pagina de Inicio (GET /api/estadisticas/hoy, tarea 10 de Cesia).
 *
 *   ventas       ventas EMITIDAS del dia
 *   total        suma de esas ventas (S/)
 *   anuladas     ventas del dia que se anularon
 *   clientes     clientes activos registrados
 *   stockBajo    variantes activas con stock en el minimo o menos (RN-10)
 */
public record ResumenDia(
        LocalDate fecha,
        long ventas,
        BigDecimal total,
        long anuladas,
        long clientes,
        long stockBajo) {
}
