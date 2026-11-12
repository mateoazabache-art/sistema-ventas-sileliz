package pe.tecsup.sileliz.modelo;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class VarianteProductoTest {

    private VarianteProducto crearVariante(int idVariante, int stock, int stockMinimo) {
        VarianteProducto variante = new VarianteProducto();
        variante.setIdVariante(idVariante);
        variante.setStock(stock);
        variante.setStockMinimo(stockMinimo);
        return variante;
    }

    @Test
    public void hayStock_conStockMayorQueLaCantidad_devuelveTrue() {
        VarianteProducto variante = crearVariante(1, 10, 2);
        assertTrue(variante.hayStock(5));
    }

    @Test
    public void hayStock_conStockIgualALaCantidad_devuelveTrue() {
        VarianteProducto variante = crearVariante(1, 5, 2);
        assertTrue(variante.hayStock(5));
    }

    @Test
    public void hayStock_conStockMenorQueLaCantidad_devuelveFalse() {
        VarianteProducto variante = crearVariante(1, 4, 2);
        assertFalse(variante.hayStock(5));
    }

    @Test
    public void estaBajoMinimo_conStockMenorQueElMinimo_devuelveTrue() {
        VarianteProducto variante = crearVariante(1, 1, 3);
        assertTrue(variante.estaBajoMinimo());
    }

    @Test
    public void estaBajoMinimo_conStockIgualAlMinimo_devuelveTrue() {
        VarianteProducto variante = crearVariante(1, 3, 3);
        assertTrue(variante.estaBajoMinimo());
    }

    @Test
    public void estaBajoMinimo_conStockMayorQueElMinimo_devuelveFalse() {
        VarianteProducto variante = crearVariante(1, 10, 3);
        assertFalse(variante.estaBajoMinimo());
    }

    @Test
    public void equals_conElMismoIdVariante_sonIguales() {
        VarianteProducto a = crearVariante(7, 10, 2);
        VarianteProducto b = crearVariante(7, 99, 5);
        assertEquals(a, b);
    }

    @Test
    public void equals_conDistintoIdVariante_sonDiferentes() {
        VarianteProducto a = crearVariante(7, 10, 2);
        VarianteProducto b = crearVariante(8, 10, 2);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_conElMismoObjeto_devuelveTrue() {
        VarianteProducto a = crearVariante(7, 10, 2);
        assertTrue(a.equals(a));
    }

    @Test
    public void equals_conNull_devuelveFalse() {
        VarianteProducto a = crearVariante(7, 10, 2);
        assertFalse(a.equals(null));
    }

    @Test
    public void equals_conObjetoDeOtroTipo_devuelveFalse() {
        VarianteProducto a = crearVariante(7, 10, 2);
        assertFalse(a.equals("7"));
    }

    @Test
    public void hashCode_conVariantesIguales_esElMismo() {
        VarianteProducto a = crearVariante(7, 10, 2);
        VarianteProducto b = crearVariante(7, 99, 5);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hashCode_siSeLlamaDosVeces_esConsistente() {
        VarianteProducto a = crearVariante(7, 10, 2);
        assertEquals(a.hashCode(), a.hashCode());
    }
}