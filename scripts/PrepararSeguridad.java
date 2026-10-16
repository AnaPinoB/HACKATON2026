import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

class PrepararSeguridad {
    public static void main(String[] args) throws Exception {
        var config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-local.properties"))) {
            config.load(reader);
        }
        try (var connection = DriverManager.getConnection(config.getProperty("spring.datasource.url"),
                config.getProperty("spring.datasource.username"), config.getProperty("spring.datasource.password"));
             var statement = connection.createStatement()) {
            for (String archivo : new String[]{"DB/tabla_usuario.sql", "DB/tabla_de_auditoriacacao.sql"}) {
                String sql = Files.readString(Path.of(archivo));
                statement.execute(sql.substring(sql.indexOf("CREATE TABLE")));
            }
            try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "lote", "motivo_estado")) {
                if (!columns.next()) statement.execute("ALTER TABLE lote ADD COLUMN motivo_estado VARCHAR(255) NULL");
            }
            try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "auditoria_cacao", "usuario_encargado")) {
                if (columns.next() && columns.getInt("COLUMN_SIZE") < 150)
                    statement.execute("ALTER TABLE auditoria_cacao MODIFY usuario_encargado VARCHAR(150) NOT NULL");
            }
            System.out.println("Esquema de usuarios, estados y auditoria preparado; no se crearon cuentas.");
        }
    }
}
