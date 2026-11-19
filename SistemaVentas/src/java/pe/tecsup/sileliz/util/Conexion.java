package pe.tecsup.sileliz.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.tecsup.sileliz.excepcion.PersistenciaException;

/**
 * Unico punto de conexion a la base de datos (seccion 5.6 del plan).
 * Es la UNICA clase que conoce la URL, el usuario y la clave.
 *
 * Lee config.properties (src/java/config.properties, que NO se sube a Git).
 * Cada integrante lo crea copiando config.example.properties.
 *
 * Uso en un DAO, siempre con try-with-resources para que la conexion se cierre:
 *
 *     try (Connection cn = Conexion.getConnection();
 *          PreparedStatement ps = cn.prepareStatement(SQL)) {
 *         ...
 *     } catch (SQLException e) {
 *         throw ErrorSql.traducir(e);
 *     }
 */
public final class Conexion {

    private static final Logger LOG = Logger.getLogger(Conexion.class.getName());
    private static final String ARCHIVO = "config.properties";

    private static volatile Properties config;

    private Conexion() {
        // Clase de utilidad: no se instancia
    }

    /** Abre una conexion nueva. Quien la pide la cierra (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        Properties p = config();
        return DriverManager.getConnection(
                p.getProperty("db.url"),
                p.getProperty("db.usuario"),
                p.getProperty("db.clave"));
    }

    /** true si la base responde. Lo usa /api/salud. Nunca lanza excepcion. */
    public static boolean probar() {
        try (Connection cn = getConnection()) {
            return cn.isValid(2);
        } catch (SQLException | RuntimeException e) {
            LOG.log(Level.WARNING, "La base de datos no responde: {0}", e.getMessage());
            return false;
        }
    }

    /** Carga config.properties una sola vez (la primera vez que se pide). */
    private static Properties config() {
        Properties p = config;
        if (p == null) {
            synchronized (Conexion.class) {
                p = config;
                if (p == null) {
                    p = cargar();
                    config = p;
                }
            }
        }
        return p;
    }

    private static Properties cargar() {
        try (InputStream in = Conexion.class.getClassLoader().getResourceAsStream(ARCHIVO)) {
            if (in == null) {
                LOG.severe("No se encontro " + ARCHIVO + ". Copie src/java/config.example.properties "
                        + "como src/java/config.properties y ponga su clave local.");
                throw new PersistenciaException();
            }
            Properties p = new Properties();
            p.load(in);
            for (String clave : new String[]{"db.url", "db.usuario", "db.clave"}) {
                if (p.getProperty(clave) == null || p.getProperty(clave).isBlank()) {
                    LOG.severe("Falta la propiedad " + clave + " en " + ARCHIVO);
                    throw new PersistenciaException();
                }
            }
            // En Tomcat el driver de WEB-INF/lib no siempre se registra solo
            Class.forName("org.postgresql.Driver");
            return p;
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "No se pudo preparar la conexion", e);
            throw new PersistenciaException("No se pudo conectar. Intente de nuevo.", e);
        }
    }
}
