package pe.tecsup.sileliz.validacion;

import java.math.BigDecimal;
import java.util.Set;

public class Validador {

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

    public static boolean esTallaPermitida(String talla, Set<String> tallasPermitidas) {
        return talla != null
                && tallasPermitidas != null
                && tallasPermitidas.contains(talla.trim().toUpperCase());
    }
}
