package pe.tecsup.sileliz.util;

import java.util.List;

/**
 * Una pagina de resultados (contrato 3.7 del plan). Gson lo convierte en:
 *   {"items":[...],"pagina":1,"tam":10,"total":47,"paginas":5}
 *
 * Uso en un DAO:
 *   int tam = Paginado.normalizarTam(tamPedido);
 *   ... SELECT ... LIMIT tam OFFSET Paginado.offset(pagina, tam)
 *   ... SELECT count(*) ... (mismo WHERE)
 *   return Paginado.de(lista, pagina, tam, total);
 *
 * Es generico (<T>): sirve para Paginado<Producto>, Paginado<Cliente>, etc.
 */
public record Paginado<T>(List<T> items, int pagina, int tam, long total, int paginas) {

    public static final int TAM_POR_DEFECTO = 10;
    public static final int TAM_MAXIMO = 50;

    /** Crea la pagina y calcula cuantas paginas hay en total. */
    public static <T> Paginado<T> de(List<T> items, int pagina, int tam, long total) {
        int t = Math.max(tam, 1);
        int paginas = (int) ((total + t - 1) / t);   // division redondeada hacia arriba
        return new Paginado<>(List.copyOf(items), pagina, tam, total, paginas);
    }

    /** Filas que hay que saltar en el OFFSET (pagina 1 -> 0). */
    public static int offset(int pagina, int tam) {
        return (Math.max(pagina, 1) - 1) * tam;
    }

    /** Evita tamanos absurdos que vengan del navegador (?tam=100000). */
    public static int normalizarTam(int tam) {
        if (tam <= 0) {
            return TAM_POR_DEFECTO;
        }
        return Math.min(tam, TAM_MAXIMO);
    }
}
