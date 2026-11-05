/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.tecsup.sileliz.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import pe.tecsup.sileliz.descuento.Descuento;

/**
 * Una venta (tabla venta). Calcula sus propios totales con BigDecimal.
 * Los precios incluyen IGV (RN-01).
 */
public class Venta {

    /** IGV de 18 %, constante del sistema (RN-01). */
    public static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");

    private int idVenta;
    private String serie;
    private int numero;
    private LocalDateTime fechaHora;
    private Integer idCliente;      // nulo = consumidor final (RN-06)
    private int idUsuario;
    private List<DetalleVenta> detalles = new ArrayList<>();
    private BigDecimal baseImponible = BigDecimal.ZERO;
    private BigDecimal descuento = BigDecimal.ZERO;
    private BigDecimal igv = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private MetodoPago metodoPago;
    private EstadoVenta estado = EstadoVenta.EMITIDA;
    private String motivoAnulacion;

    /** Constructor vacio: sirve para llenar el objeto desde una fila de la base. */
    public Venta() {
    }

    public Venta(Integer idCliente, int idUsuario, MetodoPago metodoPago) {
        this.idCliente = idCliente;
        this.idUsuario = idUsuario;
        this.metodoPago = metodoPago;
        this.fechaHora = LocalDateTime.now();
    }

    /** Agrega una linea. Si la variante ya esta en el carrito, suma la cantidad. */
    public void agregarDetalle(DetalleVenta nuevo) {
        for (DetalleVenta d : detalles) {
            if (d.getIdVariante() == nuevo.getIdVariante()) {
                d.setCantidad(d.getCantidad() + nuevo.getCantidad());
                return;
            }
        }
        detalles.add(nuevo);
    }

    /** Suma de los subtotales de todas las lineas (antes del descuento). */
    public BigDecimal totalBruto() {
        BigDecimal bruto = BigDecimal.ZERO;
        for (DetalleVenta d : detalles) {
            bruto = bruto.add(d.subtotal());
        }
        return bruto;
    }

    /**
     * Calcula descuento, total, base imponible e IGV (seccion 5.4 del Plan).
     * El descuento nunca supera el total bruto (RN-12).
     */
    public void calcularTotales(Descuento dto) {
        BigDecimal bruto = totalBruto();
        BigDecimal desc = (dto == null) ? BigDecimal.ZERO : dto.calcular(detalles);
        if (desc == null || desc.signum() < 0) {
            desc = BigDecimal.ZERO;
        }
        if (desc.compareTo(bruto) > 0) {
            desc = bruto;
        }
        desc = desc.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalFinal = bruto.subtract(desc).setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = totalFinal.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);

        this.descuento = desc;
        this.total = totalFinal;
        this.baseImponible = base;
        this.igv = totalFinal.subtract(base);
    }

    /** Marca la venta como anulada. El motivo es obligatorio (RN-08). */
    public void anular(String motivo) {
        if (estado == EstadoVenta.ANULADA) {
            throw new IllegalStateException("La venta ya esta anulada.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de anulacion es obligatorio.");
        }
        this.estado = EstadoVenta.ANULADA;
        this.motivoAnulacion = motivo.trim();
    }

    /** Detalles ordenados por id de variante: evita bloqueos cruzados (seccion 4.4). */
    public List<DetalleVenta> getDetallesOrdenadosPorVariante() {
        List<DetalleVenta> copia = new ArrayList<>(detalles);
        copia.sort(Comparator.comparingInt(DetalleVenta::getIdVariante));
        return copia;
    }

    /** Texto del comprobante, por ejemplo B001-000015. */
    public String serieNumero() {
        return String.format("%s-%06d", serie, numero);
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venta)) {
            return false;
        }
        Venta otra = (Venta) o;
        return idVenta != 0 && idVenta == otra.idVenta;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVenta);
    }

    @Override
    public String toString() {
        return "Venta " + serie + "-" + numero + " total " + total;
    }
}
