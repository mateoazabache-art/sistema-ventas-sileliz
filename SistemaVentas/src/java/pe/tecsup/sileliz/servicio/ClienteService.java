    package pe.tecsup.sileliz.servicio;

import pe.tecsup.sileliz.dao.ClienteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Cliente;
import pe.tecsup.sileliz.util.Paginado;
import pe.tecsup.sileliz.util.Validador;

/**
 * Reglas de negocio de clientes (plan 5.6): valida y llama al DAO.
 * No conoce HTTP ni escribe SQL.
 *   - RN-04: DNI de 8 digitos y telefono de 9 que empieza con 9 (Persona.validar).
 *   - RN-05: no se borra, se desactiva.
 *   - Largos maximos iguales a las columnas (plan 4.3) y correo valido:
 *     el navegador ayuda, pero aqui es donde de verdad se exige.
 */
public class ClienteService {

    private final ClienteDao dao;

    public ClienteService() {
        this(new ClienteDao());
    }

    /** Constructor para pruebas JUnit (se le pasa un DAO falso). */
    public ClienteService(ClienteDao dao) {
        this.dao = dao;
    }

    public Paginado<Cliente> listar(int pagina, int tam, String texto) {
        return dao.listarPaginado(pagina, tam, texto);
    }

    public Cliente buscarPorId(int id) {
        return dao.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el cliente " + id + "."));
    }

    /** Lo usa la venta (Luz): busca por DNI y avisa si no existe. */
    public Cliente buscarPorDni(String dni) {
        if (dni == null || !dni.trim().matches("\\d{8}")) {
            throw new DatoInvalidoException("El DNI debe tener exactamente 8 dígitos.");
        }
        return dao.buscarPorDni(dni.trim())
                .orElseThrow(() -> new NoEncontradoException("No hay un cliente con el DNI " + dni.trim() + "."));
    }

    /** Valida y guarda un cliente nuevo (activo). Devuelve el cliente con su id. */
    public Cliente registrar(Cliente c) {
        if (c == null) {
            throw new DatoInvalidoException("Faltan los datos del cliente.");
        }
        validar(c);
        c.setActivo(true);
        return dao.insertar(c);
    }

    /** Valida y actualiza. El id viene de la URL, no del JSON. */
    public Cliente actualizar(int id, Cliente c) {
        if (c == null) {
            throw new DatoInvalidoException("Faltan los datos del cliente.");
        }
        c.setIdPersona(id);
        validar(c);
        if (!dao.actualizar(c)) {
            throw new NoEncontradoException("No existe el cliente " + id + ".");
        }
        return buscarPorId(id);
    }

    /** RN-05: desactiva en lugar de borrar. */
    public void desactivar(int id) {
        if (!dao.desactivar(id)) {
            throw new NoEncontradoException("No existe el cliente " + id + ".");
        }
    }

    // ------------------------------------------------------------------
    // Validacion del servidor
    // ------------------------------------------------------------------

    /**
     * Limpia espacios, convierte los opcionales vacios en null y valida.
     * Persona.validar() revisa RN-04 (nombres, DNI y telefono); aqui se agrega
     * el correo y los largos, para responder 400 con un mensaje claro en vez
     * de dejar que la base rechace el dato con un error tecnico.
     */
    private void validar(Cliente c) {
        c.setNombres(limpiar(c.getNombres()));
        c.setApellidos(limpiar(c.getApellidos()));
        c.setDni(limpiar(c.getDni()));
        c.setTelefono(opcional(c.getTelefono()));
        c.setCorreo(opcional(c.getCorreo()));
        c.setDireccion(opcional(c.getDireccion()));
        c.setObservaciones(opcional(c.getObservaciones()));

        c.validar();

        maximo(c.getNombres(), 80, "Nombres");
        maximo(c.getApellidos(), 80, "Apellidos");
        maximo(c.getCorreo(), 120, "Correo");
        maximo(c.getDireccion(), 150, "Dirección");
        maximo(c.getObservaciones(), 200, "Observaciones");

        if (c.getCorreo() != null && !Validador.esCorreo(c.getCorreo())) {
            throw new DatoInvalidoException("El correo no es válido. Ejemplo: nombre@correo.com");
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String opcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static void maximo(String valor, int largo, String campo) {
        if (valor != null && !Validador.cumpleLongitudMaxima(valor, largo)) {
            throw new DatoInvalidoException("El campo " + campo + " admite como máximo " + largo + " caracteres.");
        }
    }
}
