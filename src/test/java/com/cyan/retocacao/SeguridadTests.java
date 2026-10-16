package com.cyan.retocacao;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SeguridadTests {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    String correo;
    String clave;

    @BeforeEach void preparar() {
        correo = "test-" + UUID.randomUUID() + "@example.invalid";
        clave = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO usuario(nombre,correo,contrasena,rol,activo) VALUES (?,?,?,?,TRUE)",
            "Prueba seguridad", correo, encoder.encode(clave), "OPERADOR");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void limpiar() { jdbc.update("DELETE FROM usuario WHERE correo=?", correo); }

    @Test void anonimoNoConsultaDatosInternos() throws Exception {
        mvc.perform(get("/api/lotes")).andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/operaciones")).andExpect(status().isUnauthorized());
    }
    @Test void usuarioRealSeAutenticaConBcrypt() throws Exception {
        mvc.perform(get("/api/cuenta").with(httpBasic(correo, clave)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value(correo))
            .andExpect(jsonPath("$.permisos[0]").value("ROLE_OPERADOR"));
        mvc.perform(get("/api/cuenta").with(httpBasic(correo, "incorrecta")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));
    }
    @Test void usuarioInactivoNoIngresa() throws Exception {
        jdbc.update("UPDATE usuario SET activo=FALSE WHERE correo=?", correo);
        mvc.perform(get("/api/cuenta").with(httpBasic(correo, clave))).andExpect(status().isUnauthorized());
    }
    @Test void operadorNoConsultaCostosAdministrativos() throws Exception {
        mvc.perform(get("/api/operaciones").with(httpBasic(correo, clave))).andExpect(status().isOk());
        mvc.perform(get("/api/lotes/TEST/auditoria").with(httpBasic(correo, clave))).andExpect(status().isForbidden());
        mvc.perform(get("/api/operaciones").with(user("admin").roles("ADMINISTRADOR"))).andExpect(status().isOk());
    }
    @Test void escrituraSinCsrfEsDenegada() throws Exception {
        mvc.perform(post("/api/lotes").with(httpBasic(correo, clave)).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/lotes").with(httpBasic(correo, clave)).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
}
