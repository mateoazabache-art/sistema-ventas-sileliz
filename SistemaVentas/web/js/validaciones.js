const Validaciones = {

    esObligatorio(valor) {
        return valor !== null && valor !== undefined
            && typeof valor === "string" && valor.trim() !== "";
    },

    cumpleLongitudMaxima(valor, longitud) {
        return valor !== null && valor !== undefined
            && typeof valor === "string" && valor.length <= longitud;
    },

    esNumeroPositivo(valor) {
        return (typeof valor === "string" || typeof valor === "number")
            && String(valor).trim() !== ""
            && Number.isFinite(Number(valor))
            && Number(valor) > 0;
    },

    esNumeroNoNegativo(valor) {
        return (typeof valor === "string" || typeof valor === "number")
            && String(valor).trim() !== ""
            && Number.isInteger(Number(valor))
            && Number(valor) >= 0;
    },

    soloDigitos(valor) {
        return typeof valor === "string" && /^\d+$/.test(valor);
    },

    tieneLongitudExacta(valor, longitud) {
        return valor !== null && valor !== undefined
            && typeof valor === "string" && valor.length === longitud;
    },

    esCorreo(correo) {
        return typeof correo === "string"
            && /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(correo);
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
        return typeof talla === "string"
            && Array.isArray(tallasPermitidas)
            && tallasPermitidas.includes(talla.trim().toUpperCase());
    }

};
