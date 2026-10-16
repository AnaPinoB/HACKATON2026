import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Genera solo un hash, no conecta a MySQL ni crea usuarios. */
class GenerarHashBCrypt {
    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Ejecutar en una terminal interactiva, sin redireccion ni tuberias");
        char[] clave = console.readPassword("Contraseña (no se mostrara): ");
        char[] confirmacion = console.readPassword("Repetir contraseña: ");
        try {
            if (clave == null || confirmacion == null || !Arrays.equals(clave, confirmacion))
                throw new IllegalArgumentException("Las contraseñas no coinciden");
            String valor = new String(clave);
            if (valor.length() < 12 || valor.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("Use al menos 12 caracteres y como maximo 72 bytes UTF-8");
            console.printf("Hash para Workbench (no es la contraseña):%n%s%n", BCrypt.hashpw(valor, BCrypt.gensalt(12)));
        } finally {
            if (clave != null) Arrays.fill(clave, '\0');
            if (confirmacion != null) Arrays.fill(confirmacion, '\0');
        }
    }
}
