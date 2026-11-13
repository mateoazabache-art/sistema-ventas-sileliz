package pe.tecsup.sileliz.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Test;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.modelo.Cliente;

/**
 * Lectura de JSON (contrato 3.7): todo JSON mal escrito debe terminar en 400,
 * nunca en una excepcion que llegue a Tomcat como 500 con la traza del error.
 */
public class JsonTest {

    /** Clase de ayuda con una fecha sin hora (como los filtros de reportes). */
    private static class ConFecha {
        LocalDate dia;
    }

    @Test
    public void fechaHoraValidaSeLee() {
        Cliente c = Json.leer(new StringReader("{\"fechaRegistro\":\"2026-11-12T10:30:00\"}"), Cliente.class);
        assertEquals(LocalDateTime.of(2026, 11, 12, 10, 30), c.getFechaRegistro());
    }

    @Test
    public void fechaHoraNulaSeAcepta() {
        Cliente c = Json.leer(new StringReader("{\"fechaRegistro\":null}"), Cliente.class);
        assertNull(c.getFechaRegistro());
    }

    @Test
    public void fechaHoraMalEscritaEs400() {
        // Antes del arreglo: DateTimeParseException -> 500 de Tomcat con la traza
        esperar400("{\"fechaRegistro\":\"ayer\"}", Cliente.class);
        esperar400("{\"fechaRegistro\":\"12/11/2026\"}", Cliente.class);
        esperar400("{\"fechaRegistro\":123}", Cliente.class);
    }

    @Test
    public void fechaMalEscritaEs400() {
        esperar400("{\"dia\":\"2026-02-30\"}", ConFecha.class);
        assertEquals(LocalDate.of(2026, 11, 12),
                Json.leer(new StringReader("{\"dia\":\"2026-11-12\"}"), ConFecha.class).dia);
    }

    @Test
    public void jsonRotoOVacioEs400() {
        esperar400("hola", Cliente.class);
        esperar400("[]", Cliente.class);
        esperar400("", Cliente.class);
        esperar400("{\"idPersona\":\"x\"}", Cliente.class);
    }

    private static void esperar400(String json, Class<?> tipo) {
        try {
            Json.leer(new StringReader(json), tipo);
            fail("Debio responder 400: " + json);
        } catch (DatoInvalidoException e) {
            assertEquals(400, e.getCodigoHttp());
        }
    }
}
