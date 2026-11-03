package pe.tecsup.sileliz.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Claves con PBKDF2 del JDK, sin librerias (seccion 6.3 del plan).
 *
 * Nunca se guarda la clave: se guarda "iteraciones:sal:hash".
 *   - sal: 16 bytes al azar, distinta para cada usuario. Dos personas con la
 *     misma clave tienen hashes distintos.
 *   - 600 000 iteraciones: probar millones de claves se vuelve muy lento.
 *
 * Se usa char[] y no String para poder borrar la clave de la memoria.
 */
public final class Clave {

    private static final int ITER = 600_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom AZAR = new SecureRandom();

    private Clave() {
    }

    /** Genera el texto que se guarda en usuario.clave_hash. */
    public static String generar(char[] clave) {
        byte[] sal = new byte[BYTES_SAL];
        AZAR.nextBytes(sal);
        return ITER + ":" + b64(sal) + ":" + b64(derivar(clave, sal, ITER));
    }

    /** true si la clave escrita corresponde al hash guardado. */
    public static boolean verificar(char[] clave, String guardado) {
        if (clave == null || guardado == null) {
            return false;
        }
        String[] p = guardado.split(":");
        if (p.length != 3) {
            return false;
        }
        try {
            byte[] esperado = Base64.getDecoder().decode(p[2]);
            byte[] obtenido = derivar(clave, Base64.getDecoder().decode(p[1]), Integer.parseInt(p[0]));
            return MessageDigest.isEqual(esperado, obtenido);   // comparacion en tiempo constante
        } catch (IllegalArgumentException e) {
            return false;   // hash mal formado
        }
    }

    /** RN-11: al menos 8 caracteres, con letras y con numeros. */
    public static boolean cumplePolitica(char[] clave) {
        if (clave == null || clave.length < 8) {
            return false;
        }
        boolean letra = false;
        boolean numero = false;
        for (char c : clave) {
            letra |= Character.isLetter(c);
            numero |= Character.isDigit(c);
        }
        return letra && numero;
    }

    private static byte[] derivar(char[] clave, byte[] sal, int iter) {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, iter, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    /**
     * Genera hashes para 02_datos_prueba.sql.
     * En NetBeans: clic derecho en Clave.java, Run File.
     * Para otras claves: Properties, Run, Arguments (separadas por espacio).
     */
    public static void main(String[] args) {
        String[] claves = args.length > 0 ? args : new String[]{"Admin2026", "Venta2026"};
        for (String texto : claves) {
            char[] c = texto.toCharArray();
            if (!cumplePolitica(c)) {
                System.out.println(texto + " -> NO cumple RN-11 (8+ caracteres, letras y numeros)");
                continue;
            }
            String hash = generar(c);
            System.out.println(texto + " -> " + hash);
            System.out.println("   verificar: " + verificar(c, hash));
            Arrays.fill(c, '\0');
        }
    }
}
