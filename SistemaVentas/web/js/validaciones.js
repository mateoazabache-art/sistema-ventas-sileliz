const Validaciones = {

    esObligatorio(valor) {
        return valor !== null && valor.trim() !== "";
    },

    cumpleLongitudMaxima(valor, longitud) {
        return valor !== null && valor.length <= longitud;
    },

    esNumeroPositivo(valor) {
        return valor !== "" && Number.isFinite(Number(valor))
            && Number(valor) > 0;
    },

    esNumeroNoNegativo(valor) {
        return valor !== "" && Number.isInteger(Number(valor))
            && Number(valor) >= 0;
    },

    soloDigitos(valor) {
        return /^\d+$/.test(valor);
    },

    tieneLongitudExacta(valor, longitud) {
        return valor !== null && valor.length === longitud;
    },

    esCorreo(correo) {
        return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(correo);
    },

    esDni(dni) {
        return this.soloDigitos(dni)
            && this.tieneLongitudExacta(dni, 8);
    },

    esCelular(celular) {
        return this.soloDigitos(celular)
            && this.tieneLongitudExacta(celular, 9)
            && celular.startsWith("9");
    },

    esTallaPermitida(talla, tallasPermitidas) {
        return talla !== null
            && tallasPermitidas.includes(talla.trim().toUpperCase());
    }

};
