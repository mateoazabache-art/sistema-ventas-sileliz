package pe.tecsup.sileliz.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import pe.tecsup.sileliz.excepcion.DatoInvalidoException;
import pe.tecsup.sileliz.excepcion.SilelizException;

/**
 * Lectura y escritura de JSON para todos los Servlets (contrato 3.7 del plan).
 *
 *   Json.responder(resp, 200, objeto);   // exito
 *   Json.error(resp, e);                 // {"error": "..."} con el codigo de la excepcion
 *   Cliente c = Json.leer(req, Cliente.class);
 *
 * Fechas: se envian como texto ISO ("2026-11-01T10:30:00"). Sin estos
 * adaptadores, Gson falla con LocalDateTime en Java 17.
 */
public final class Json {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new FechaHoraAdapter().nullSafe())
            .registerTypeAdapter(LocalDate.class, new FechaAdapter().nullSafe())
            .create();

    private Json() {
    }

    /** El Gson configurado del proyecto (usar este, no new Gson()). */
    public static Gson gson() {
        return GSON;
    }

    /** Responde un objeto como JSON con el codigo HTTP indicado y UTF-8. */
    public static void responder(HttpServletResponse resp, int codigo, Object datos) throws IOException {
        resp.setStatus(codigo);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(GSON.toJson(datos));
    }

    /** Responde {"error": mensaje} con el codigo HTTP de la excepcion. */
    public static void error(HttpServletResponse resp, SilelizException e) throws IOException {
        responder(resp, e.getCodigoHttp(), Map.of("error", e.getMessage()));
    }

    /** Lee el cuerpo JSON de la peticion. Si esta vacio o mal formado: 400. */
    public static <T> T leer(HttpServletRequest req, Class<T> tipo) throws IOException {
        return leer(req.getReader(), tipo);
    }

    static <T> T leer(Reader reader, Class<T> tipo) {
        try {
            T objeto = GSON.fromJson(reader, tipo);
            if (objeto == null) {
                throw new DatoInvalidoException("La petición no trae datos.");
            }
            return objeto;
        } catch (JsonParseException e) {
            throw new DatoInvalidoException("El formato de los datos no es válido.", e);
        }
    }

    // ---- Adaptadores de fechas (texto ISO) ----

    private static final class FechaHoraAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime valor) throws IOException {
            out.value(valor.withNano(0).toString());
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            return LocalDateTime.parse(in.nextString());
        }
    }

    private static final class FechaAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate valor) throws IOException {
            out.value(valor.toString());
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            return LocalDate.parse(in.nextString());
        }
    }
}
