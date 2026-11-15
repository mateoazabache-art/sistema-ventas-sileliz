package pe.tecsup.sileliz.modelo;

/**
 * Lo que envia la pagina para registrar un ingreso de mercaderia:
 *   POST /api/ingresos  {"idVariante": 11, "cantidad": 12, "motivo": "Compra a Gamarra, guia 0123"}
 *
 * Solo ids, cantidad y texto: el usuario sale de la sesion y el stock lo calcula la base.
 * cantidad es Integer (no int) para distinguir "no lo enviaron" (null) de un 0.
 */
public class IngresoSolicitud {

    private int idVariante;
    private Integer cantidad;
    private String motivo;

    public IngresoSolicitud() {
    }

    public IngresoSolicitud(int idVariante, Integer cantidad, String motivo) {
        this.idVariante = idVariante;
        this.cantidad = cantidad;
        this.motivo = motivo;
    }

    public int getIdVariante() {
        return idVariante;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public String getMotivo() {
        return motivo;
    }
}
