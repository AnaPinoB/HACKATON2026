import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Migracion aditiva: conserva tablas, usuarios y registros existentes. */
class PrepararIntegracion {
    public static void main(String[] args) throws Exception {
        var config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-local.properties"))) { config.load(reader); }
        try (var c = DriverManager.getConnection(config.getProperty("spring.datasource.url"),
                config.getProperty("spring.datasource.username"), config.getProperty("spring.datasource.password"));
             var statement = c.createStatement()) {
            try (var cols = c.getMetaData().getColumns(c.getCatalog(), null, "lote", "observacion")) {
                if (!cols.next()) statement.execute("ALTER TABLE lote ADD COLUMN observacion VARCHAR(2000) NULL");
            }
            String sql = Files.readString(Path.of("DB/tabla_comprador.sql"));
            statement.execute(sql.substring(sql.indexOf("CREATE TABLE")));
            try (var rs = statement.executeQuery("SELECT COUNT(*) FROM usuario")) {
                rs.next(); System.out.println("Usuarios internos existentes: " + rs.getLong(1));
            }
            System.out.println("Notas de lote y registro de compradores preparados. No se crearon usuarios internos.");
        }
    }
}
