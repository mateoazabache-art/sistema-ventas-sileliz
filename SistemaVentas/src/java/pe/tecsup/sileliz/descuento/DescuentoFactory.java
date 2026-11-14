package pe.tecsup.sileliz.descuento;

import java.math.BigDecimal;

/**
 * Elige la implementacion de Descuento segun el tipo permitido.
 * El navegador solo envia el tipo y un valor; el servidor arma el objeto.
 */
public final class DescuentoFactory {

    /** Minimo de prendas para el descuento por cantidad (propuesta del Plan). */
    public static final int MINIMO_POR_CANTIDAD = 3;

    /** Porcentaje del descuento por cantidad (propuesta del Plan). */
    public static final BigDecimal PORCENTAJE_POR_CANTIDAD = new BigDecimal("10");

    private DescuentoFactory() {
    }

    /**
     * @param tipo  SIN, PORCENTAJE o POR_CANTIDAD (nulo o vacio = SIN)
     * @param valor porcentaje para el tipo PORCENTAJE (ej. 10 para 10 %)
     * @throws IllegalArgumentException si el tipo no es permitido o el valor es invalido
     */
    public static Descuento crear(String tipo, BigDecimal valor) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return new SinDescuento();
        }
        switch (tipo.trim().toUpperCase()) {
            case "SIN":
                return new SinDescuento();
            case "PORCENTAJE":
                return new DescuentoPorcentaje(valor);
            case "POR_CANTIDAD":
                return new DescuentoPorCantidad(MINIMO_POR_CANTIDAD, PORCENTAJE_POR_CANTIDAD);
            default:
                throw new IllegalArgumentException("Tipo de descuento no permitido: " + tipo);
        }
    }
}