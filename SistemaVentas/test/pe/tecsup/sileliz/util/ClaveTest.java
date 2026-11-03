package pe.tecsup.sileliz.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas unitarias de Clave (PBKDF2 y politica RN-11).
 * No necesitan base de datos.
 */
public class ClaveTest {

    @Test
    public void laClaveCorrectaSeVerifica() {
        String hash = Clave.generar("Admin2026".toCharArray());
        assertTrue(Clave.verificar("Admin2026".toCharArray(), hash));
    }

    @Test
    public void unaClaveDistintaNoSeVerifica() {
        String hash = Clave.generar("Admin2026".toCharArray());
        assertFalse(Clave.verificar("admin2026".toCharArray(), hash));   // otra mayuscula
        assertFalse(Clave.verificar("Admin2025".toCharArray(), hash));
    }

    @Test
    public void laMismaClaveGeneraHashesDistintos() {
        // Cada hash lleva una sal al azar: dos usuarios con la misma clave no se delatan
        String h1 = Clave.generar("Venta2026".toCharArray());
        String h2 = Clave.generar("Venta2026".toCharArray());
        assertNotEquals(h1, h2);
    }

    @Test
    public void elHashTieneFormatoIteracionesSalHash() {
        String[] partes = Clave.generar("Venta2026".toCharArray()).split(":");
        assertEquals(3, partes.length);
        assertEquals("600000", partes[0]);
    }

    @Test
    public void unHashMalFormadoNoRompeElLogin() {
        assertFalse(Clave.verificar("Admin2026".toCharArray(), "basura"));
        assertFalse(Clave.verificar("Admin2026".toCharArray(), null));
        assertFalse(Clave.verificar(null, "600000:abc:def"));
    }

    @Test
    public void politicaRN11() {
        assertFalse("muy corta", Clave.cumplePolitica("Ab1".toCharArray()));
        assertFalse("sin numeros", Clave.cumplePolitica("abcdefgh".toCharArray()));
        assertFalse("sin letras", Clave.cumplePolitica("12345678".toCharArray()));
        assertTrue(Clave.cumplePolitica("Venta2026".toCharArray()));
    }
}
