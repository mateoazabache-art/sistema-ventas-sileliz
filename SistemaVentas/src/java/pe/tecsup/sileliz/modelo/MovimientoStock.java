package pe.tecsup.sileliz.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kardex: una fila por cada cambio de stock (tabla movimiento_stock).
 * La cantidad es negativa cuando el stock baja (venta) y positiva
 * cuando sube (anulacion, ingreso).
 */
public class MovimientoStock {

    /** Tipos permitidos; coinciden con el CHECK de la columna tipo. */
    public enum Tipo {
        VENTA,
        ANULACION,
        INGRESO,
        AJUSTE
    }

    private int idMovimiento;
    private int idVariante;
    private Integer idVenta;          // puede ser nulo (ej. un ingreso de mercaderia)
    private int idUsuario;
    private Tipo tipo;
    private int cantidad;
    private LocalDateTime fechaHora;
    private String motivo;

    /** Constructor vacio: sirve para llenar el objeto desde una fila de la base. */
    public MovimientoStock() {
    }

    public MovimientoStock(int idVariante, Integer idVenta, int idUsuario,
                           Tipo tipo, int cantidad, String motivo) {
        this.idVariante = idVariante;
        this.idVenta = idVenta;
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        setCantidad(cantidad);
        this.motivo = motivo;
        this.fechaHora = LocalDateTime.now();
    }

    /** Movimiento por una venta: el stock baja, por eso la cantidad es negativa. */
    public static MovimientoStock deVenta(int idVenta, int idVariante, int cantidadVendida, int idUsuario) {
        return new MovimientoStock(idVariante, idVenta, idUsuario,
                Tipo.VENTA, -cantidadVendida, "Venta");
    }

    /** Movimiento por una anulacion: el stock sube, por eso la cantidad es positiva. */
    public static MovimientoStock deAnulacion(int idVenta, int idVariante, int cantidadRepuesta,
                                              int idUsuario, String motivo) {
        return new MovimientoStock(idVariante, idVenta, idUsuario,
                Tipo.ANULACION, cantidadRepuesta, motivo);
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    /** La tabla exige cantidad distinta de 0. */
    public void setCantidad(int cantidad) {
        if (cantidad == 0) {
            throw new IllegalArgumentException("La cantidad del movimiento no puede ser 0.");
        }
        this.cantidad = cantidad;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MovimientoStock)) {
            return false;
        }
        MovimientoStock otro = (MovimientoStock) o;
        return idMovimiento != 0 && idMovimiento == otro.idMovimiento;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMovimiento);
    }

    @Override
    public String toString() {
        return tipo + " variante " + idVariante + " cantidad " + cantidad;
    }
}
