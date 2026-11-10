package pe.tecsup.sileliz.servicio;

import pe.tecsup.sileliz.dao.ClienteDao;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.NoEncontradoException;
import pe.tecsup.sileliz.modelo.Cliente;
import pe.tecsup.sileliz.util.Paginado;

/**
 * Reglas de negocio de clientes (plan 5.6): valida y llama al DAO.
 * No conoce HTTP ni escribe SQL.
 *   - RN-04: DNI de 8 digitos y telefono de 9 que empieza con 9 (Persona.validar).
 *   - RN-05: no se borra, se desactiva.
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
        c.validar();
        c.setActivo(true);
        return dao.insertar(c);
    }

    /** Valida y actualiza. El id viene de la URL, no del JSON. */
    public Cliente actualizar(int id, Cliente c) {
        if (c == null) {
            throw new DatoInvalidoException("Faltan los datos del cliente.");
        }
        c.setIdPersona(id);
        c.validar();
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
}
