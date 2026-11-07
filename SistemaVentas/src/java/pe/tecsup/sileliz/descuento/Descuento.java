package pe.tecsup.sileliz.descuento;

import java.math.BigDecimal;
import java.util.List;
import pe.tecsup.sileliz.modelo.DetalleVenta;

/**
 * Contrato de cualquier descuento. Venta llama a calcular() sin saber
 * si es SinDescuento, DescuentoPorcentaje o DescuentoPorCantidad (polimorfismo).
 */
public interface Descuento {

    /** Monto a descontar sobre las lineas. Nunca mayor al total bruto (RN-12). */
    BigDecimal calcular(List<DetalleVenta> lineas);

    /** Texto corto para mostrar, por ejemplo "10 % de descuento". */
    String descripcion();
}