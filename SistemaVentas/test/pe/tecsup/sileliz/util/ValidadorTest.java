package pe.tecsup.sileliz.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidadorTest {

    private final Set<String> tallasPermitidas = new HashSet<>(
            Arrays.asList("XL", "2XL", "3XL")
    );

    @Test
    public void esObligatorio_conTexto_devuelveTrue() {
        assertTrue(Validador.esObligatorio("Blusa"));
    }

    @Test
    public void esObligatorio_conEspacios_devuelveFalse() {
        assertFalse(Validador.esObligatorio("   "));
    }

    @Test
    public void esObligatorio_conNull_devuelveFalse() {
        assertFalse(Validador.esObligatorio(null));
    }

    @Test
    public void cumpleLongitudMaxima_conLongitudMenor_devuelveTrue() {
        assertTrue(Validador.cumpleLongitudMaxima("Blusa", 10));
    }

    @Test
    public void cumpleLongitudMaxima_conLongitudExacta_devuelveTrue() {
        assertTrue(Validador.cumpleLongitudMaxima("Blusa", 5));
    }

    @Test
    public void cumpleLongitudMaxima_conLongitudMayor_devuelveFalse() {
        assertFalse(Validador.cumpleLongitudMaxima("Blusa", 4));
    }

    @Test
    public void cumpleLongitudMaxima_conNull_devuelveFalse() {
        assertFalse(Validador.cumpleLongitudMaxima(null, 10));
    }

    @Test
    public void esNumeroPositivo_conNumeroPositivo_devuelveTrue() {
        assertTrue(Validador.esNumeroPositivo(new BigDecimal("10.50")));
    }

    @Test
    public void esNumeroPositivo_conCero_devuelveFalse() {
        assertFalse(Validador.esNumeroPositivo(BigDecimal.ZERO));
    }

    @Test
    public void esNumeroPositivo_conNumeroNegativo_devuelveFalse() {
        assertFalse(Validador.esNumeroPositivo(new BigDecimal("-5")));
    }

    @Test
    public void esNumeroPositivo_conNull_devuelveFalse() {
        assertFalse(Validador.esNumeroPositivo(null));
    }

    @Test
    public void esNumeroNoNegativo_conNumeroPositivo_devuelveTrue() {
        assertTrue(Validador.esNumeroNoNegativo(5));
    }

    @Test
    public void esNumeroNoNegativo_conCero_devuelveTrue() {
        assertTrue(Validador.esNumeroNoNegativo(0));
    }

    @Test
    public void esNumeroNoNegativo_conNumeroNegativo_devuelveFalse() {
        assertFalse(Validador.esNumeroNoNegativo(-1));
    }

    @Test
    public void soloDigitos_conNumeros_devuelveTrue() {
        assertTrue(Validador.soloDigitos("123456"));
    }

    @Test
    public void soloDigitos_conLetras_devuelveFalse() {
        assertFalse(Validador.soloDigitos("ABC123"));
    }

    @Test
    public void soloDigitos_conEspacios_devuelveFalse() {
        assertFalse(Validador.soloDigitos("123 456"));
    }

    @Test
    public void soloDigitos_conNull_devuelveFalse() {
        assertFalse(Validador.soloDigitos(null));
    }

    @Test
    public void tieneLongitudExacta_conLongitudCorrecta_devuelveTrue() {
        assertTrue(Validador.tieneLongitudExacta("12345678", 8));
    }

    @Test
    public void tieneLongitudExacta_conLongitudIncorrecta_devuelveFalse() {
        assertFalse(Validador.tieneLongitudExacta("1234567", 8));
    }

    @Test
    public void tieneLongitudExacta_conNull_devuelveFalse() {
        assertFalse(Validador.tieneLongitudExacta(null, 8));
    }

    @Test
    public void esCorreo_conCorreoValido_devuelveTrue() {
        assertTrue(Validador.esCorreo("usuario@gmail.com"));
    }

    @Test
    public void esCorreo_sinArroba_devuelveFalse() {
        assertFalse(Validador.esCorreo("usuariogmail.com"));
    }

    @Test
    public void esCorreo_sinDominio_devuelveFalse() {
        assertFalse(Validador.esCorreo("usuario@"));
    }

    @Test
    public void esCorreo_conNull_devuelveFalse() {
        assertFalse(Validador.esCorreo(null));
    }

    @Test
    public void esDni_conOchoDigitos_devuelveTrue() {
        assertTrue(Validador.esDni("12345678"));
    }

    @Test
    public void esDni_conMenosDeOchoDigitos_devuelveFalse() {
        assertFalse(Validador.esDni("1234567"));
    }

    @Test
    public void esDni_conLetras_devuelveFalse() {
        assertFalse(Validador.esDni("1234567A"));
    }

    @Test
    public void esDni_conNull_devuelveFalse() {
        assertFalse(Validador.esDni(null));
    }

    @Test
    public void esCelular_conNueveDigitosYEmpiezaEnNueve_devuelveTrue() {
        assertTrue(Validador.esCelular("987654321"));
    }

    @Test
    public void esCelular_conOchoDigitos_devuelveFalse() {
        assertFalse(Validador.esCelular("98765432"));
    }

    @Test
    public void esCelular_siNoEmpiezaEnNueve_devuelveFalse() {
        assertFalse(Validador.esCelular("887654321"));
    }

    @Test
    public void esCelular_conLetras_devuelveFalse() {
        assertFalse(Validador.esCelular("98765432A"));
    }

    @Test
    public void esCelular_conNull_devuelveFalse() {
        assertFalse(Validador.esCelular(null));
    }

    @Test
    public void esTallaPermitida_conTallaRegistrada_devuelveTrue() {
        assertTrue(Validador.esTallaPermitida("XL", tallasPermitidas));
    }

    @Test
    public void esTallaPermitida_conMinusculas_devuelveTrue() {
        assertTrue(Validador.esTallaPermitida("xl", tallasPermitidas));
    }

    @Test
    public void esTallaPermitida_conEspaciosDevuelveTrue() {
        assertTrue(Validador.esTallaPermitida(" XL ", tallasPermitidas));
    }

    @Test
    public void esTallaPermitida_conTallaNoRegistrada_devuelveFalse() {
        assertFalse(Validador.esTallaPermitida("M", tallasPermitidas));
    }

    @Test
    public void esTallaPermitida_conConjuntoNulo_devuelveFalse() {
        assertFalse(Validador.esTallaPermitida("XL", null));
    }

    @Test
    public void esTallaPermitida_conTallaNula_devuelveFalse() {
        assertFalse(Validador.esTallaPermitida(null, tallasPermitidas));
    }
}
