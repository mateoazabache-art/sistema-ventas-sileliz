package pe.tecsup.sileliz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.tecsup.sileliz.modelo.Rol;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Acceso a datos de Usuario (tablas persona + usuario).
 *
 * Sirve de MOLDE para los demas DAO (ver docs/guia-dao.md):
 *   1. Toda conexion sale de Conexion.getConnection(), dentro de try-with-resources.
 *   2. Todo SQL usa PreparedStatement con "?" (nunca se concatena texto del usuario).
 *   3. Se listan las columnas; nunca SELECT *.
 *   4. Toda SQLException se convierte con ErrorSql.traducir(e) (409, 400, 500...).
 *   5. Si se escribe en dos tablas, va en una transaccion: commit o rollback.
 *
 * El DAO solo guarda y lee. No valida reglas de negocio ni genera hashes:
 * eso lo hacen AuthService y UsuarioService.
 */
public class UsuarioDao {

    /** Columnas comunes a todas las consultas de usuario. */
    private static final String SELECT_USUARIO =
            "SELECT p.id_persona, p.nombres, p.apellidos, p.dni, p.telefono, p.correo, "
            + "       p.direccion, p.activo, p.fecha_registro, "
            + "       u.nombre_usuario, u.clave_hash, u.rol, u.intentos_fallidos, u.bloqueado_hasta "
            + "FROM usuario u "
            + "JOIN persona p ON p.id_persona = u.id_persona ";

