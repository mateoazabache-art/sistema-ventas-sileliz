package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.tecsup.sileliz.modelo.Cliente;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Acceso a datos de Cliente (tablas persona + cliente). Plan 5.1 y tarea 9 de Mateo.
 *
 * Sigue las 5 reglas de docs/guia-dao.md:
 *   1. Conexion.getConnection() dentro de try-with-resources.
 *   2. PreparedStatement con "?" (nunca concatenar texto del usuario).
 *   3. Columnas listadas, nunca SELECT *.
 *   4. SQLException -> ErrorSql.traducir(e) (DNI repetido = 409, dato invalido = 400...).
 *   5. Persona + cliente se guardan en UNA transaccion.
 *
 * El DAO solo guarda y lee. Las validaciones (RN-04) las hace ClienteService
 * con cliente.validar() antes de llamar al DAO.
 */
public class ClienteDao {

    private static final String SELECT_CLIENTE =
            "SELECT p.id_persona, p.nombres, p.apellidos, p.dni, p.telefono, p.correo, "
            + "       p.direccion, p.activo, p.fecha_registro, c.observaciones "
            + "FROM cliente c "
            + "JOIN persona p ON p.id_persona = c.id_persona ";

    /** Buscador: nombres, apellidos o DNI. */
    private static final String FILTRO_TEXTO =
            "WHERE (p.nombres ILIKE ? OR p.apellidos ILIKE ? OR p.dni LIKE ?) ";

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Busca por DNI exacto. Lo usa la venta (Luz) para encontrar al cliente.
     * Devuelve el cliente aunque este inactivo: el servicio decide si se le puede vender.
     */
    public Optional<Cliente> buscarPorDni(String dni) {
        String sql = SELECT_CLIENTE + "WHERE p.dni = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, dni == null ? null : dni.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    public Optional<Cliente> buscarPorId(int idPersona) {
        String sql = SELECT_CLIENTE + "WHERE c.id_persona = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * Una pagina de clientes ordenados por apellidos.
     * texto vacio o null = todos. Incluye inactivos (la pagina los marca).
     */
    public Paginado<Cliente> listarPaginado(int pagina, int tam, String texto) {
        int t = Paginado.normalizarTam(tam);
        String patron = patron(texto);
        String sql = SELECT_CLIENTE + FILTRO_TEXTO
                + "ORDER BY p.apellidos, p.nombres "
                + "LIMIT ? OFFSET ?";
        List<Cliente> lista = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = ponerFiltro(ps, patron);
            ps.setInt(i++, t);
            ps.setInt(i, Paginado.offset(pagina, t));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return Paginado.de(lista, Math.max(pagina, 1), t, contar(cn, patron));
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    private long contar(Connection cn, String patron) throws SQLException {
        String sql = "SELECT count(*) FROM cliente c JOIN persona p ON p.id_persona = c.id_persona "
                + FILTRO_TEXTO;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ponerFiltro(ps, patron);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    /**
     * Guarda un cliente nuevo: INSERT en persona y luego en cliente, en una
     * sola transaccion (plan 5.1). Si el segundo falla, el primero se deshace.
     * Al terminar, c tiene su idPersona y fechaRegistro.
     */
    public Cliente insertar(Cliente c) {
        String sqlPersona =
                "INSERT INTO persona (nombres, apellidos, dni, telefono, correo, direccion, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "RETURNING id_persona, fecha_registro";
        String sqlCliente =
                "INSERT INTO cliente (id_persona, observaciones) VALUES (?, ?)";

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(sqlPersona)) {
                    ps.setString(1, c.getNombres());
                    ps.setString(2, c.getApellidos());
                    ps.setString(3, c.getDni());
                    ps.setString(4, vacioANull(c.getTelefono()));
                    ps.setString(5, vacioANull(c.getCorreo()));
                    ps.setString(6, vacioANull(c.getDireccion()));
                    ps.setBoolean(7, c.isActivo());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        c.setIdPersona(rs.getInt("id_persona"));
                        c.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement(sqlCliente)) {
                    ps.setInt(1, c.getIdPersona());
                    ps.setString(2, vacioANull(c.getObservaciones()));
                    ps.executeUpdate();
                }
                cn.commit();
                return c;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                c.setIdPersona(0);
                throw e;
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * Actualiza datos personales y observaciones.
     * Devuelve false si el cliente no existe.
     */
    public boolean actualizar(Cliente c) {
        String sqlPersona =
                "UPDATE persona SET nombres = ?, apellidos = ?, dni = ?, telefono = ?, correo = ?, direccion = ? "
                + "WHERE id_persona = ?";
        String sqlCliente =
                "UPDATE cliente SET observaciones = ? WHERE id_persona = ?";

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int filas;
                try (PreparedStatement ps = cn.prepareStatement(sqlCliente)) {
                    ps.setString(1, vacioANull(c.getObservaciones()));
                    ps.setInt(2, c.getIdPersona());
                    filas = ps.executeUpdate();
                }
                if (filas == 0) {
                    cn.rollback();
                    return false;
                }
                try (PreparedStatement ps = cn.prepareStatement(sqlPersona)) {
                    ps.setString(1, c.getNombres());
                    ps.setString(2, c.getApellidos());
                    ps.setString(3, c.getDni());
                    ps.setString(4, vacioANull(c.getTelefono()));
                    ps.setString(5, vacioANull(c.getCorreo()));
                    ps.setString(6, vacioANull(c.getDireccion()));
                    ps.setInt(7, c.getIdPersona());
                    ps.executeUpdate();
                }
                cn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * RN-05: un cliente no se borra, se desactiva (persona.activo = false).
     * Devuelve false si el cliente no existe.
     */
    public boolean desactivar(int idPersona) {
        return cambiarActivo(idPersona, false);
    }

    /** Vuelve a activar un cliente desactivado. */
    public boolean activar(int idPersona) {
        return cambiarActivo(idPersona, true);
    }

    private boolean cambiarActivo(int idPersona, boolean activo) {
        String sql = "UPDATE persona SET activo = ? "
                + "WHERE id_persona = ? AND EXISTS (SELECT 1 FROM cliente WHERE id_persona = ?)";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idPersona);
            ps.setInt(3, idPersona);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    /** Convierte la fila actual del ResultSet en un Cliente. */
    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setIdPersona(rs.getInt("id_persona"));
        c.setNombres(rs.getString("nombres"));
        c.setApellidos(rs.getString("apellidos"));
        c.setDni(rs.getString("dni"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        c.setDireccion(rs.getString("direccion"));
        c.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        c.setFechaRegistro(ts == null ? null : ts.toLocalDateTime());
        c.setObservaciones(rs.getString("observaciones"));
        return c;
    }

    /** Pone el mismo patron en los 3 "?" del filtro. Devuelve el siguiente indice libre. */
    private int ponerFiltro(PreparedStatement ps, String patron) throws SQLException {
        for (int i = 1; i <= 3; i++) {
            ps.setString(i, patron);
        }
        return 4;
    }

    /** "ana" -> "%ana%"; vacio o null -> "%" (todos). */
    private String patron(String texto) {
        return (texto == null || texto.isBlank()) ? "%" : "%" + texto.trim() + "%";
    }

    /** Campos opcionales vacios se guardan como NULL (si no, fallaria el CHECK del telefono). */
    private String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
