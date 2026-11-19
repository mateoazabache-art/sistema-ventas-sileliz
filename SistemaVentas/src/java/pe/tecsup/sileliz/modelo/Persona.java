package pe.tecsup.sileliz.modelo;

import java.time.LocalDateTime;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;

/**
 * Padre de la herencia Persona -> Cliente / Usuario (secciones 5.1 y 5.2).
 * Es abstracta: no existe "una persona" suelta en el sistema, siempre es
 * un cliente o un usuario. Coincide con la tabla persona.
 */
public abstract class Persona {

    private int idPersona;
    private String nombres;
    private String apellidos;
    private String dni;
    private String telefono;
    private String correo;
    private String direccion;
    private boolean activo = true;
    private LocalDateTime fechaRegistro;

    protected Persona() {
    }

    protected Persona(String nombres, String apellidos, String dni) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
    }

    /** Cada hija dice que tipo de persona es ("CLIENTE" o "USUARIO"). */
    public abstract String getTipo();

    /** "Ana Ruiz" */
    public String nombreCompleto() {
        return (nombres == null ? "" : nombres.trim()) + " " + (apellidos == null ? "" : apellidos.trim());
    }

    /**
     * RN-04 antes de ir a la base: nombres y apellidos obligatorios,
     * DNI de 8 digitos y telefono (si hay) de 9 digitos que empieza con 9.
     * La base tiene los mismos CHECK como ultima barrera.
     */
    public void validar() {
        if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) {
            throw new DatoInvalidoException("Complete los nombres y apellidos.");
        }
        if (dni == null || !dni.matches("\\d{8}")) {
            throw new DatoInvalidoException("El DNI debe tener exactamente 8 dígitos.");
        }
        if (telefono != null && !telefono.isBlank() && !telefono.matches("9\\d{8}")) {
            throw new DatoInvalidoException("El teléfono debe tener 9 dígitos y empezar con 9.");
        }
    }

    public int getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(int idPersona) {
        this.idPersona = idPersona;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
