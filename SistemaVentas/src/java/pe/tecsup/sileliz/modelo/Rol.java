package pe.tecsup.sileliz.modelo;

/**
 * Rol del usuario. Se guarda con name() en usuario.rol: el mismo texto en Java
 * y en el CHECK de la base ('ADMINISTRADOR' o 'VENDEDOR').
 */
public enum Rol {
    ADMINISTRADOR("Administrador"),
    VENDEDOR("Vendedor");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto para mostrar en pantalla. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
