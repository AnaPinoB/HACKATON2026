package com.cyan.retocacao;

import com.cyan.retocacao.dto.OperacionSolicitud;
import com.cyan.retocacao.model.Lote;
import com.cyan.retocacao.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Transacciones reales confirmadas: el bloqueo se prueba entre conexiones distintas.
@SpringBootTest(properties = {"logging.level.root=WARN", "spring.jpa.show-sql=false"})
class RegistroOperacionTests {
    @Autowired RegistroOperacionService service;
    @Autowired LoteService lotes;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext context;
    String lote;
    MockMvc mvc;

    @BeforeEach void preparar() {
        lote = "OP-" + UUID.randomUUID().toString().substring(0, 12);
        Lote nuevo = new Lote(); nuevo.setCodigo(lote); nuevo.setProductor("Prueba sintetica operaciones");
        nuevo.setOrigen("Rivera"); nuevo.setFecha(LocalDate.of(2026, 10, 15)); nuevo.setCantidad(new BigDecimal("100"));
        lotes.registrarLote(nuevo);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
    @AfterEach void limpiar() {
        jdbc.update("DELETE FROM registro_operaciones WHERE lote_codigo = ?", lote);
        jdbc.update("DELETE FROM lote WHERE codigo = ?", lote);
    }
    OperacionSolicitud datos(String codigo, String entrada, String salida, String justificacion) {
        return new OperacionSolicitud(codigo, lote, "Transformacion final", "Operador sintetico", LocalDate.of(2026, 10, 15),
            new BigDecimal(entrada), new BigDecimal(salida), null, new BigDecimal("1600000"),
            new BigDecimal("25000"), "Materia prima, mano de obra y energia", justificacion);
    }
    String codigo() { return UUID.randomUUID().toString(); }
    String json(String codigo) {
        return """
            {"codigoOperacion":"%s","loteCodigo":"%s","etapa":"Transformacion",
             "responsable":"Operador sintetico","fechaOperacion":"2026-10-15",
             "cantidadEntradaKg":100,"cantidadSalidaKg":80,"costoTotal":1600000,
             "precioVentaKg":25000,"costosIncluidos":"Materia prima y trabajo"}
            """.formatted(codigo, lote);
    }

    @Test void ejemploBaseYReenvioHttp() throws Exception {
        String body = json(codigo());
        mvc.perform(post("/api/operaciones").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.mermaPorcentaje").value(20))
            .andExpect(jsonPath("$.alertaMerma").value(true)).andExpect(jsonPath("$.costoUnitarioCopKg").value(20000))
            .andExpect(jsonPath("$.margenSobreVentaPorcentaje").value(20)).andExpect(jsonPath("$.fechaCreacion").isNotEmpty());
        mvc.perform(post("/api/operaciones").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        mvc.perform(get("/api/lotes/{codigo}/trazabilidad", lote)).andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibleKg").value(0)).andExpect(jsonPath("$.consumidoKg").value(100))
            .andExpect(jsonPath("$.salidaProducidaKg").value(80)).andExpect(jsonPath("$.operaciones.length()").value(1));
        mvc.perform(get("/api/lotes/{codigo}/operaciones", lote)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void sobreconsumoNoGuarda() {
        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(datos(codigo(), "120", "80", null)));
        assertEquals(409, ex.getStatusCode().value());
        assertEquals(0, service.trazabilidad(lote).operaciones().size());
        assertEquals(0, new BigDecimal("100").compareTo(service.trazabilidad(lote).disponibleKg()));
    }
    @Test void rechazaBloqueadoUnidadIncompatibleYFechaAnterior() {
        jdbc.update("UPDATE lote SET estado_lote='BLOQUEADO' WHERE codigo=?", lote);
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos(codigo(), "100", "80", null))).getStatusCode().value());
        jdbc.update("UPDATE lote SET estado_lote='PENDIENTE', unidad='g' WHERE codigo=?", lote);
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos(codigo(), "100", "80", null))).getStatusCode().value());
        jdbc.update("UPDATE lote SET unidad='kg', fecha='2026-10-16' WHERE codigo=?", lote);
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos(codigo(), "100", "80", null))).getStatusCode().value());
        assertEquals(0, service.trazabilidad(lote).operaciones().size());
    }
    @Test void mismoCodigoDistintosDatosEsConflicto() {
        String id = codigo(); service.registrar(datos(id, "50", "40", null));
        var ex = assertThrows(ResponseStatusException.class, () -> service.registrar(datos(id, "60", "40", null)));
        assertEquals(409, ex.getStatusCode().value());
        assertEquals(1, service.trazabilidad(lote).operaciones().size());
    }
    @Test void reenvioNormalizaEscalaDecimal() {
        String id = codigo(); var inicial = service.registrar(datos(id, "100", "80", null));
        var repetida = service.registrar(datos(id, "100.00", "80.00", null));
        assertFalse(repetida.creada()); assertEquals(inicial.operacion().id(), repetida.operacion().id());
    }
    @Test void salidaCeroNoDivide() {
        var r = service.registrar(datos(codigo(), "100", "0", null)).operacion();
        assertNull(r.costoUnitarioCopKg()); assertNull(r.margenSobreVentaPorcentaje());
        assertTrue(r.alertaMerma());
    }
    @Test void salidaMayorRequiereJustificacion() {
        assertThrows(ResponseStatusException.class, () -> service.registrar(datos(codigo(), "100", "110", null)));
        var r = service.registrar(datos(codigo(), "100", "110", "Adicion de 10 kg de insumos")).operacion();
        assertNull(r.mermaPorcentaje()); assertFalse(r.alertaMerma());
        assertEquals(0, service.trazabilidad(lote).disponibleKg().signum());
    }
    @Test void alertaUsaPorcentajeSinRedondear() {
        // 3 kg -> 2.5499 no cabe en DECIMAL(10,2); ampliar lote para probar el borde exacto.
        jdbc.update("UPDATE lote SET cantidad=1000 WHERE codigo=?", lote);
        var r = service.registrar(datos(codigo(), "1000", "849.99", null)).operacion();
        assertEquals(0, new BigDecimal("15.00").compareTo(r.mermaPorcentaje()));
        assertTrue(r.alertaMerma());
    }
    @Test void datosInvalidosHttpNoGuardan() throws Exception {
        for (String body : new String[]{json(codigo()).replace("\"cantidadEntradaKg\":100", "\"cantidadEntradaKg\":-1"),
                json(codigo()).replace("\"responsable\":\"Operador sintetico\"", "\"responsable\":\"\""),
                json(codigo()).replace("\"costoTotal\":1600000", "\"costoTotal\":-1")}) {
            mvc.perform(post("/api/operaciones").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").isNotEmpty());
        }
        assertEquals(0, service.trazabilidad(lote).operaciones().size());
    }
    @Test void falloPosteriorRevierteInsercionEInventario() {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            service.registrar(datos(codigo(), "100", "80", null));
            throw new IllegalStateException("Fallo simulado antes del commit");
        }));
        assertEquals(0, service.trazabilidad(lote).operaciones().size());
        assertEquals(0, new BigDecimal("100").compareTo(service.trazabilidad(lote).disponibleKg()));
    }
    @Test void operacionesSimultaneasNoSobreconsumen() throws Exception {
        var resultados = concurrentes(datos(codigo(), "60", "40", null), datos(codigo(), "60", "40", null));
        assertEquals(1, resultados.stream().filter("creada"::equals).count());
        assertEquals(1, resultados.stream().filter("conflicto"::equals).count());
        assertEquals(0, new BigDecimal("40").compareTo(service.trazabilidad(lote).disponibleKg()));
    }
    @Test void reenviosSimultaneosConsumenUnaVez() throws Exception {
        var s = datos(codigo(), "100", "80", null); var resultados = concurrentes(s, s);
        assertEquals(1, resultados.stream().filter("creada"::equals).count());
        assertEquals(1, resultados.stream().filter("reenvio"::equals).count());
        assertEquals(1, service.trazabilidad(lote).operaciones().size());
    }
    private java.util.List<String> concurrentes(OperacionSolicitud a, OperacionSolicitud b) throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tareas = new java.util.ArrayList<Future<String>>();
            for (var s : java.util.List.of(a, b)) tareas.add(pool.submit(() -> {
                inicio.await();
                try { return service.registrar(s).creada() ? "creada" : "reenvio"; }
                catch (ResponseStatusException ex) { if (ex.getStatusCode().value() == 409) return "conflicto"; throw ex; }
            }));
            inicio.countDown();
            return java.util.List.of(tareas.get(0).get(20, TimeUnit.SECONDS), tareas.get(1).get(20, TimeUnit.SECONDS));
        }
    }
}
