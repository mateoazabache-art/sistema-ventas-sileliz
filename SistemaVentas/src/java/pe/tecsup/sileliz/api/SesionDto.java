package pe.tecsup.sileliz.api;

import pe.tecsup.sileliz.modelo.Usuario;

/**
 * Lo que la pagina recibe sobre el usuario conectado (plan 6.1).
 * Nunca incluye la clave ni el hash. Gson lo convierte en:
 *   {"id":12,"nombre":"Sofia Administradora","usuario":"admin","rol":"ADMINISTRADOR","rolEtiqueta":"Administrador"}
 */
public record SesionDto(int id, String nombre, String usuario, String rol, String rolEtiqueta) {

    public static SesionDto de(Usuario u) {
        return new SesionDto(u.getIdPersona(), u.nombreCompleto(), u.getNombreUsuario(),
                u.getRol().name(), u.getRol().getEtiqueta());
    }
}
