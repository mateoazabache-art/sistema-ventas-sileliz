/**
 * Módulo de cliente API para Sistema Ventas Sileliz
 * Maneja peticiones HTTP (GET, POST, etc.) con redirección automática si vence la sesión.
 */
const API = {
    // Determina la ruta base del proyecto dinámicamente
    baseUrl: window.location.origin + window.location.pathname.substring(0, window.location.pathname.indexOf('/', 1) + 1),

    /**
     * Petición HTTP genérica base
     */
    async request(endpoint, options = {}) {
        const url = endpoint.startsWith('http') || endpoint.startsWith('/') 
            ? endpoint 
            : `${this.baseUrl}${endpoint}`;

        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        try {
            const response = await fetch(url, config);

            // Redirección al login si la sesión caducó (HTTP 401 o 403)
            if (response.status === 401 || response.status === 403) {
                window.location.href = `${this.baseUrl}login.jsp`;
                return null;
            }

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(data.mensaje || `Error HTTP: ${response.status}`);
            }

            return data;
        } catch (error) {
            console.error(`Error en la petición API (${endpoint}):`, error);
            throw error;
        }
    },

    // Métodos estándar HTTP
    async get(endpoint) {
        return await this.request(endpoint, { method: 'GET' });
    },

    async post(endpoint, body) {
        return await this.request(endpoint, { 
            method: 'POST', 
            body: JSON.stringify(body) 
        });
    },

    // Compatibilidad y verificación
    async verificarSalud() {
        return await this.get('api/salud');
    },

    // Módulos de Autenticación (Login, Logout, Sesión)
    auth: {
        login: (credenciales) => API.post('api/login', credenciales),
        logout: () => API.post('api/logout', {}),
        obtenerSesion: () => API.get('api/sesion')
    },

    // Módulos de Reportes y Estadísticas
    reportes: {
        obtenerResumenDia: () => API.get('api/estadisticas/resumen'),
        obtenerVentasPorRango: (inicio, fin, pagina = 1) => 
            API.get(`api/reportes/ventas?inicio=${inicio}&fin=${fin}&pagina=${pagina}`),
        obtenerMasVendidos: () => API.get('api/reportes/mas-vendidos')
    }
};

window.API = API;