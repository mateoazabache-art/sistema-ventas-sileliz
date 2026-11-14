package pe.tecsup.sileliz.descuento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import pe.tecsup.sileliz.modelo.DetalleVenta;

/**
 * Pruebas de las implementaciones de Descuento y de DescuentoFactory.
 */
public class DescuentoTest {

    /** Una sola linea: cantidad x precio. */
    private List<DetalleVenta> lineas(int cantidad, String precio) {
        List<DetalleVenta> l = new ArrayList<>();
        l.add(new DetalleVenta(1, "Prenda de prueba", cantidad, new BigDecimal(precio)));
        return l;
    }

    @Test
    public void sinDescuento_devuelveCero() {
        assertEquals(BigDecimal.ZERO, new SinDescuento().calcular(lineas(2, "59.00")));
    }

    @Test
    public void porcentaje_del10SobreBruto118_da11con80() {
        Descuento d = new DescuentoPorcentaje(new BigDecimal("10"));
        assertEquals(new BigDecimal("11.80"), d.calcular(lineas(2, "59.00")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void porcentaje_mayorQue100_seRechaza() {
        new DescuentoPorcentaje(new BigDecimal("101"));
    }

    @Test
    public void porCantidad_conMenosDelMinimo_noDescuenta() {
        Descuento d = new DescuentoPorCantidad(3, new BigDecimal("10"));
        assertEquals(BigDecimal.ZERO, d.calcular(lineas(2, "50.00")));
    }

    @Test
    public void porCantidad_conTresPrendas_descuenta10Porciento() {
        Descuento d = new DescuentoPorCantidad(3, new BigDecimal("10"));
        // bruto = 3 x 50.00 = 150.00 -> 10 % = 15.00
        assertEquals(new BigDecimal("15.00"), d.calcular(lineas(3, "50.00")));
    }

    @Test
    public void factory_devuelveLaImplementacionCorrecta() {
        assertTrue(DescuentoFactory.crear(null, null) instanceof SinDescuento);
        assertTrue(DescuentoFactory.crear("SIN", null) instanceof SinDescuento);
        assertTrue(DescuentoFactory.crear("PORCENTAJE", new BigDecimal("10")) instanceof DescuentoPorcentaje);
        assertTrue(DescuentoFactory.crear("POR_CANTIDAD", null) instanceof DescuentoPorCantidad);
    }

    @Test(expected = IllegalArgumentException.class)
    public void factory_tipoDesconocido_seRechaza() {
        DescuentoFactory.crear("BITCOIN", null);
    }
   
    @Test
    public void porCantidad_sumaVariasLineas() {
        List<DetalleVenta> l = new ArrayList<>();
        l.add(new DetalleVenta(1, "Prenda A", 1, new BigDecimal("50.00")));
        l.add(new DetalleVenta(2, "Prenda B", 2, new BigDecimal("30.00")));
        Descuento d = new DescuentoPorCantidad(3, new BigDecimal("10"));
        // 3 prendas en total, bruto = 50 + 60 = 110.00 -> 10 % = 11.00
        assertEquals(new BigDecimal("11.00"), d.calcular(l));
    }

    @Test
    public void porCantidad_conCantidadesEnormes_noDesborda() {
        List<DetalleVenta> l = new ArrayList<>();
        l.add(new DetalleVenta(1, "Prenda A", Integer.MAX_VALUE, new BigDecimal("1.00")));
        l.add(new DetalleVenta(2, "Prenda B", Integer.MAX_VALUE, new BigDecimal("1.00")));
        Descuento d = new DescuentoPorCantidad(3, new BigDecimal("10"));
        // Con un int la suma daria negativo y no descontaria; con long si descuenta
        assertEquals(new BigDecimal("429496729.40"), d.calcular(l));
    }

    @Test
    public void porCantidad_conExactamenteElMinimo_descuenta() {
        Descuento d = new DescuentoPorCantidad(3, new BigDecimal("10"));
        assertEquals(new BigDecimal("15.00"), d.calcular(lineas(3, "50.00")));
    } 
}
