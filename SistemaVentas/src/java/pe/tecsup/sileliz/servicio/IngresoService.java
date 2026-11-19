package pe.tecsup.sileliz.servicio;

import java.sql.Connection;
import java.sql.SQLException;
import pe.tecsup.sileliz.dao.MovimientoDao;
import pe.tecsup.sileliz.dao.VarianteDao;
import pe.tecsup.sileliz.excepcion.AccesoDenegadoException;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.IngresoRegistrado;
import pe.tecsup.sileliz.modelo.IngresoSolicitud;
import pe.tecsup.sileliz.modelo.MovimientoStock;
import pe.tecsup.sileliz.modelo.Usuario;
import pe.tecsup.sileliz.modelo.VarianteProducto;
import pe.tecsup.sileliz.util.Conexion;
import pe.tecsup.sileliz.util.ErrorSql;

/**
 * Ingreso de mercaderia (Rafael): sube el stock de una variante y lo anota en el kardex.
 *
 * Regla unica del stock (plan 4.4): el stock nunca se escribe a mano. Solo cambia por:
 *   VENTA (-)  ->  VentaService.registrar
 *   ANULACION (+) -> VentaService.anular
 *   INGRESO (+)   -> este servicio
 * y SIEMPRE junto con su fila en movimiento_stock, en la misma transaccion (todo o nada).
 *
 * Por que "stock = stock + ?" y no "stock = 20": si en ese mismo instante se registra una
 * venta, sumar respeta lo que la venta desconto; escribir un numero fijo lo pisaria.
 */
public class IngresoService {

    /** Mas de esto en un solo ingreso casi seguro es un error al escribir (ej. 1000 por 100). */
    public static final int CANTIDAD_MAXIMA = 999;
    public static final int LARGO_MOTIVO = 200;

    /** De donde sale la conexion. En produccion, Conexion; en las pruebas, una que hace rollback. */
    @FunctionalInterface
    public interface FuenteConexion {
        Connection abrir() throws SQLException;
    }

    private final VarianteDao varianteDao;
    private final MovimientoDao movimientoDao;
    private final FuenteConexion fuente;

    public IngresoService() {
        this(new VarianteDao(), new MovimientoDao(), Conexion::getConnection);
    }

    public IngresoService(VarianteDao varianteDao, MovimientoDao movimientoDao, FuenteConexion fuente) {
        this.varianteDao = varianteDao;
        this.movimientoDao = movimientoDao;
        this.fuente = fuente;
    }

    public IngresoRegistrado registrar(IngresoSolicitud s, Usuario usuario) {
        // 1. Permiso. El filtro ya lo exige; se repite aqui porque el servicio no debe
        //    depender de que alguien configure bien el filtro (defensa en profundidad).
        if (usuario == null || !usuario.esAdministrador()) {
            throw new AccesoDenegadoException("Solo el administrador registra ingresos de mercadería.");
        }

        // 2. Validar lo que llego del navegador (sin tocar la base).
        if (s == null) {
            throw new DatoInvalidoException("Faltan los datos del ingreso.");
        }
        if (s.getIdVariante() <= 0) {
            throw new DatoInvalidoException("Elija la talla y color que ingresa.");
        }
        Integer cantidad = s.getCantidad();
        if (cantidad == null || cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new DatoInvalidoException("La cantidad debe ser un entero entre 1 y " + CANTIDAD_MAXIMA + ".");
        }
        String motivo = s.getMotivo() == null ? "" : s.getMotivo().trim();
        if (motivo.isEmpty()) {
            throw new DatoInvalidoException("Escriba el motivo del ingreso (por ejemplo, el proveedor o la guía).");
        }
        if (motivo.length() > LARGO_MOTIVO) {
            throw new DatoInvalidoException("El motivo no puede pasar de " + LARGO_MOTIVO + " caracteres.");
        }

        // 3. Transaccion: todo lo que sigue usa la MISMA conexion. Todo o nada.
        try (Connection cn = fuente.abrir()) {
            cn.setAutoCommit(false);
            try {
                // La variante debe existir y estar a la venta (ella y su producto).
                VarianteProducto v = varianteDao.buscarPorId(cn, s.getIdVariante()).orElseThrow(
                        () -> new NoEncontradoException("No existe la talla y color elegida."));
                if (!v.isActivo() || v.getProducto() == null || !v.getProducto().isActivo()) {
                    throw new DatoInvalidoException("No se puede ingresar mercadería a una prenda desactivada.");
                }

                // Sumar stock + anotar en el kardex
                varianteDao.reponerStock(cn, v.getIdVariante(), cantidad);
                movimientoDao.insertar(cn, MovimientoStock.deIngreso(
                        v.getIdVariante(), cantidad, usuario.getIdPersona(), motivo));

                // Stock real despues del ingreso: lo lee la base, no se calcula aqui
                int stockActual = varianteDao.buscarPorId(cn, v.getIdVariante()).get().getStock();

                cn.commit();        // recien aqui se guarda todo junto
                return new IngresoRegistrado(v.getIdVariante(), v.getSku(), cantidad, stockActual);
            } catch (SQLException | RuntimeException e) {
                cn.rollback();      // si fallo el kardex, el stock tampoco sube
                throw e;
            }
        } catch (SQLException e) {
            throw ErrorSql.traducir(e);
        }
    }
}
