package com.cyan.retocacao.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Aprovisionamiento local explicito, sin endpoint publico ni contrasenas predeterminadas. */
@Component
@Profile("crear-usuario")
public class CrearUsuarioLocal implements ApplicationRunner {
    private final Environment env;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public CrearUsuarioLocal(Environment env, JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.env = env; this.jdbc = jdbc; this.encoder = encoder;
    }
    @Override public void run(ApplicationArguments args) {
        String nombre = env.getRequiredProperty("CACAO_NOMBRE");
        String correo = env.getRequiredProperty("CACAO_CORREO");
        String rol = env.getRequiredProperty("CACAO_ROL");
        String clave = env.getRequiredProperty("CACAO_PASSWORD");
        if (nombre.isBlank() || nombre.length() > 120 || correo.isBlank() || correo.length() > 150 ||
            !java.util.Set.of("OPERADOR", "ADMINISTRADOR").contains(rol) || clave.length() < 12 ||
            clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Revise nombre, correo, rol y clave (minimo 12 caracteres, maximo 72 bytes)");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo = ?", Integer.class, correo) != 0) {
            throw new IllegalArgumentException("El usuario ya existe; no se modificaron sus credenciales");
        }
        jdbc.update("INSERT INTO usuario(nombre, correo, contrasena, rol, activo) VALUES (?, ?, ?, ?, TRUE)",
            nombre, correo, encoder.encode(clave), rol);
        System.out.println("Usuario creado con hash BCrypt. Reinicie sin el perfil crear-usuario.");
    }
}
