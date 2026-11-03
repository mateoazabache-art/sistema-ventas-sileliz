package pe.tecsup.sileliz.model;

public class Cliente {
    private int id;
    private String dni;
    private String nombres;
    private String telefono;
    private String correo;
    private int estado; // 1: Activo, 0: Inactivo

    public Cliente() {}

    public Cliente(int id, String dni, String nombres, String telefono, String correo, int estado) {
        this.id = id;
        this.dni = dni;
        this.nombres = nombres;
        this.telefono = telefono;
        this.correo = correo;
        this.estado = estado;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }
}