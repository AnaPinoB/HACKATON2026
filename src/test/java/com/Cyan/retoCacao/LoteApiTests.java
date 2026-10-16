package com.Cyan.retoCacao;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.Cyan.retoCacao.repository.LoteRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties = {"logging.level.root=WARN", "spring.jpa.show-sql=false"})
@Transactional
class LoteApiTests {
    @Autowired WebApplicationContext context;
    @Autowired LoteRepository repository;
    MockMvc mvc;
    String codigo;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        codigo = "TST-" + UUID.randomUUID().toString().substring(0, 12);
    }

    String datos(String cantidad, String productor) {
        return """
            {"codigo":"%s","productor":"%s","origen":"Rivera",
             "fecha":"2026-10-15","cantidad":%s,"unidad":"kg","estadoLote":"LIBERADO"}
            """.formatted(codigo, productor, cantidad);
    }

    @Test void registraYListaDesdeMySQL() throws Exception {
        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(datos("100", "Productor prueba")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value(codigo))
            .andExpect(jsonPath("$.estadoLote").value("PENDIENTE"))
            .andExpect(jsonPath("$.fechaCreacionRegistroLote").isNotEmpty());
        mvc.perform(get("/api/lotes")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].codigo", hasItem(codigo)));
    }

    @Test void rechazaDuplicadoSinModificarOriginal() throws Exception {
        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(datos("100", "Original")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(datos("200", "Cambio")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").isNotEmpty());
        assertEquals("Original", repository.findById(codigo).orElseThrow().getProductor());
    }

    @Test void rechazaCantidadesInvalidasSinGuardar() throws Exception {
        for (String cantidad : new String[]{"-1", "0", "0.001", "100000000", "null"}) {
            mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(datos(cantidad, "Prueba")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
            assertFalse(repository.existsById(codigo));
        }
    }

    @Test void rechazaProductorVacio() throws Exception {
        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(datos("100", " ")))
            .andExpect(status().isBadRequest());
        assertFalse(repository.existsById(codigo));
    }

    @Test void rechazaFechaInvalidaYJsonMalformado() throws Exception {
        for (String body : new String[]{"{", datos("100", "Prueba").replace("2026-10-15", "no-es-fecha")}) {
            mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
        }
        assertFalse(repository.existsById(codigo));
    }
}
