// Módulo para peticiones a la API del sistema
const API = {
    // Función base para realizar peticiones GET
    async get(endpoint) {
        try {
            const response = await fetch(endpoint);
            if (!response.ok) {
                throw new Error(`Error HTTP: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error("Error en la petición API:", error);
            throw error;
        }
    },

    // Prueba de estado del servicio
    async verificarSalud() {
        return await this.get('/SistemaVentas/api/salud');
    }
};
