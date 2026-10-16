import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Ejecutar desde la raiz con el driver MySQL en el classpath. No imprime credenciales. */
class PrepararOperaciones {
    public static void main(String[] args) throws Exception {
        var config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-local.properties"))) {
            config.load(reader);
        }
        try (var connection = DriverManager.getConnection(config.getProperty("spring.datasource.url"),
                config.getProperty("spring.datasource.username"), config.getProperty("spring.datasource.password"));
             var statement = connection.createStatement()) {
            String sql = Files.readString(Path.of("DB/tabla_registro_operaciones.sql"));
            statement.execute(sql.substring(sql.indexOf("CREATE TABLE")));
            for (String columna : new String[]{"costos_incluidos", "justificacion_balance"}) {
                try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "registro_operaciones", columna)) {
                    if (!columns.next()) {
                        statement.execute("ALTER TABLE registro_operaciones ADD COLUMN " + columna + " VARCHAR(500) NULL");
                        System.out.println("Columna agregada: " + columna);
                    }
                }
            }
            System.out.println("Esquema de operaciones preparado; registros existentes conservados.");
        }
    }
}
