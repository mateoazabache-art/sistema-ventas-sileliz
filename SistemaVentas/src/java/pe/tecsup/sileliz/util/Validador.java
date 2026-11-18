package pe.tecsup.sileliz.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class Validador {

    /** Tallas que vende la tienda (entrevista con el dueño, 14 nov), en el orden de la lista. */
    private static final List<String> TALLAS_PERMITIDAS = List.of(
            "L", "XL", "2XL", "3XL", "4XL"
    );

    /** Las mismas tallas, como conjunto, para buscar rápido. */
    private static final Set<String> CONJUNTO_TALLAS = Set.copyOf(TALLAS_PERMITIDAS);

    public static boolean esObligatorio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    public static boolean cumpleLongitudMaxima(String valor, int longitud) {
        return valor != null && valor.length() <= longitud;
    }

    public static boolean esNumeroPositivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean esNumeroNoNegativo(int valor) {
        return valor >= 0;
    }

    public static boolean soloDigitos(String valor) {
        return valor != null && valor.matches("\\d+");
    }

    public static boolean tieneLongitudExacta(String valor, int longitud) {
        return valor != null && valor.length() == longitud;
    }

    public static boolean esCorreo(String correo) {
        return correo != null
                && correo.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static boolean esDni(String dni) {
        return soloDigitos(dni) && tieneLongitudExacta(dni, 8);
    }

    public static boolean esCelular(String celular) {
        return soloDigitos(celular)
                && tieneLongitudExacta(celular, 9)
                && celular.startsWith("9");
    }

    /**
     * true si la talla (sin espacios y en mayúsculas) está en el conjunto dado.
     * Es la regla base: la versión de un parámetro la usa con las tallas oficiales.
     */
    public static boolean esTallaPermitida(String talla, Set<String> tallasPermitidas) {
        return talla != null
                && tallasPermitidas != null
                && tallasPermitidas.contains(talla.trim().toUpperCase());
    }

    /** true si es una talla oficial de la tienda: L, XL, 2XL, 3XL o 4XL ("2xl" también vale). */
    public static boolean esTallaPermitida(String talla) {
        return esTallaPermitida(talla, CONJUNTO_TALLAS);
    }

    /** Tallas oficiales en orden, para la lista de productos.html (GET /api/catalogos/tallas). */
    public static List<String> obtenerTallasPermitidas() {
        return TALLAS_PERMITIDAS;
    }
}
