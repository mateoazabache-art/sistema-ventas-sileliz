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
 * Descuento por cantidad: si se llevan al menos "minimo" prendas en total,
 * se descuenta un porcentaje sobre el total bruto.
 * Propuesta del Plan: 10 % desde la tercera prenda (se confirma con la tienda).
 */
public class DescuentoPorCantidad implements Descuento {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final int minimo;
    private final BigDecimal porcentaje;

    public DescuentoPorCantidad(int minimo, BigDecimal porcentaje) {
        if (minimo < 1) {
            throw new IllegalArgumentException("El minimo de prendas debe ser al menos 1.");
        }
        if (porcentaje == null
                || porcentaje.signum() < 0
                || porcentaje.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100.");
        }
        this.minimo = minimo;
        this.porcentaje = porcentaje;
    }

    public int getMinimo() {
        return minimo;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    @Override
    public BigDecimal calcular(List<DetalleVenta> lineas) {
        int prendas = 0;
        BigDecimal bruto = BigDecimal.ZERO;
        for (DetalleVenta d : lineas) {
            prendas += d.getCantidad();
            bruto = bruto.add(d.subtotal());
        }
        if (prendas < minimo) {
            return BigDecimal.ZERO;
        }
        return bruto.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String descripcion() {
        return porcentaje.stripTrailingZeros().toPlainString()
                + " % de descuento desde " + minimo + " prendas";
    }
}