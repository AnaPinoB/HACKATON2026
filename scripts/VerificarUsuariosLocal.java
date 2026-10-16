import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Consulta de solo lectura: no imprime correos, contrasenas ni hashes. */
class VerificarUsuariosLocal {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-local.properties"))) {
            config.load(reader);
        }
        try (var connection = DriverManager.getConnection(config.getProperty("spring.datasource.url"),
                config.getProperty("spring.datasource.username"), config.getProperty("spring.datasource.password"))) {
            connection.setReadOnly(true);
            for (String rol : new String[]{"ADMINISTRADOR", "OPERADOR"}) {
                int total = 0, validos = 0;
                try (var query = connection.prepareStatement("SELECT activo, contrasena FROM usuario WHERE rol = ?")) {
                    query.setString(1, rol);
                    try (var result = query.executeQuery()) {
                        while (result.next()) {
                            total++;
                            String hash = result.getString("contrasena");
                            if (result.getBoolean("activo") && hash != null
                                    && hash.matches("[$]2[aby][$]12[$][./A-Za-z0-9]{53}")) validos++;
                        }
                    }
                }
                System.out.printf("%s: cuentas=%d, activas con formato BCrypt costo 12=%d%n", rol, total, validos);
                if (validos == 0) throw new IllegalStateException("Falta una cuenta valida para " + rol);
            }
        }
    }
}