    /** Filtro del buscador: nombres, apellidos, usuario o DNI. */
    private static final String FILTRO_TEXTO =
            "WHERE (p.nombres ILIKE ? OR p.apellidos ILIKE ? OR u.nombre_usuario ILIKE ? OR p.dni LIKE ?) ";

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Busca por nombre de usuario (para el login). Devuelve el usuario aunque
     * este inactivo o bloqueado: AuthService decide que hacer.
     */
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        String sql = SELECT_USUARIO + "WHERE u.nombre_usuario = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    public Optional<Usuario> buscarPorId(int idPersona) {
        String sql = SELECT_USUARIO + "WHERE u.id_persona = ?";
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
     * Lista una pagina de usuarios, ordenados por apellidos.
     * texto vacio o null = todos.
     */
    public Paginado<Usuario> listarPaginado(int pagina, int tam, String texto) {
        int t = Paginado.normalizarTam(tam);
        String patron = patron(texto);
        String sql = SELECT_USUARIO + FILTRO_TEXTO
                + "ORDER BY p.apellidos, p.nombres "
                + "LIMIT ? OFFSET ?";
        List<Usuario> lista = new ArrayList<>();
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
        String sql = "SELECT count(*) FROM usuario u JOIN persona p ON p.id_persona = u.id_persona "
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
     * Guarda un usuario nuevo: INSERT en persona y luego en usuario, en una
     * sola transaccion (plan 5.1). Si el segundo falla (por ejemplo, nombre de
     * usuario repetido), el primero se deshace: no queda una persona suelta.
     *
     * u.getClaveHash() debe venir ya generado con Clave.generar (UsuarioService).
     * Al terminar, u tiene su idPersona y fechaRegistro.
     */
    public Usuario insertar(Usuario u) {
        String sqlPersona =
                "INSERT INTO persona (nombres, apellidos, dni, telefono, correo, direccion, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "RETURNING id_persona, fecha_registro";
        String sqlUsuario =
                "INSERT INTO usuario (id_persona, nombre_usuario, clave_hash, rol) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(sqlPersona)) {
                    ps.setString(1, u.getNombres());
                    ps.setString(2, u.getApellidos());
                    ps.setString(3, u.getDni());
                    ps.setString(4, vacioANull(u.getTelefono()));
                    ps.setString(5, vacioANull(u.getCorreo()));
                    ps.setString(6, vacioANull(u.getDireccion()));
                    ps.setBoolean(7, u.isActivo());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        u.setIdPersona(rs.getInt("id_persona"));
                        u.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement(sqlUsuario)) {
                    ps.setInt(1, u.getIdPersona());
                    ps.setString(2, u.getNombreUsuario());
                    ps.setString(3, u.getClaveHash());
                    ps.setString(4, u.getRol() == null ? null : u.getRol().name());
                    ps.executeUpdate();
                }
                cn.commit();
                return u;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                u.setIdPersona(0);
                throw e;
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * Actualiza datos personales, nombre de usuario y rol (no la clave).
     * Devuelve false si el usuario no existe.
     */
    public boolean actualizar(Usuario u) {
        String sqlPersona =
                "UPDATE persona SET nombres = ?, apellidos = ?, dni = ?, telefono = ?, correo = ?, direccion = ? "
                + "WHERE id_persona = ?";
        String sqlUsuario =
                "UPDATE usuario SET nombre_usuario = ?, rol = ? WHERE id_persona = ?";

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int filas;
                try (PreparedStatement ps = cn.prepareStatement(sqlUsuario)) {
                    ps.setString(1, u.getNombreUsuario());
                    ps.setString(2, u.getRol() == null ? null : u.getRol().name());
                    ps.setInt(3, u.getIdPersona());
                    filas = ps.executeUpdate();
                }
                if (filas == 0) {
                    cn.rollback();
                    return false;
                }
                try (PreparedStatement ps = cn.prepareStatement(sqlPersona)) {
                    ps.setString(1, u.getNombres());
                    ps.setString(2, u.getApellidos());
                    ps.setString(3, u.getDni());
                    ps.setString(4, vacioANull(u.getTelefono()));
                    ps.setString(5, vacioANull(u.getCorreo()));
                    ps.setString(6, vacioANull(u.getDireccion()));
                    ps.setInt(7, u.getIdPersona());
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
     * Guarda el contador de intentos fallidos y el bloqueo (RN-11).
     * AuthService lo llama despues de Usuario.registrarIntentoFallido()
     * o reiniciarIntentos().
     */
    public void guardarIntentos(Usuario u) {
        String sql = "UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id_persona = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, u.getIntentosFallidos());
            ps.setTimestamp(2, aTimestamp(u.getBloqueadoHasta()));
            ps.setInt(3, u.getIdPersona());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * Cambia la clave (restablecer clave, CU-02) y de paso quita el bloqueo.
     * claveHash debe venir de Clave.generar.
     */
    public boolean cambiarClave(int idPersona, String claveHash) {
        String sql = "UPDATE usuario SET clave_hash = ?, intentos_fallidos = 0, bloqueado_hasta = NULL "
                + "WHERE id_persona = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, claveHash);
            ps.setInt(2, idPersona);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    /**
     * RN-05: un usuario no se borra, se desactiva (persona.activo = false).
     * Devuelve false si el usuario no existe.
     */
    public boolean desactivar(int idPersona) {
        String sql = "UPDATE persona SET activo = false "
                + "WHERE id_persona = ? AND EXISTS (SELECT 1 FROM usuario WHERE id_persona = ?)";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            ps.setInt(2, idPersona);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    /** Convierte la fila actual del ResultSet en un objeto Usuario. */
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdPersona(rs.getInt("id_persona"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setDni(rs.getString("dni"));
        u.setTelefono(rs.getString("telefono"));
        u.setCorreo(rs.getString("correo"));
        u.setDireccion(rs.getString("direccion"));
        u.setActivo(rs.getBoolean("activo"));
        u.setFechaRegistro(aFecha(rs.getTimestamp("fecha_registro")));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setClaveHash(rs.getString("clave_hash"));
        u.setRol(Rol.valueOf(rs.getString("rol")));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        u.setBloqueadoHasta(aFecha(rs.getTimestamp("bloqueado_hasta")));
        return u;
    }

    /** Pone el mismo patron en los 4 "?" del filtro. Devuelve el siguiente indice libre. */
    private int ponerFiltro(PreparedStatement ps, String patron) throws SQLException {
        for (int i = 1; i <= 4; i++) {
            ps.setString(i, patron);
        }
        return 5;
    }

    /** "ana" -> "%ana%"; vacio o null -> "%" (todos). */
    private String patron(String texto) {
        return (texto == null || texto.isBlank()) ? "%" : "%" + texto.trim() + "%";
    }

    /** Los campos opcionales vacios se guardan como NULL (si no, fallaria el CHECK del telefono). */
    private String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private LocalDateTime aFecha(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private Timestamp aTimestamp(LocalDateTime fecha) {
        return fecha == null ? null : Timestamp.valueOf(fecha);
    }
}
