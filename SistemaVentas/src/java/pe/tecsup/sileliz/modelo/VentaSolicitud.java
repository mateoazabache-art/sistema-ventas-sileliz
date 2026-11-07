package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que llega en el JSON de POST /api/ventas (DTO, no se guarda).
 * El navegador solo envia ids, cantidades y el tipo de descuento:
 * los precios y los totales los calcula el servidor.
 */
public class VentaSolicitud {

    /** Una linea del carrito: que variante y cuantas unidades. */
    public static class Linea {
        private int idVariante;
        private int cantidad;

        public Linea() {
        }

        public Linea(int idVariante, int cantidad) {
            this.idVariante = idVariante;
            this.cantidad = cantidad;
        }

        public int getIdVariante() {
            return idVariante;
        }

        public void setIdVariante(int idVariante) {
            this.idVariante = idVariante;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }
    }

    private Integer idCliente;          // nulo = consumidor final (RN-06)
    private MetodoPago metodoPago;
    private String tipoDescuento;       // ej. SIN, PORCENTAJE, POR_CANTIDAD
    private BigDecimal valorDescuento;  // ej. 10 para 10 %
    private List<Linea> lineas = new ArrayList<>();

    public VentaSolicitud() {
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getTipoDescuento() {
        return tipoDescuento;
    }

    public void setTipoDescuento(String tipoDescuento) {
        this.tipoDescuento = tipoDescuento;
    }

    public BigDecimal getValorDescuento() {
        return valorDescuento;
    }

    public void setValorDescuento(BigDecimal valorDescuento) {
        this.valorDescuento = valorDescuento;
    }

    public List<Linea> getLineas() {
        return lineas;
    }

    public void setLineas(List<Linea> lineas) {
        this.lineas = lineas;
    }
}