package pe.tecsup.sileliz.servicio;

import java.sql.Connection;
import java.sql.SQLException;
import pe.tecsup.sileliz.dao.MovimientoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.dao.VentaDao;
import pe.tecsup.sileliz.descuento.Descuento;
import pe.tecsup.sileliz.descuento.DescuentoFactory;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.excepcion.StockInsuficienteException;
import pe.tecsup.sileliz.modelo.DetalleVenta;
import pe.tecsup.sileliz.modelo.MovimientoStock;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.modelo.Venta;
import pe.tecsup.sileliz.modelo.VentaSolicitud;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Reglas de la venta (RN-07 stock, RN-09 detalle, RN-12 totales).
 * Todo o nada: si algo falla dentro de la transaccion, rollback.
 */
public class VentaService {

    private final VentaDao ventaDao;
    private final VarianteDao varianteDao;
    private final MovimientoDao movimientoDao;

    public VentaService() {
        this(new VentaDao(), new VarianteDao(), new MovimientoDao());
    }

    /** Para pruebas JUnit (DAO falsos), igual que ClienteService. */
    public VentaService(VentaDao ventaDao, VarianteDao varianteDao, MovimientoDao movimientoDao) {
        // PASO 0: guardar los tres DAO
        this.ventaDao = ventaDao;
        this.varianteDao = varianteDao;
        this.movimientoDao = movimientoDao;
    }

    public Venta registrar(VentaSolicitud s, int idUsuario) {
        // ===== A. ARMAR LA BOLETA EN PAPEL (sin conexion) =====

        // PASO 1: validar lo basico. Si algo esta mal, fallamos RAPIDO
        // sin abrir conexion ni bloquear nada en la base de datos.
        if (s == null) {
            throw new DatoInvalidoException("La solicitud de venta es obligatoria.");
        }
        if (s.getLineas() == null || s.getLineas().isEmpty()) {
            throw new DatoInvalidoException("La venta debe tener al menos una prenda.");
        }
        if (s.getMetodoPago() == null) {
            throw new DatoInvalidoException("Debe indicar el metodo de pago.");
        }
        for (VentaSolicitud.Linea l : s.getLineas()) {
            if (l.getCantidad() <= 0) {
                throw new DatoInvalidoException("La cantidad de cada prenda debe ser mayor a 0.");
            }
        }

        // PASO 2: crear el descuento. La fabrica lanza IllegalArgumentException
        // (error del cliente) y la convertimos a DatoInvalidoException (400, no 500).
        Descuento dto;
        try {
            dto = DescuentoFactory.crear(s.getTipoDescuento(), s.getValorDescuento());
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(e.getMessage());
        }

        // PASO 3: la venta se arma solo en memoria (la "boleta en papel").
        Venta venta = new Venta(s.getIdCliente(), idUsuario, s.getMetodoPago());

        // PASO 4: por cada linea, consultar la variante y agregar el detalle.
        for (VentaSolicitud.Linea l : s.getLineas()) {
            // buscarPorId devuelve Optional (nunca null): orElseThrow lanza si no existe.
            VarianteProducto v = varianteDao.buscarPorId(l.getIdVariante())
                    .orElseThrow(() -> new NoEncontradoException("No existe la variante " + l.getIdVariante()));
            if (!v.isActivo() || !v.getProducto().isActivo()) {
                throw new DatoInvalidoException("La prenda " + v.getProducto().getNombre() + " no esta disponible.");
            }
            String descripcion = v.getProducto().getNombre() + " " + v.getTalla() + " " + v.getColor();
            try {
                venta.agregarDetalle(new DetalleVenta(
                        l.getIdVariante(), descripcion, l.getCantidad(), v.getProducto().getPrecioVenta()));
            } catch (IllegalArgumentException e) {
                throw new DatoInvalidoException(e.getMessage());
            }
        }

        // PASO 5: calcular subtotal, descuento, IGV y total (todavia sin tocar la BD).
        venta.calcularTotales(dto);

        // ===== B. LA CAJA FUERTE (transaccion) =====

        // PASO 6: recien aqui abrimos conexion. La transaccion es lo mas corta posible.
        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false); // nada se guarda de verdad hasta el commit
            try {
                ventaDao.insertar(cn, venta);

                // Siempre en el mismo orden (por id de variante) para evitar deadlocks.
                for (DetalleVenta d : venta.getDetallesOrdenadosPorVariante()) {
                    ventaDao.insertarDetalle(cn, venta.getIdVenta(), d);

                    // UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?
                    // Devuelve false si no habia stock suficiente.
                    if (!varianteDao.descontarStock(cn, d.getIdVariante(), d.getCantidad())) {
                        // Es RuntimeException: salta al catch de abajo y hace rollback.
                        throw new StockInsuficienteException(d.getDescripcion());
                    }

                    // Orden de deVenta: (idVenta, idVariante, cantidad, idUsuario).
                    movimientoDao.insertar(cn, MovimientoStock.deVenta(
                            venta.getIdVenta(), d.getIdVariante(), d.getCantidad(), idUsuario));
                }
                cn.commit();   // todo salio bien: se guarda TODO junto
                return venta;
            } catch (RuntimeException | SQLException e) {
                cn.rollback(); // algo fallo: se deshace TODO lo hecho en esta transaccion
                throw e;
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }
}
