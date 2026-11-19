package pe.tecsup.sileliz.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import pe.tecsup.sileliz.dao.ReporteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.modelo.FilaReporte;
import pe.tecsup.sileliz.modelo.ReporteVentas;
import pe.tecsup.sileliz.modelo.VentaResumen;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Reglas de los reportes (tarea 13 de Rafael). Valida lo que llega del
 * navegador y recien entonces llama a ReporteDao, que solo lee.
 *
 * Recibe los parametros como texto (tal como llegan en la direccion:
 * ?desde=2026-11-01&hasta=2026-11-11&pagina=1) y los convierte aqui,
 * para que el Servlet no tenga ninguna regla.
 *
 * Reglas del rango de fechas:
 *   - Formato AAAA-MM-DD (el que envia <input type="date">).
 *   - Sin "hasta": hoy. Sin "desde": el dia 1 del mes de "hasta".
 *   - "desde" no puede ser posterior a "hasta".
 *   - Como maximo MAX_DIAS dias, para no pedirle a la base anos enteros.
 * Si algo no cumple: DatoInvalidoException (HTTP 400) con mensaje claro.
 */
public class ReporteService {

    /** Rango mas largo que se puede consultar (un ano). */
    public static final int MAX_DIAS = 366;

    /** Cuantas filas trae "mas vendidos" si no se indica. */
    public static final int LIMITE_POR_DEFECTO = 10;

    private final ReporteDao dao;
    private final Clock reloj;

    public ReporteService() {
        this(new ReporteDao(), Clock.systemDefaultZone());
    }

    /** Para las pruebas: DAO falso y un reloj detenido en una fecha conocida. */
    ReporteService(ReporteDao dao, Clock reloj) {
        this.dao = dao;
        this.reloj = reloj;
    }

    /** Rango de fechas ya validado (dias completos, los dos incluidos). */
    public record Rango(LocalDate desde, LocalDate hasta) {
    }

    // ------------------------------------------------------------------
    // Reportes
    // ------------------------------------------------------------------

    /** Lista paginada de ventas del rango con su total acumulado. */
    public ReporteVentas ventas(String desde, String hasta, String pagina, String tam) {
        Rango r = validarRango(desde, hasta);
        int pag = Math.max(entero(pagina, "página", 1), 1);
        int t = entero(tam, "tamaño de página", Paginado.TAM_POR_DEFECTO);

        FilaReporte total = dao.totalPorRango(r.desde(), r.hasta());
        Paginado<VentaResumen> lista = dao.ventasPorRango(r.desde(), r.hasta(), pag, t);
        return new ReporteVentas(r.desde(), r.hasta(), total.cantidad(), total.monto(), lista);
    }

    /** Variantes con mas unidades vendidas en el rango. */
    public List<FilaReporte> masVendidos(String desde, String hasta, String limite) {
        Rango r = validarRango(desde, hasta);
        return dao.masVendidos(r.desde(), r.hasta(), entero(limite, "límite", LIMITE_POR_DEFECTO));
    }

    /** Numero de ventas y monto por vendedor en el rango. */
    public List<FilaReporte> porVendedor(String desde, String hasta) {
        Rango r = validarRango(desde, hasta);
        return dao.ventasPorVendedor(r.desde(), r.hasta());
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    Rango validarRango(String desdeTexto, String hastaTexto) {
        LocalDate hasta = fecha(hastaTexto, "hasta");
        if (hasta == null) {
            hasta = LocalDate.now(reloj);
        }
        LocalDate desde = fecha(desdeTexto, "desde");
        if (desde == null) {
            desde = hasta.withDayOfMonth(1);
        }
        if (desde.isAfter(hasta)) {
            throw new DatoInvalidoException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\".");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS) {
            throw new DatoInvalidoException("El rango no puede pasar de " + MAX_DIAS + " días.");
        }
        return new Rango(desde, hasta);
    }

    /** Texto AAAA-MM-DD -> fecha. Vacio -> null (se usa el valor por defecto). */
    private static LocalDate fecha(String texto, String nombre) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException(
                    "La fecha \"" + nombre + "\" no es válida. Use el formato AAAA-MM-DD.", e);
        }
    }

    /** Texto -> entero. Vacio -> valor por defecto. Letras -> 400. */
    private static int entero(String texto, String nombre, int porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El valor de \"" + nombre + "\" debe ser un número entero.", e);
        }
    }
}
