package pe.tecsup.sileliz.servicio;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import pe.tecsup.sileliz.dao.ProductoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regla de tallas de VarianteService (PR #61): solo L, XL, 2XL, 3XL y 4XL.
 * Usa DAO falsos: no necesita la base de datos.
 */
public class VarianteServiceTest {

    /** DAO falso: guarda lo que le llega en vez de ir a la base. */
    private static class VarianteDaoFalso extends VarianteDao {
        VarianteProducto insertada;

        @Override
        public VarianteProducto insertar(VarianteProducto variante) {
            insertada = variante;
            variante.setIdVariante(99);
            return variante;
        }
    }

    /** DAO falso: el producto 1 existe y está activo. */
    private static class ProductoDaoFalso extends ProductoDao {
        @Override
        public Optional<Producto> buscarPorId(int idProducto) {
            Producto p = new Producto();
            p.setIdProducto(idProducto);
            p.setActivo(true);
            return Optional.of(p);
        }
    }

    private VarianteDaoFalso varianteDao;
    private VarianteService servicio;

    @Before
    public void preparar() {
        varianteDao = new VarianteDaoFalso();
        servicio = new VarianteService(varianteDao, new ProductoDaoFalso());
    }

    private VarianteProducto variante(String talla) {
        Producto producto = new Producto();
        producto.setIdProducto(1);

        VarianteProducto v = new VarianteProducto();
        v.setProducto(producto);
        v.setTalla(talla);
        v.setColor("Negro");
        v.setSku("P-001-" + talla.trim().toUpperCase() + "-NEG");
        v.setStockMinimo(2);
        return v;
    }

    @Test
    public void registraUnaTallaOficial() {
        servicio.registrar(variante("2XL"));

        assertEquals("2XL", varianteDao.insertada.getTalla());
    }

    @Test
    public void aceptaLaTallaEnMinusculas() {
        servicio.registrar(variante("4xl"));

        assertTrue(varianteDao.insertada != null);
    }

    @Test
    public void rechazaUnaTallaQueLaTiendaNoVende() {
        try {
            servicio.registrar(variante("5XL"));
            fail("Debió rechazar la talla 5XL");
        } catch (DatoInvalidoException e) {
            assertEquals("La talla debe ser L, XL, 2XL, 3XL o 4XL.", e.getMessage());
        }
        assertFalse("No debe llegar a la base", varianteDao.insertada != null);
    }

    @Test
    public void rechazaLaTallaM() {
        try {
            servicio.registrar(variante("M"));
            fail("Debió rechazar la talla M");
        } catch (DatoInvalidoException e) {
            assertTrue(e.getMessage().contains("L, XL, 2XL, 3XL o 4XL"));
        }
    }
}
