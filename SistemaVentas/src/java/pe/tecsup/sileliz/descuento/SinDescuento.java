/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.tecsup.sileliz.descuento;

import java.math.BigDecimal;
import java.util.List;
import pe.tecsup.sileliz.modelo.DetalleVenta;

/**
 * Venta sin descuento: siempre descuenta 0.
 */
public class SinDescuento implements Descuento {

    @Override
    public BigDecimal calcular(List<DetalleVenta> lineas) {
        return BigDecimal.ZERO;
    }

    @Override
    public String descripcion() {
        return "Sin descuento";
    }
}
