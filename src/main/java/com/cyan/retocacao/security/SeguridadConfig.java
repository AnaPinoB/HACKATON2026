package com.cyan.retocacao.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SeguridadConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService usuarios(JdbcTemplate jdbc) {
        return correo -> jdbc.query("SELECT correo, contrasena, rol, activo FROM usuario WHERE correo = ?",
            (rs, row) -> User.withUsername(rs.getString("correo"))
                .password(rs.getString("contrasena")).roles(rs.getString("rol"))
                .disabled(!rs.getBoolean("activo")).build(), correo).stream().findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
    }

    @Bean SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/csrf", "/error").permitAll()
                .requestMatchers("/api/**").hasAnyRole("OPERADOR", "ADMINISTRADOR")
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults())
            // CSRF permanece activo, incluso usando Basic en un navegador.
            .csrf(Customizer.withDefaults())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setHeader("WWW-Authenticate", "Basic realm=\"RetoCacao\"");
                    response.setContentType("application/problem+json");
                    response.getWriter().write("{\"status\":401,\"detail\":\"Autenticacion requerida\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("{\"status\":403,\"detail\":\"Permiso denegado o token CSRF invalido\"}");
                }))
            .build();
    }
}
