/**
 * Módulo de cliente API para Sistema Ventas Sileliz
 * Único lugar donde se llama a fetch (plan 5.6).
 * - 401 (sin sesión): redirige a login.html, salvo en el propio login.
 * - Otros errores: lanza Error con el mensaje que manda el servidor {"error": "..."}.
 */
const API = {
    // Determina la ruta base del proyecto dinámicamente (por ejemplo http://localhost:8080/sileliz/)
    baseUrl: window.location.origin + window.location.pathname.substring(0, window.location.pathname.indexOf('/', 1) + 1),

    /**
     * Petición HTTP genérica base.
     * El endpoint se escribe SIN barra inicial: 'api/clientes', no '/api/clientes'.
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

            // 401 = no hay sesión -> al login (salvo el propio login: ahí se muestra el error)
            if (response.status === 401 && !endpoint.includes('api/login')) {
                window.location.href = `${this.baseUrl}login.html`;
                return null;
            }

            const data = await response.json().catch(() => ({}));

            // 400, 403, 404, 409, 423, 500...: el mensaje claro viene del servidor
            if (!response.ok) {
                throw new Error(data.error || `Error HTTP: ${response.status}`);
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

    async put(endpoint, body) {
        return await this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    async delete(endpoint) {
        return await this.request(endpoint, { method: 'DELETE' });
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

    // Estadísticas del Inicio (Rafael)
    estadisticas: {
        hoy: () => API.get('api/estadisticas/hoy')
    }
};

window.API = API;