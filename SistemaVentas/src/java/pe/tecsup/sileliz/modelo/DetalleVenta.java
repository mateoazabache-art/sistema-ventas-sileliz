package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una linea de una venta (tabla detalle_venta).
 * Guarda copia de la descripcion y del precio al momento de vender (RN-09),
 * asi que cambiar el producto despues no altera ventas pasadas.
 */
public class DetalleVenta {

    private int idDetalle;
    private int idVariante;
    private String descripcion;
    private int cantidad;
    private BigDecimal precioUnitario;

    /** Constructor vacio: sirve para llenar el objeto desde una fila de la base. */
    public DetalleVenta() {
    }

    public DetalleVenta(int idVariante, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this.idVariante = idVariante;
        this.descripcion = descripcion;
        setCantidad(cantidad);
        setPrecioUnitario(precioUnitario);
    }

    /** Subtotal de la linea = precio unitario x cantidad. */
    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0.");
        }
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        if (precioUnitario == null || precioUnitario.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0.");
        }
        this.precioUnitario = precioUnitario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DetalleVenta)) {
            return false;
        }
        DetalleVenta otro = (DetalleVenta) o;
        return idDetalle != 0 && idDetalle == otro.idDetalle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDetalle);
    }

    @Override
    public String toString() {
        return descripcion + " x" + cantidad + " a " + precioUnitario;
    }
}