package pe.tecsup.sileliz.servicio;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import pe.tecsup.sileliz.dao.MovimientoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.dao.VentaDao;
import pe.tecsup.sileliz.descuento.Descuento;
import pe.tecsup.sileliz.descuento.DescuentoFactory;
import pe.tecsup.sileliz.excepcion.AccesoDenegadoException;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.excepcion.StockInsuficienteException;
import pe.tecsup.sileliz.modelo.Comprobante;
import pe.tecsup.sileliz.modelo.DetalleVenta;
import pe.tecsup.sileliz.modelo.MovimientoStock;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.modelo.Venta;
import pe.tecsup.sileliz.modelo.VentaFila;
import pe.tecsup.sileliz.modelo.VentaSolicitud;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;
import pe.tecsup.sileliz.util.Paginado;

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

    /**
     * Venta completa para el comprobante (GET /api/ventas/{id}).
     * Plan 3.7: el vendedor solo ve SUS ventas; el administrador ve todas.
     */
    public Comprobante buscarPorId(int idVenta, Usuario u) {
        Comprobante c = ventaDao.buscarPorId(idVenta)
                .orElseThrow(() -> new NoEncontradoException("No existe la venta " + idVenta + "."));
        if (!u.esAdministrador() && c.venta().getIdUsuario() != u.getIdPersona()) {
            throw new AccesoDenegadoException("Solo puede ver sus propias ventas.");
        }
        return c;
    }


    /** Largo maximo del motivo (columna venta.motivo_anulacion). */
    private static final int MAX_MOTIVO = 200;

    /**
     * Anula una venta (POST /api/ventas/{id}/anular). Solo el administrador.
     * Todo o nada: marca ANULADA (RN-08), repone el stock de cada prenda y lo anota en el kardex.
     * La venta NO se borra (el trigger trg_venta_protegida lo impide): queda en el historial.
     */
    public Comprobante anular(int idVenta, String motivo, Usuario u) {
        // ===== A. VALIDAR (sin conexion) =====
        if (!u.esAdministrador()) {
            throw new AccesoDenegadoException("Solo el administrador puede anular ventas.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new DatoInvalidoException("Escriba el motivo de la anulación.");
        }
        String m = motivo.trim();
        if (m.length() > MAX_MOTIVO) {
            throw new DatoInvalidoException("El motivo no puede pasar de " + MAX_MOTIVO + " caracteres.");
        }
        Comprobante c = ventaDao.buscarPorId(idVenta)
                .orElseThrow(() -> new NoEncontradoException("No existe la venta " + idVenta + "."));
        try {
            c.venta().anular(m);   // regla del modelo: no se anula dos veces (RN-08)
        } catch (IllegalStateException e) {
            throw new DatoInvalidoException("La venta " + c.venta().serieNumero() + " ya está anulada.");
        }

        // ===== B. LA CAJA FUERTE (transaccion) =====
        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try {
                // WHERE estado = 'EMITIDA': si otro admin la anulo hace un instante, devuelve false.
                if (!ventaDao.anular(cn, idVenta, m)) {
                    throw new DatoInvalidoException("La venta " + c.venta().serieNumero() + " ya está anulada.");
                }
                // Mismo orden que al vender (por variante) para evitar deadlocks.
                for (DetalleVenta d : c.venta().getDetallesOrdenadosPorVariante()) {
                    varianteDao.reponerStock(cn, d.getIdVariante(), d.getCantidad());
                    // Orden de deAnulacion: (idVenta, idVariante, cantidad, idUsuario, motivo).
                    movimientoDao.insertar(cn, MovimientoStock.deAnulacion(
                            idVenta, d.getIdVariante(), d.getCantidad(), u.getIdPersona(), m));
                }
                cn.commit();
            } catch (RuntimeException | SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
        return buscarPorId(idVenta, u);   // el comprobante ya con estado ANULADA
    }

    /**
     * Historial de ventas (GET /api/ventas?desde=&hasta=&pagina=&tam=).
     * Los parametros llegan como texto desde la URL y se validan aqui (400 si estan mal).
     * Sin fechas: todas. El vendedor solo ve SUS ventas; el administrador, todas (plan 3.7).
     */
    public Paginado<VentaFila> listar(String desde, String hasta, String pagina, String tam, Usuario u) {
        LocalDate d = fecha(desde, "desde");
        LocalDate h = fecha(hasta, "hasta");
        if (d != null && h != null && d.isAfter(h)) {
            throw new DatoInvalidoException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\".");
        }
        int pag = entero(pagina, "página", 1);
        int t = entero(tam, "tamaño de página", Paginado.TAM_POR_DEFECTO);
        Integer idUsuario = u.esAdministrador() ? null : u.getIdPersona();
        return ventaDao.listarPaginado(pag, t, d, h, idUsuario);
    }

    /** Texto AAAA-MM-DD -> fecha. Vacio -> null (sin filtro). Mal escrito -> 400. */
    private static LocalDate fecha(String texto, String nombre) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException(
                    "La fecha \"" + nombre + "\" no es válida. Use el formato AAAA-MM-DD.");
        }
    }

    /** Texto -> entero. Vacio -> valor por defecto. Letras -> 400. */
    private static int entero(String texto, String nombre, int porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El valor de \"" + nombre + "\" debe ser un número entero.");
        }
    }
}
