
package pe.tecsup.sileliz.servicio;

import java.util.Objects;
import pe.tecsup.sileliz.dao.CategoriaDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.util.Paginado;
import pe.tecsup.sileliz.util.Validador;

public class CategoriaService {

    private static final int LONGITUD_MAXIMA_NOMBRE = 60;

    private final CategoriaDao categoriaDao;

    public CategoriaService(CategoriaDao categoriaDao) {
        this.categoriaDao = Objects.requireNonNull(
                categoriaDao, "categoriaDao");
    }

    // ---------- Escritura ----------

    public Categoria registrar(Categoria categoria) {
        validarDatos(categoria);
        categoria.setNombre(categoria.getNombre().trim());
        categoria.setActivo(true);
        return categoriaDao.insertar(categoria);
    }

    public void actualizar(Categoria categoria) {
        validarDatos(categoria);
        validarId(categoria.getIdCategoria());
        categoria.setNombre(categoria.getNombre().trim());
        exigirCategoriaExistente(categoria.getIdCategoria());

        if (!categoriaDao.actualizar(categoria)) {
            throw new NoEncontradoException(
                    "No se encontró la categoría con ID "
                    + categoria.getIdCategoria() + ".");
        }
    }

    public void desactivar(int idCategoria) {
        validarId(idCategoria);
        exigirCategoriaExistente(idCategoria);

        if (!categoriaDao.desactivar(idCategoria)) {
            throw new NoEncontradoException(
                    "No se encontró la categoría con ID "
                    + idCategoria + ".");
        }
    }

    // ---------- Consultas ----------

    public Categoria obtenerPorId(int idCategoria) {
        validarId(idCategoria);
        return exigirCategoriaExistente(idCategoria);
    }

    public Paginado<Categoria> listarPaginado(
            int pagina, int tam, String texto, Boolean activo) {
        return categoriaDao.listarPaginado(
                pagina, tam, texto, activo);
    }

    // ---------- Validaciones privadas ----------

    private void validarId(int idCategoria) {
        if (idCategoria <= 0) {
            throw new DatoInvalidoException(
                    "El ID de la categoría debe ser un número positivo.");
        }
    }

    private Categoria exigirCategoriaExistente(int idCategoria) {
        return categoriaDao.buscarPorId(idCategoria)
                .orElseThrow(() -> new NoEncontradoException(
                        "No se encontró la categoría con ID "
                        + idCategoria + "."));
    }

    private void validarDatos(Categoria categoria) {
        if (categoria == null) {
            throw new DatoInvalidoException(
                    "La categoría es obligatoria.");
        }

        if (!Validador.esObligatorio(categoria.getNombre())) {
            throw new DatoInvalidoException(
                    "El nombre de la categoría es obligatorio.");
        }

        if (!Validador.cumpleLongitudMaxima(
                categoria.getNombre().trim(), LONGITUD_MAXIMA_NOMBRE)) {
            throw new DatoInvalidoException(
                    "El nombre de la categoría no puede superar "
                    + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
    }
}
