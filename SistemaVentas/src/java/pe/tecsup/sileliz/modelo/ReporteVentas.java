package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Respuesta de GET /api/reportes/ventas: el rango que se uso, el total
 * acumulado (solo ventas EMITIDAS) y una pagina de la lista de ventas.
 *
 *   {"desde":"2026-11-01","hasta":"2026-11-11","ventas":12,"total":2540.30,
 *    "pagina":{"items":[...],"pagina":1,"tam":10,"total":13,"paginas":2}}
 *
 * "ventas" y "total" no cuentan las anuladas; la lista si las trae
 * (la pagina las pinta en rojo), por eso pagina.total puede ser mayor.
 */
public record ReporteVentas(
        LocalDate desde,
        LocalDate hasta,
        long ventas,
        BigDecimal total,
        Paginado<VentaResumen> pagina) {
}
