package pe.tecsup.sileliz.util;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.DuplicadoException;
import pe.tecsup.sileliz.excepcion.PersistenciaException;
import pe.tecsup.sileliz.excepcion.ReferenciaException;
import pe.tecsup.sileliz.excepcion.SilelizException;

/**
 * Traduce los errores de PostgreSQL (SQLState) a las excepciones del sistema
 * (tabla 5.5 del plan). Lo tecnico va al log; al usuario solo le llega un
 * mensaje claro.
 *
 *   23505 UNIQUE      -> DuplicadoException      409
 *   23514 CHECK       -> DatoInvalidoException   400
 *   23502 NOT NULL    -> DatoInvalidoException   400
 *   23503 FOREIGN KEY -> ReferenciaException     409
 *   08xxx conexion    -> PersistenciaException   500
 *   otro              -> PersistenciaException   500
 */
public final class ErrorSql {

    private static final Logger LOG = Logger.getLogger(ErrorSql.class.getName());

    /**
     * Mensajes especificos segun el nombre de la restriccion que crea
     * 01_crear_tablas.sql. Si una restriccion no esta aqui, se usa el
     * mensaje general de la excepcion.
     */
    private static final Map<String, String> MENSAJES = Map.ofEntries(
            Map.entry("persona_dni_key", "Ya existe una persona con ese DNI."),
            Map.entry("persona_dni_check", "El DNI debe tener exactamente 8 dígitos."),
            Map.entry("persona_telefono_check", "El teléfono debe tener 9 dígitos y empezar con 9."),
            Map.entry("usuario_nombre_usuario_key", "Ese nombre de usuario ya está en uso."),
            Map.entry("categoria_nombre_key", "Ya existe una categoría con ese nombre."),
            Map.entry("producto_codigo_key", "Ya existe un producto con ese código."),
            Map.entry("producto_precio_venta_check", "El precio debe ser mayor que 0."),
            Map.entry("variante_producto_sku_key", "Ya existe una variante con ese SKU."),
            Map.entry("uq_variante_producto_talla_color", "Ese producto ya tiene esa talla y color."),
            Map.entry("variante_producto_stock_check", "El stock no puede quedar negativo."),
            Map.entry("ck_venta_motivo_anulacion", "Para anular una venta indique el motivo.")
    );

    private ErrorSql() {
    }

    /** Convierte una SQLException en la excepcion del sistema que corresponde. */
    public static SilelizException traducir(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        LOG.log(Level.WARNING, "Error SQL " + estado + ": " + e.getMessage(), e);

        String especifico = buscarMensaje(e.getMessage());
        switch (estado) {
            case "23505":
                return especifico == null ? new DuplicadoException() : new DuplicadoException(especifico, e);
            case "23514":
            case "23502":
            case "22001": // texto mas largo que el VARCHAR
            case "22P02": // numero o fecha con formato invalido
                return especifico == null ? new DatoInvalidoException() : new DatoInvalidoException(especifico, e);
            case "23503":
                return new ReferenciaException("Tiene ventas: se desactivará en lugar de eliminarse.", e);
            default:
                // 08xxx = no hay conexion; cualquier otro error tambien es 500
                return new PersistenciaException("No se pudo conectar. Intente de nuevo.", e);
        }
    }

    /**
     * Version para el catch (RuntimeException | SQLException e) de los
     * servicios (seccion 5.6): si ya es nuestra la deja pasar, si es SQL la
     * traduce y si es otra cosa la trata como error interno.
     */
    public static SilelizException traducir(Exception e) {
        if (e instanceof SilelizException) {
            return (SilelizException) e;
        }
        if (e instanceof SQLException) {
            return traducir((SQLException) e);
        }
        LOG.log(Level.SEVERE, "Error inesperado", e);
        return new PersistenciaException("Ocurrió un error inesperado. Intente de nuevo.", e);
    }

    private static String buscarMensaje(String textoError) {
        if (textoError == null) {
            return null;
        }
        for (Map.Entry<String, String> m : MENSAJES.entrySet()) {
            if (textoError.contains("\"" + m.getKey() + "\"") || textoError.contains("«" + m.getKey() + "»")) {
                return m.getValue();
            }
        }
        return null;
    }
}
