package pe.tecsup.sileliz.servicio;

import java.util.List;
import java.util.Objects;

import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.dao.ProductoDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.modelo.Producto;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Paginado;
import pe.tecsup.sileliz.util.Validador;

public class ProductoService {

    private static final int LONGITUD_MAXIMA_CODIGO = 20;
    private static final int LONGITUD_MAXIMA_NOMBRE = 100;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 250;

    private final ProductoDao productoDao;
    private final CategoriaDao categoriaDao;

    public ProductoService(ProductoDao productoDao, CategoriaDao categoriaDao) {
        this.productoDao = Objects.requireNonNull(productoDao, "productoDao");
        this.categoriaDao = Objects.requireNonNull(categoriaDao, "categoriaDao");
    }

    // ---------- Escritura ----------

    public Producto registrar(Producto producto) {
        validarDatos(producto);

        if (!producto.isActivo()) {
            throw new DatoInvalidoException("Un producto nuevo no puede registrarse inactivo.");
        }

        validarCategoria(producto.getCategoria().getIdCategoria());

        return productoDao.insertar(producto);
    }

    public void actualizar(Producto producto) {
        validarDatos(producto);
        validarId(producto.getIdProducto());
        exigirProductoExistente(producto.getIdProducto());
        validarCategoria(producto.getCategoria().getIdCategoria());

        if (!productoDao.actualizar(producto)) {
            throw new NoEncontradoException(
                    "No se encontró el producto con ID " + producto.getIdProducto() + ".");
        }
    }

    public void desactivar(int idProducto) {
        validarId(idProducto);
        exigirProductoExistente(idProducto);

        if (!productoDao.desactivar(idProducto)) {
            throw new NoEncontradoException(
                    "No se encontró el producto con ID " + idProducto + ".");
        }
    }

    // ---------- Consultas ----------

    public Producto obtenerPorId(int idProducto) {
        validarId(idProducto);
        return exigirProductoExistente(idProducto);
    }

    public Paginado<Producto> listarPaginado(
            int pagina, int tam, String texto, int idCategoria) {

        if (!Validador.esNumeroNoNegativo(idCategoria)) {
            throw new DatoInvalidoException(
                    "El ID de categoría del filtro no puede ser negativo.");
        }

        return productoDao.listarPaginado(pagina, tam, texto, idCategoria);
    }

    public List<VarianteProducto> listarStockBajo() {
        return productoDao.listarStockBajo();
    }

    // ---------- Validaciones privadas ----------

    private void validarId(int idProducto) {
        if (idProducto <= 0) {
            throw new DatoInvalidoException(
                    "El ID del producto debe ser un número positivo.");
        }
    }

    private Producto exigirProductoExistente(int idProducto) {
        return productoDao.buscarPorId(idProducto)
                .orElseThrow(() -> new NoEncontradoException(
                        "No se encontró el producto con ID " + idProducto + "."));
    }

    private void validarDatos(Producto producto) {
        if (producto == null) {
            throw new DatoInvalidoException("El producto es obligatorio.");
        }

        if (!Validador.esObligatorio(producto.getCodigo())) {
            throw new DatoInvalidoException("El código es obligatorio.");
        }

        if (!Validador.cumpleLongitudMaxima(
                producto.getCodigo(), LONGITUD_MAXIMA_CODIGO)) {
            throw new DatoInvalidoException(
                    "El código no puede superar "
                    + LONGITUD_MAXIMA_CODIGO + " caracteres.");
        }

        if (!Validador.esObligatorio(producto.getNombre())) {
            throw new DatoInvalidoException("El nombre es obligatorio.");
        }

        if (!Validador.cumpleLongitudMaxima(
                producto.getNombre(), LONGITUD_MAXIMA_NOMBRE)) {
            throw new DatoInvalidoException(
                    "El nombre no puede superar "
                    + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }

        if (producto.getDescripcion() != null
                && !Validador.cumpleLongitudMaxima(
                        producto.getDescripcion(), LONGITUD_MAXIMA_DESCRIPCION)) {
            throw new DatoInvalidoException(
                    "La descripción no puede superar "
                    + LONGITUD_MAXIMA_DESCRIPCION + " caracteres.");
        }

        if (producto.getCategoria() == null
                || producto.getCategoria().getIdCategoria() <= 0) {
            throw new DatoInvalidoException(
                    "La categoría es obligatoria y su ID debe ser positivo.");
        }

        if (producto.getPrecioVenta() == null
                || !Validador.esNumeroPositivo(producto.getPrecioVenta())) {
            throw new DatoInvalidoException(
                    "El precio de venta es obligatorio y debe ser mayor que cero.");
        }
    }

    private void validarCategoria(int idCategoria) {
        Categoria categoria = categoriaDao.buscarPorId(idCategoria)
                .orElseThrow(() -> new NoEncontradoException(
                        "No se encontró la categoría con ID " + idCategoria + "."));

        if (!categoria.isActivo()) {
            throw new DatoInvalidoException(
                    "La categoría seleccionada está inactiva.");
        }
    }
}
