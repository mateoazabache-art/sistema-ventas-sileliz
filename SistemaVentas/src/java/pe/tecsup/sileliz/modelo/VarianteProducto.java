
package pe.tecsup.sileliz.modelo;

import java.util.Objects;

public class VarianteProducto {

    private int idVariante;
    private Producto producto;
    private String talla;
    private String color;
    private String sku;
    private int stock;
    private int stockMinimo;
    private boolean activo;

    public VarianteProducto() {
    }

    public VarianteProducto(int idVariante, Producto producto,
            String talla, String color, String sku,
            int stock, int stockMinimo, boolean activo) {
        this.idVariante = idVariante;
        this.producto = producto;
        this.talla = talla;
        this.color = color;
        this.sku = sku;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(int idVariante) {
        this.idVariante = idVariante;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean hayStock() {
        return stock > 0;
    }

    public boolean estaBajoMinimo() {
        return stock <= stockMinimo;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof VarianteProducto)) {
            return false;
        }
        VarianteProducto variante = (VarianteProducto) objeto;
        return idVariante == variante.idVariante;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVariante);
    }
}