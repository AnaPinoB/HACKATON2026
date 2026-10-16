package com.cyan.retocacao;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class IntegracionWebTests {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    MockMvc mvc;
    String admin, operador, comprador, clave, lote;
    record Sesion(MockHttpSession session, String header, String token) {}

    @BeforeEach void preparar() {
        String id = UUID.randomUUID().toString();
        admin = "admin-" + id + "@example.invalid";
        operador = "op-" + id + "@example.invalid";
        comprador = "buyer-" + id + "@example.invalid";
        clave = UUID.randomUUID().toString(); lote = "WEB-" + id.substring(0,12);
        String hash = encoder.encode(clave);
        jdbc.update("INSERT INTO usuario(nombre,correo,contrasena,rol,activo) VALUES (?,?,?,?,TRUE)","Test web",admin,hash,"ADMINISTRADOR");
        jdbc.update("INSERT INTO usuario(nombre,correo,contrasena,rol,activo) VALUES (?,?,?,?,TRUE)","Test web",operador,hash,"OPERADOR");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void limpiarSoloDatosDeEstaPrueba() {
        jdbc.update("DELETE FROM auditoria_cacao WHERE (tipo_registro='LOTE' AND registro_id=?) OR " +
            "(tipo_registro='OPERACION' AND registro_id IN (SELECT codigo_operacion FROM registro_operaciones WHERE lote_codigo=?))",lote,lote);
        jdbc.update("DELETE FROM registro_operaciones WHERE lote_codigo=?",lote);
        jdbc.update("DELETE FROM lote WHERE codigo=?",lote);
        jdbc.update("DELETE FROM usuario WHERE correo IN (?,?)",admin,operador);
        jdbc.update("DELETE FROM comprador WHERE correo=?",comprador);
    }
    Sesion csrf(MockHttpSession session) throws Exception {
        var request = get("/api/csrf"); if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Sesion((MockHttpSession)result.getRequest().getSession(),body.get("headerName").asString(),body.get("token").asString());
    }
    Sesion login(String correo) throws Exception {
        var s = csrf(null);
        mvc.perform(post("/api/login").session(s.session()).header(s.header(),s.token())
            .param("correo",correo).param("password",clave)).andExpect(status().isNoContent());
        return csrf(s.session());
    }
    @Test void flujoRealConSesionRolesMySQLYFichaPublica() throws Exception {
        var op = login(operador); var adm = login(admin);
        mvc.perform(get("/api/cuenta").session(op.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.correo").value(operador)).andExpect(jsonPath("$.permisos[0]").value("ROLE_OPERADOR"));
        String recepcion = json.writeValueAsString(Map.of("codigo",lote,"productor","Productor privado",
            "origen","Rivera","fecha","2026-10-16","cantidad",100,"unidad","kg","observacion","Nota privada"));
        mvc.perform(post("/api/lotes").session(op.session()).header(op.header(),op.token()).contentType("application/json").content(recepcion))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.observacion").value("Nota privada"));
        String transformacion = """
            {"codigoOperacion":"%s","loteCodigo":"%s","etapa":"Seleccion","responsable":"Responsable privado",
            "fechaOperacion":"2026-10-16","cantidadEntradaKg":100,"cantidadSalidaKg":80,"costoTotal":1600000,
            "precioVentaKg":25000,"costosIncluidos":"Materia prima y trabajo"}
            """.formatted(UUID.randomUUID(),lote);
        mvc.perform(post("/api/operaciones").session(op.session()).header(op.header(),op.token()).contentType("application/json").content(transformacion))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.mermaPorcentaje").value(20)).andExpect(jsonPath("$.alertaMerma").value(true))
            .andExpect(jsonPath("$.datos.costoTotal").doesNotExist()).andExpect(jsonPath("$.datos.precioVentaKg").doesNotExist())
            .andExpect(jsonPath("$.datos.costosIncluidos").doesNotExist()).andExpect(jsonPath("$.costoUnitarioCopKg").doesNotExist())
            .andExpect(jsonPath("$.margenSobreVentaPorcentaje").doesNotExist());
        mvc.perform(post("/api/operaciones").session(op.session()).header(op.header(),op.token()).contentType("application/json").content(transformacion))
            .andExpect(status().isOk()).andExpect(jsonPath("$.datos.costoTotal").doesNotExist())
            .andExpect(jsonPath("$.costoUnitarioCopKg").doesNotExist());
        mvc.perform(get("/api/lotes/{codigo}/trazabilidad",lote).session(op.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.disponibleKg").value(0))
            .andExpect(jsonPath("$.operaciones[0].datos.costoTotal").doesNotExist())
            .andExpect(jsonPath("$.operaciones[0].costoUnitarioCopKg").doesNotExist());
        mvc.perform(get("/api/lotes/{codigo}/trazabilidad",lote).session(adm.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.operaciones[0].costoUnitarioCopKg").value(20000));
        String liberar = "{\"estado\":\"LIBERADO\",\"motivo\":\"Control realizado\"}";
        mvc.perform(patch("/api/lotes/{codigo}/estado",lote).session(op.session()).header(op.header(),op.token()).contentType("application/json").content(liberar))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/lotes/{codigo}/estado",lote).session(adm.session()).header(adm.header(),adm.token()).contentType("application/json").content(liberar))
            .andExpect(status().isOk());
        var publico = mvc.perform(get("/api/publico/productos/{codigo}",lote)).andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibleParaVenta").value(true)).andExpect(jsonPath("$.trazabilidad.length()").value(1)).andReturn();
        String publicoJson = publico.getResponse().getContentAsString();
        for (String privado : List.of("costo", "precio", "productor", "responsable", "Nota privada", "motivo"))
            assertFalse(publicoJson.toLowerCase().contains(privado.toLowerCase()),privado);
        mvc.perform(patch("/api/lotes/{codigo}/estado",lote).session(adm.session()).header(adm.header(),adm.token()).contentType("application/json")
            .content("{\"estado\":\"BLOQUEADO\",\"motivo\":\"Incidencia\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/publico/productos/{codigo}",lote)).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("BLOQUEADO")).andExpect(jsonPath("$.disponibleParaVenta").value(false));
        mvc.perform(get("/api/lotes/{codigo}/auditoria",lote).session(adm.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(post("/api/logout").session(op.session()).header(op.header(),op.token())).andExpect(status().isNoContent());
        assertTrue(op.session().isInvalid());
        mvc.perform(get("/api/cuenta")).andExpect(status().isUnauthorized());
    }
    @Test void credencialesIncorrectasYCsrfNoConcedenSesion() throws Exception {
        mvc.perform(post("/api/login").param("correo",admin).param("password",clave)).andExpect(status().isForbidden());
        var s = csrf(null);
        mvc.perform(post("/api/login").session(s.session()).header(s.header(),s.token()).param("correo",admin).param("password","incorrecta"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cuenta").session(s.session())).andExpect(status().isUnauthorized());
    }
    @Test void registroCompradorNoOtorgaRolesYGuardaSoloHash() throws Exception {
        var s = csrf(null);
        var body = json.writeValueAsString(Map.of("nombre","Comprador prueba","correo",comprador,"telefono","3000000000",
            "tipoComprador","Persona natural","ciudad","Neiva","password",clave));
        var result = mvc.perform(post("/api/publico/compradores").session(s.session()).header(s.header(),s.token()).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(clave));
        String hash = jdbc.queryForObject("SELECT contrasena FROM comprador WHERE correo=?",String.class,comprador);
        assertTrue(encoder.matches(clave,hash)); assertNotEquals(clave,hash);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo=?",Integer.class,comprador));
        mvc.perform(get("/api/lotes").session(s.session())).andExpect(status().isUnauthorized());
    }
    @Test void portadaYRecursosSiguenPublicos() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/api/publico/productos/NO-EXISTE-TEST")).andExpect(status().isNotFound());
    }
}
