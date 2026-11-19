package pe.tecsup.sileliz.modelo;

/**
 * Lo que envia usuarios.html para crear o editar un usuario (CU-02):
 *   POST /api/usuarios       {"nombres":"Rosa","apellidos":"Quispe","dni":"71234567",
 *                             "telefono":"912345678","correo":null,"direccion":null,
 *                             "nombreUsuario":"rosa","rol":"VENDEDOR","clave":"Tienda2026"}
 *   PUT  /api/usuarios/{id}  lo mismo, sin "clave" (la clave se cambia aparte)
 *   POST /api/usuarios/{id}/clave  {"clave":"Nueva2026"}
 *
 * Es una clase aparte de Usuario porque Usuario no tiene la clave en texto
 * (solo el hash, que es transient). La clave en texto vive solo mientras
 * UsuarioService genera el hash; nunca se guarda ni se devuelve en un JSON.
 * rol llega como texto y UsuarioService lo valida (si no, Gson dejaria null).
 */
public class UsuarioSolicitud {

    private String nombres;
    private String apellidos;
    private String dni;
    private String telefono;
    private String correo;
    private String direccion;
    private String nombreUsuario;
    private String rol;
    private String clave;

    public UsuarioSolicitud() {
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

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
