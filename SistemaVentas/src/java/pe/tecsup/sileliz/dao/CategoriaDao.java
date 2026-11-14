package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.tecsup.sileliz.modelo.Categoria;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Acceso a datos de la tabla categoria.
 *
 * Las consultas publicas abren su propia conexion.
 * Las sobrecargas que reciben Connection permiten reutilizar
 * una conexion y trabajar dentro de una transaccion.
 */
public class CategoriaDao {

    private static final String FILTRO =
            "WHERE nombre ILIKE ? ESCAPE '\\' ";

    private static final String SQL_CONTAR =
            "SELECT COUNT(*) FROM categoria " + FILTRO;

    private static final String SQL_LISTAR =
            "SELECT id_categoria, nombre, activo FROM categoria "
            + FILTRO
            + "ORDER BY nombre, id_categoria LIMIT ? OFFSET ?";

    private static final String SQL_POR_ID =
            "SELECT id_categoria, nombre, activo FROM categoria "
            + "WHERE id_categoria = ?";

    private static final String SQL_INSERTAR =
            "INSERT INTO categoria (nombre, activo) VALUES (?, ?) "
            + "RETURNING id_categoria";

    private static final String SQL_ACTUALIZAR =
            "UPDATE categoria SET nombre = ? WHERE id_categoria = ?";

    private static final String SQL_DESACTIVAR =
            "UPDATE categoria SET activo = FALSE WHERE id_categoria = ?";

    /**
     * Lista categorias activas e inactivas, filtradas por nombre.
     * Una pagina vacia o null en texto devuelve todas.
     */
    public Paginado<Categoria> listarPaginado(int pagina, int tam, String texto) {
        try (Connection cn = Conexion.getConnection()) {
            return listarPaginado(cn, pagina, tam, texto);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    Paginado<Categoria> listarPaginado(
            Connection cn, int pagina, int tam, String texto) throws SQLException {

        int paginaValida = Math.max(pagina, 1);
        int tamValido = Paginado.normalizarTam(tam);
        String patron = crearPatron(texto);

        long total = contar(cn, patron);
        List<Categoria> items = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(SQL_LISTAR)) {
            ps.setString(1, patron);
            ps.setInt(2, tamValido);
            ps.setInt(3, Paginado.offset(paginaValida, tamValido));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapear(rs));
                }
            }
        }

        return Paginado.de(items, paginaValida, tamValido, total);
    }

    /**
     * Cuenta las categorias que coinciden con el texto.
     */
    public long contar(String texto) {
        try (Connection cn = Conexion.getConnection()) {
            return contar(cn, crearPatron(texto));
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    long contar(Connection cn, String patron) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_CONTAR)) {
            ps.setString(1, patron);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Busca una categoria por su identificador.
     */
    public Optional<Categoria> buscarPorId(int idCategoria) {
        try (Connection cn = Conexion.getConnection()) {
            return buscarPorId(cn, idCategoria);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    Optional<Categoria> buscarPorId(Connection cn, int idCategoria)
            throws SQLException {

        try (PreparedStatement ps = cn.prepareStatement(SQL_POR_ID)) {
            ps.setInt(1, idCategoria);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapear(rs))
                        : Optional.empty();
            }
        }
    }

    /**
     * Inserta una categoria y asigna el identificador generado.
     */
    public Categoria insertar(Categoria categoria) {
        try (Connection cn = Conexion.getConnection()) {
            return insertar(cn, categoria);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    Categoria insertar(Connection cn, Categoria categoria)
            throws SQLException {

        try (PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, categoria.getNombre());
            ps.setBoolean(2, categoria.isActivo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    categoria.setIdCategoria(rs.getInt("id_categoria"));
                    return categoria;
                }
            }
        }

        throw new SQLException("No se obtuvo el id generado de la categoria.");
    }

    /**
     * Actualiza el nombre de una categoria.
     * Devuelve false si no existe.
     */
    public boolean actualizar(Categoria categoria) {
        try (Connection cn = Conexion.getConnection()) {
            return actualizar(cn, categoria);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    boolean actualizar(Connection cn, Categoria categoria)
            throws SQLException {

        try (PreparedStatement ps = cn.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, categoria.getNombre());
            ps.setInt(2, categoria.getIdCategoria());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Desactiva una categoria sin eliminarla de la base de datos.
     */
    public boolean desactivar(int idCategoria) {
        try (Connection cn = Conexion.getConnection()) {
            return desactivar(cn, idCategoria);
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    boolean desactivar(Connection cn, int idCategoria)
            throws SQLException {

        try (PreparedStatement ps = cn.prepareStatement(SQL_DESACTIVAR)) {
            ps.setInt(1, idCategoria);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Prepara el patron de busqueda y escapa los comodines.
     */
    private static String crearPatron(String texto) {
        String limpio = texto == null ? "" : texto.trim();

        String escapado = limpio.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escapado + "%";
    }

    /**
     * Convierte una fila de categoria en un objeto Categoria.
     */
    private static Categoria mapear(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getInt("id_categoria"));
        categoria.setNombre(rs.getString("nombre"));
        categoria.setActivo(rs.getBoolean("activo"));
        return categoria;
    }
}
