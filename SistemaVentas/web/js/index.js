/**
 * index.html (Inicio) - tarea 10 de Cesia.
 * - Saludo con el nombre de la sesión: API.auth.obtenerSesion()
 * - Tarjetas del día: API.estadisticas.hoy() -> GET api/estadisticas/hoy
 *   {"fecha":"2026-11-13","ventas":3,"total":284.60,"anuladas":0,"clientes":10,"stockBajo":13}
 * - Accesos rápidos (Reportes solo para el ADMINISTRADOR).
 * Todo con textContent. Va dentro de una función para no chocar con los nombres de layout.js.
 */
(function () {
    const porId = (id) => document.getElementById(id);

    document.addEventListener('DOMContentLoaded', cargar);

    async function cargar() {
        try {
            const [sesion, datos] = await Promise.all([
                API.auth.obtenerSesion(),
                API.estadisticas.hoy()
            ]);
            if (!sesion || !datos) return;      // sin sesión: api.js ya mandó a login.html
            pintarSaludo(sesion);
            pintarResumen(datos);
        } catch (error) {
            mostrarMensaje(error.message);      // el mensaje viene del servidor
        }
    }

    function pintarSaludo(sesion) {
        const hora = new Date().getHours();
        const saludo = hora < 12 ? 'Buenos días' : (hora < 19 ? 'Buenas tardes' : 'Buenas noches');
        porId('saludo').textContent = saludo + ', ' + sesion.nombre;
        // Reportes solo lo ve el administrador (el servidor igual lo valida)
        porId('acceso-reportes').hidden = sesion.rol !== 'ADMINISTRADOR';
    }

    function pintarResumen(d) {
        porId('fecha-resumen').textContent = 'Resumen del día ' + fechaCorta(d.fecha);
        porId('valor-ventas').textContent = d.ventas;
        porId('nota-anuladas').textContent = d.anuladas > 0 ? 'Anuladas: ' + d.anuladas : '';
        porId('valor-total').textContent = 'S/ ' + Number(d.total || 0).toFixed(2);
        porId('valor-clientes').textContent = d.clientes;
        porId('valor-stock').textContent = d.stockBajo;

        // Stock bajo en rojo (RN-10) y con enlace a Productos si hay algo que atender
        const hayBajo = d.stockBajo > 0;
        porId('valor-stock').classList.toggle('stock-bajo', hayBajo);
        porId('enlace-stock').hidden = !hayBajo;
    }

    function mostrarMensaje(texto) {
        porId('mensaje').textContent = texto;
        porId('mensaje').hidden = false;
    }

    /** "2026-11-13" -> "13/11/2026" */
    function fechaCorta(iso) {
        const [a, m, d] = String(iso).substring(0, 10).split('-');
        return d + '/' + m + '/' + a;
    }
})();
