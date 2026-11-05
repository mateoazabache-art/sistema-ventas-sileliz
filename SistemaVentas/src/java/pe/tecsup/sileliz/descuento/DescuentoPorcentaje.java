/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.tecsup.sileliz.descuento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import pe.tecsup.sileliz.modelo.DetalleVenta;

/**
 * Descuento de un porcentaje sobre el total bruto (por ejemplo 10 %).
 */
public class DescuentoPorcentaje implements Descuento {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final BigDecimal porcentaje;

    public DescuentoPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null
                || porcentaje.signum() < 0
                || porcentaje.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100.");
        }
        this.porcentaje = porcentaje;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    @Override
    public BigDecimal calcular(List<DetalleVenta> lineas) {
        BigDecimal bruto = BigDecimal.ZERO;
        for (DetalleVenta d : lineas) {
            bruto = bruto.add(d.subtotal());
        }
        return bruto.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String descripcion() {
        return porcentaje.stripTrailingZeros().toPlainString() + " % de descuento";
    }
}