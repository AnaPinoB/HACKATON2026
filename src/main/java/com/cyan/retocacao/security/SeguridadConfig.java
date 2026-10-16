
package com.cyan.retocacao.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(JdbcTemplate jdbc) {
        return correo -> jdbc.query(
            "SELECT correo, contrasena, rol, activo FROM usuario WHERE correo = ?",
            (rs, row) -> User.withUsername(rs.getString("correo"))
                .password(rs.getString("contrasena"))
                .roles(rs.getString("rol"))
                .disabled(!rs.getBoolean("activo"))
                .build(),
            correo
        ).stream().findFirst()
        .orElseThrow(() ->
            new UsernameNotFoundException("Credenciales invalidas")
        );
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        org.springframework.security.web.AuthenticationEntryPoint sinSesion = (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":401,\"detail\":\"Autenticacion requerida\"}");
        };
        return http
            .authorizeHttpRequests(auth -> auth

                // Endpoint para obtener el token CSRF
                .requestMatchers(
                    "/api/csrf",
                    "/error"
                ).permitAll()

                .requestMatchers(HttpMethod.POST, "/api/publico/compradores").permitAll()

                // RF06: Consulta publica de productos
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/publico/productos/*"
                ).permitAll()

                // Permitir acceso publico al frontend
                .requestMatchers(
                    HttpMethod.GET,
                    "/",
                    "/index.html",
                    "/css/**",
                    "/js/**",
                    "/assets/**",
                    "/favicon.ico"
                ).permitAll()

                // El resto de la API requiere autenticacion
                .requestMatchers("/api/**")
                    .hasAnyRole("OPERADOR", "ADMINISTRADOR")

                // Denegar cualquier otra solicitud
                .anyRequest().denyAll()
            )

            // Autenticacion HTTP Basic
            .httpBasic(basic -> basic.authenticationEntryPoint(sinSesion))

            // El navegador usa una sesion HttpOnly; nunca necesita conservar la clave.
            .formLogin(login -> login.loginProcessingUrl("/api/login")
                .usernameParameter("correo").passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(204);
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("{\"status\":401,\"detail\":\"Correo o contrasena incorrectos, o cuenta inactiva\"}");
                }).permitAll())
            .logout(logout -> logout.logoutUrl("/api/logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))

            // Mantener proteccion CSRF
            .csrf(Customizer.withDefaults())

            // Manejo de errores de seguridad
            .exceptionHandling(errors -> errors

                // Error 401: Usuario no autenticado
                .authenticationEntryPoint(sinSesion)

                // Error 403: Acceso denegado
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/problem+json");
                    response.getWriter().write(
                        "{\"status\":403,\"detail\":\"Permiso denegado o token CSRF invalido\"}"
                    );
                })
            )
            .build();
    }
}
