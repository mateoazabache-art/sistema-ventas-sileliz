/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import pe.tecsup.sileliz.descuento.Descuento;
import pe.tecsup.sileliz.descuento.DescuentoPorcentaje;
import pe.tecsup.sileliz.descuento.SinDescuento;

/**
 * Pruebas de Venta.calcularTotales (precios con IGV incluido, RN-01 y RN-12).
 */
public class VentaTest {

    /** Crea una venta con una sola linea: cantidad x precio. */
    private Venta ventaConUnaLinea(int cantidad, String precio) {
        Venta v = new Venta(null, 1, MetodoPago.EFECTIVO);
        v.agregarDetalle(new DetalleVenta(1, "Prenda de prueba", cantidad, new BigDecimal(precio)));
        return v;
    }

    @Test
    public void sinDescuento_calculaBaseIgvYTotal() {
        Venta v = ventaConUnaLinea(2, "59.00");
        v.calcularTotales(new SinDescuento());

        assertEquals(new BigDecimal("0.00"), v.getDescuento());
        assertEquals(new BigDecimal("118.00"), v.getTotal());
        assertEquals(new BigDecimal("100.00"), v.getBaseImponible());
        assertEquals(new BigDecimal("18.00"), v.getIgv());
    }

    @Test
    public void conDescuentoDel10Porciento_restaAntesDelIgv() {
        Venta v = ventaConUnaLinea(2, "59.00");
        v.calcularTotales(new DescuentoPorcentaje(new BigDecimal("10")));

        assertEquals(new BigDecimal("11.80"), v.getDescuento());
        assertEquals(new BigDecimal("106.20"), v.getTotal());
        assertEquals(new BigDecimal("90.00"), v.getBaseImponible());
        assertEquals(new BigDecimal("16.20"), v.getIgv());
    }

    @Test
    public void descuentoNuncaSuperaElBruto() {
        Venta v = ventaConUnaLinea(2, "59.00");
        // Descuento falso que intenta restar 500 sobre un bruto de 118.00
        v.calcularTotales(new Descuento() {
            @Override
            public BigDecimal calcular(List<DetalleVenta> lineas) {
                return new BigDecimal("500.00");
            }

            @Override
            public String descripcion() {
                return "Descuento de prueba";
            }
        });

        assertEquals(new BigDecimal("118.00"), v.getDescuento());
        assertEquals(new BigDecimal("0.00"), v.getTotal());
        assertEquals(new BigDecimal("0.00"), v.getIgv());
    }

    @Test
    public void baseMasIgvSiempreSumaElTotal() {
        Venta v = ventaConUnaLinea(1, "50.00");
        v.calcularTotales(new SinDescuento());

        // 50.00 / 1.18 = 42.3728... -> 42.37 con HALF_UP; IGV = 50.00 - 42.37 = 7.63
        assertEquals(new BigDecimal("42.37"), v.getBaseImponible());
        assertEquals(new BigDecimal("7.63"), v.getIgv());
        assertEquals(v.getTotal(), v.getBaseImponible().add(v.getIgv()));
    }
}