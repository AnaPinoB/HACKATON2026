import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Alta interactiva, sin DDL, sin credenciales en argumentos ni archivos. */
class CrearUsuariosLocal {
    static String texto(Console console, String prompt, int max) {
        String value = console.readLine(prompt);
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new IllegalArgumentException("Dato vacio o demasiado largo. No se guardaron cambios.");
        return value.trim();
    }

    static void crear(Console console, Connection connection, String rol) throws SQLException {
        console.printf("%nCuenta %s%n", rol);
        String nombre = texto(console, "Nombre: ", 120);
        String correo = texto(console, "Correo: ", 150).toLowerCase(Locale.ROOT);
        if (!correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Correo invalido.");
        try (var query = connection.prepareStatement("SELECT id_usuario FROM usuario WHERE correo = ?")) {
            query.setString(1, correo);
            try (var result = query.executeQuery()) {
                if (result.next()) throw new IllegalArgumentException("El correo ya existe. No se sobrescriben cuentas.");
            }
        }
        char[] password = console.readPassword("Contrasena (oculta, minimo 12 caracteres): ");
        char[] confirmation = console.readPassword("Repetir contrasena (oculta): ");
        try {
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation))
                throw new IllegalArgumentException("Las contrasenas no coinciden.");
            String plain = new String(password);
            if (plain.length() < 12 || plain.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("La contrasena requiere minimo 12 caracteres y maximo 72 bytes UTF-8.");
            String hash = BCrypt.hashpw(plain, BCrypt.gensalt(12));
            try (var insert = connection.prepareStatement("INSERT INTO usuario (nombre, correo, contrasena, rol, activo) VALUES (?, ?, ?, ?, TRUE)")) {
                insert.setString(1, nombre); insert.setString(2, correo);
                insert.setString(3, hash); insert.setString(4, rol);
                if (insert.executeUpdate() != 1) throw new SQLException("Alta incompleta");
            }
            try (var query = connection.prepareStatement("SELECT contrasena, rol, activo FROM usuario WHERE correo = ?")) {
                query.setString(1, correo);
                try (var result = query.executeQuery()) {
                    if (!result.next() || !rol.equals(result.getString("rol")) || !result.getBoolean("activo")
                            || !BCrypt.checkpw(plain, result.getString("contrasena")))
                        throw new SQLException("Verificacion fallida");
                }
            }
            console.printf("Rol, estado y verificacion BCrypt correctos; pendiente de confirmar transaccion.%n");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Use una terminal interactiva, sin tuberias ni redireccion.");
        Properties config = new Properties();
        try {
            try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-local.properties"))) {
                config.load(reader);
            }
            try (Connection connection = DriverManager.getConnection(config.getProperty("spring.datasource.url"),
                    config.getProperty("spring.datasource.username"), config.getProperty("spring.datasource.password"))) {
                connection.setAutoCommit(false);
                try {
                    crear(console, connection, "ADMINISTRADOR");
                    crear(console, connection, "OPERADOR");
                    connection.commit();
                    console.printf("%nEXITO: ambas cuentas registradas y verificadas. Ya puedes iniciar sesion con sus correos.%n");
                } catch (Exception failure) {
                    connection.rollback();
                    throw failure;
                }
            }
        } catch (IllegalArgumentException failure) {
            console.printf("Cancelado: %s No se guardaron las altas de esta ejecucion.%n", failure.getMessage());
            System.exit(1);
        } catch (Exception failure) {
            console.printf("No se completo el alta. Revisa la conexion y la tabla usuario. No se muestran detalles sensibles.%n");
            System.exit(1);
        }
    }
}
