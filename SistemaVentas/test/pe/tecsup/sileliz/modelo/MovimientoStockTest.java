package pe.tecsup.sileliz.modelo;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Pruebas de las fabricas de MovimientoStock (correccion pedida en la revision del PR #21).
 */
public class MovimientoStockTest {

    @Test
    public void deVenta_guardaLaCantidadEnNegativo() {
        MovimientoStock m = MovimientoStock.deVenta(1, 5, 3, 2);
        assertEquals(-3, m.getCantidad());
        assertEquals(MovimientoStock.Tipo.VENTA, m.getTipo());
    }

    @Test
    public void deAnulacion_guardaLaCantidadEnPositivo() {
        MovimientoStock m = MovimientoStock.deAnulacion(1, 5, 3, 2, "Error de cobro");
        assertEquals(3, m.getCantidad());
        assertEquals(MovimientoStock.Tipo.ANULACION, m.getTipo());
    }

    @Test
    public void deVenta_aceptaElMaximoEntero() {
        MovimientoStock m = MovimientoStock.deVenta(1, 5, Integer.MAX_VALUE, 2);
        assertEquals(-Integer.MAX_VALUE, m.getCantidad());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deVenta_conCero_seRechaza() {
        MovimientoStock.deVenta(1, 5, 0, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deVenta_conNegativo_seRechaza() {
        MovimientoStock.deVenta(1, 5, -4, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deVenta_conMinValue_seRechaza() {
        MovimientoStock.deVenta(1, 5, Integer.MIN_VALUE, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deAnulacion_conCero_seRechaza() {
        MovimientoStock.deAnulacion(1, 5, 0, 2, "motivo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deAnulacion_conNegativo_seRechaza() {
        MovimientoStock.deAnulacion(1, 5, -1, 2, "motivo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deAnulacion_conMinValue_seRechaza() {
        MovimientoStock.deAnulacion(1, 5, Integer.MIN_VALUE, 2, "motivo");
    }
}
