package com.cyan.retocacao.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/publico/compradores")
public class CompradorController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    public CompradorController(JdbcTemplate jdbc, PasswordEncoder passwords) { this.jdbc = jdbc; this.passwords = passwords; }

    public record Registro(@NotBlank @Size(max=120) String nombre, @NotBlank @Email @Size(max=150) String correo,
        @NotBlank @Size(max=30) String telefono, @NotBlank @Size(max=40) String tipoComprador,
        @NotBlank @Size(max=150) String ciudad, @NotBlank @Size(min=12, max=72) String password) {
        @Override public String toString() { return "RegistroComprador[datos personales y clave omitidos]"; }
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String,String> registrar(@Valid @RequestBody Registro r) {
        if (r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La clave admite hasta 72 bytes UTF-8");
        if (!java.util.Set.of("Persona natural", "Comercio / tienda", "Empresa", "Distribuidor").contains(r.tipoComprador()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de comprador no valido");
        jdbc.update("INSERT INTO comprador(nombre,correo,telefono,tipo_comprador,ciudad,contrasena) VALUES (?,?,?,?,?,?)",
            r.nombre().trim(), r.correo().trim().toLowerCase(java.util.Locale.ROOT), r.telefono().trim(),
            r.tipoComprador(), r.ciudad().trim(), passwords.encode(r.password()));
        return java.util.Map.of("mensaje", "Registro de comprador recibido. La consulta publica no requiere sesion.");
    }
}
