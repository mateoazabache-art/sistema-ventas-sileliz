package pe.tecsup.sileliz.servicio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pe.tecsup.sileliz.dao.ProductoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Validador;

public class VarianteService {

    private static final int LONGITUD_MAXIMA_TALLA = 10;
    private static final int LONGITUD_MAXIMA_COLOR = 30;
    private static final int LONGITUD_MAXIMA_SKU = 30;

    private final VarianteDao varianteDao;
    private final ProductoDao productoDao;

    public VarianteService(VarianteDao varianteDao, ProductoDao productoDao) {
        this.varianteDao = Objects.requireNonNull(varianteDao, "varianteDao");
        this.productoDao = Objects.requireNonNull(productoDao, "productoDao");
    }

    // ---------- Escritura ----------

    public VarianteProducto registrar(VarianteProducto variante) {
        validarDatos(variante);

        if (!variante.isActivo()) {
            throw new DatoInvalidoException(
                    "Una variante nueva no puede registrarse inactiva.");
        }

        validarProducto(variante.getProducto().getIdProducto());

        return varianteDao.insertar(variante);
    }

    
    public void actualizar(VarianteProducto variante) {
        if (variante == null) {
            throw new DatoInvalidoException("La variante es obligatoria.");
        }

        validarId(variante.getIdVariante());

        VarianteProducto existente = exigirVarianteExistente(
                variante.getIdVariante());

        if (variante.getProducto() == null
                || variante.getProducto().getIdProducto() <= 0) {
            variante.setProducto(existente.getProducto());
        } else if (variante.getProducto().getIdProducto()
                != existente.getProducto().getIdProducto()) {
            throw new DatoInvalidoException(
                    "No se puede cambiar el producto de una variante.");
        }

        validarDatos(variante);

        if (!varianteDao.actualizar(variante)) {
            throw new NoEncontradoException(
                    "No se encontró la variante con ID "
                    + variante.getIdVariante() + ".");
        }
    }


    public void desactivar(int idVariante) {
        validarId(idVariante);
        exigirVarianteExistente(idVariante);

        if (!varianteDao.desactivar(idVariante)) {
            throw new NoEncontradoException(
                    "No se encontró la variante con ID " + idVariante + ".");
        }
    }

    // ---------- Consultas ----------

    public VarianteProducto obtenerPorId(int idVariante) {
        validarId(idVariante);
        return exigirVarianteExistente(idVariante);
    }

    public List<VarianteProducto> buscarPorTexto(String texto) {
        return varianteDao.buscarPorTexto(texto);
    }

    // ---------- Validaciones privadas ----------

    private void validarId(int idVariante) {
        if (idVariante <= 0) {
            throw new DatoInvalidoException(
                    "El ID de la variante debe ser un número positivo.");
        }
    }

    private VarianteProducto exigirVarianteExistente(int idVariante) {
        Optional<VarianteProducto> resultado = varianteDao.buscarPorId(idVariante);

        return resultado.orElseThrow(() -> new NoEncontradoException(
                "No se encontró la variante con ID " + idVariante + "."));
    }

    private void validarProducto(int idProducto) {
        Producto producto = productoDao.buscarPorId(idProducto)
                .orElseThrow(() -> new NoEncontradoException(
                        "No se encontró el producto con ID " + idProducto + "."));

        if (!producto.isActivo()) {
            throw new DatoInvalidoException(
                    "No se pueden registrar variantes en un producto inactivo.");
        }
    }

    private void validarDatos(VarianteProducto variante) {
        if (variante == null) {
            throw new DatoInvalidoException("La variante es obligatoria.");
        }

        if (variante.getProducto() == null
                || variante.getProducto().getIdProducto() <= 0) {
            throw new DatoInvalidoException(
                    "El producto es obligatorio y su ID debe ser positivo.");
        }

        if (!Validador.esObligatorio(variante.getTalla())) {
            throw new DatoInvalidoException("La talla es obligatoria.");
        }

        if (!Validador.cumpleLongitudMaxima(
                variante.getTalla(), LONGITUD_MAXIMA_TALLA)) {
            throw new DatoInvalidoException(
                    "La talla no puede superar "
                    + LONGITUD_MAXIMA_TALLA + " caracteres.");
        }

        if (!Validador.esObligatorio(variante.getColor())) {
            throw new DatoInvalidoException("El color es obligatorio.");
        }

        if (!Validador.cumpleLongitudMaxima(
                variante.getColor(), LONGITUD_MAXIMA_COLOR)) {
            throw new DatoInvalidoException(
                    "El color no puede superar "
                    + LONGITUD_MAXIMA_COLOR + " caracteres.");
        }

        if (!Validador.esObligatorio(variante.getSku())) {
            throw new DatoInvalidoException("El SKU es obligatorio.");
        }

        if (!Validador.cumpleLongitudMaxima(
                variante.getSku(), LONGITUD_MAXIMA_SKU)) {
            throw new DatoInvalidoException(
                    "El SKU no puede superar "
                    + LONGITUD_MAXIMA_SKU + " caracteres.");
        }

        if (!Validador.esNumeroNoNegativo(variante.getStockMinimo())) {
            throw new DatoInvalidoException(
                    "El stock mínimo no puede ser negativo.");
        }
    }
}
