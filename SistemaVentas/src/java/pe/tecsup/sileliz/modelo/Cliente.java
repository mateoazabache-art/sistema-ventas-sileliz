package pe.tecsup.sileliz.modelo;

/**
 * Cliente de la tienda. Hereda de Persona y comparte id_persona
 * (tablas persona + cliente). Se guarda con dos INSERT en una transaccion.
 */
public class Cliente extends Persona {

    private String observaciones;

    public Cliente() {
    }

    public Cliente(String nombres, String apellidos, String dni) {
        super(nombres, apellidos, dni);
    }

    @Override
    public String getTipo() {
        return "CLIENTE";
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
