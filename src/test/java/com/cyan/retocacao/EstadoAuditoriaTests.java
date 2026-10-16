package com.cyan.retocacao;

import com.cyan.retocacao.dto.*;
import com.cyan.retocacao.model.Lote;
import com.cyan.retocacao.repository.LoteRepository;
import com.cyan.retocacao.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithMockUser(username = "admin-test", roles = "ADMINISTRADOR")
class EstadoAuditoriaTests {
    @Autowired LoteService lotes;
    @Autowired LoteRepository repository;
    @Autowired EstadoLoteService estados;
    @Autowired RegistroOperacionService operaciones;
    @MockitoSpyBean AuditoriaService auditoria;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    String codigo;

    @BeforeEach void preparar() {
        codigo = "EST-" + UUID.randomUUID().toString().substring(0, 12);
        var l = new Lote(); l.setCodigo(codigo); l.setProductor("Prueba estados"); l.setOrigen("Rivera");
        l.setFecha(LocalDate.of(2026, 10, 15)); l.setCantidad(new BigDecimal("100"));
        lotes.registrarLote(l);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void limpiar() {
        reset(auditoria);
        jdbc.update("DELETE FROM auditoria_cacao WHERE (tipo_registro='LOTE' AND registro_id=?) OR " +
            "(tipo_registro='OPERACION' AND registro_id IN (SELECT codigo_operacion FROM registro_operaciones WHERE lote_codigo=?))", codigo, codigo);
        jdbc.update("DELETE FROM registro_operaciones WHERE lote_codigo=?", codigo);
        jdbc.update("DELETE FROM lote WHERE codigo=?", codigo);
    }
    CambioEstadoSolicitud cambio(CambioEstadoSolicitud.Estado estado) { return new CambioEstadoSolicitud(estado, "Incidencia de prueba"); }
    OperacionSolicitud operacion() {
        return new OperacionSolicitud(UUID.randomUUID().toString(), codigo, "Etapa final", "Responsable declarado",
            LocalDate.of(2026, 10, 15), new BigDecimal("100"), new BigDecimal("80"), null,
            new BigDecimal("1600000"), new BigDecimal("25000"), "Materia prima y trabajo", null);
    }

    @Test void administradorBloqueaYAuditaIdentidadAnteriorYNuevo() throws Exception {
        mvc.perform(patch("/api/lotes/{codigo}/estado", codigo).with(user("admin-real").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content("{\"estado\":\"BLOQUEADO\",\"motivo\":\"Incidencia CAC001 simulada\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estadoLote").value("BLOQUEADO"));
        var eventos = auditoria.porLote(codigo);
        assertEquals(2, eventos.size());
        var e = eventos.get(1);
        assertEquals("admin-real", e.usuario()); assertEquals("CAMBIO_ESTADO", e.accion());
        assertTrue(e.valorAnterior().contains("PENDIENTE")); assertTrue(e.valorNuevo().contains("BLOQUEADO"));
        assertEquals("Incidencia CAC001 simulada", e.motivo()); assertNotNull(e.fecha());
    }
    @Test @WithMockUser(username = "operador", roles = "OPERADOR")
    void operadorNoCambiaEstadoNiAccedeAuditoriaNiPorServicio() throws Exception {
        mvc.perform(patch("/api/lotes/{codigo}/estado", codigo).with(csrf()).contentType("application/json")
            .content("{\"estado\":\"LIBERADO\",\"motivo\":\"Intento\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/lotes/{codigo}/auditoria", codigo)).andExpect(status().isForbidden());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.LIBERADO)));
        assertEquals("PENDIENTE", repository.findById(codigo).orElseThrow().getEstadoLote());
    }
    @Test void motivoYEstadoSonObligatorios() throws Exception {
        for (String body : new String[]{"{\"estado\":\"LIBERADO\",\"motivo\":\" \"}",
            "{\"estado\":\"OTRO\",\"motivo\":\"Prueba\"}", "{}"}) {
            mvc.perform(patch("/api/lotes/{codigo}/estado", codigo).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        }
        assertEquals(1, auditoria.porLote(codigo).size());
    }
    @Test void bloqueoConservaOrigenCantidadesEHistorial() {
        var s = operacion(); operaciones.registrar(s);
        estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.BLOQUEADO));
        var traza = operaciones.trazabilidad(codigo);
        assertEquals("Rivera", traza.lote().getOrigen()); assertEquals("BLOQUEADO", traza.lote().getEstadoLote());
        assertEquals(0, traza.disponibleKg().signum()); assertEquals(1, traza.operaciones().size());
        assertEquals(4, auditoria.porLote(codigo).size());
        assertFalse(operaciones.registrar(s).creada());
        assertEquals(4, auditoria.porLote(codigo).size());
    }
    @Test void bloqueadoNoAdmiteNuevoConsumo() {
        estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.BLOQUEADO));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> operaciones.registrar(operacion()));
        assertEquals(0, operaciones.trazabilidad(codigo).operaciones().size());
        assertEquals(2, auditoria.porLote(codigo).size());
    }
    @Test void falloDeAuditoriaRevierteEstado() {
        doThrow(new IllegalStateException("Fallo de auditoria simulado")).when(auditoria)
            .registrar(eq("CAMBIO_ESTADO"), anyString(), anyString(), anyString(), any(), any(), anyString());
        assertThrows(IllegalStateException.class, () -> estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.BLOQUEADO)));
        assertEquals("PENDIENTE", repository.findById(codigo).orElseThrow().getEstadoLote());
        assertEquals(1, auditoria.porLote(codigo).size());
    }
    @Test void liberarYRepetirNoBorraHistorialNiDuplica() {
        estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.BLOQUEADO));
        estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.LIBERADO));
        estados.cambiar(codigo, cambio(CambioEstadoSolicitud.Estado.LIBERADO));
        assertEquals(3, auditoria.porLote(codigo).size());
        assertEquals("LIBERADO", repository.findById(codigo).orElseThrow().getEstadoLote());
    }
    @Test void consumoSeAuditaConUsuarioAutenticado() {
        var s = operacion(); operaciones.registrar(s); operaciones.registrar(s);
        var eventos = auditoria.porLote(codigo);
        assertEquals(3, eventos.size());
        assertTrue(eventos.stream().allMatch(e -> e.usuario().equals("admin-test")));
        var consumo = eventos.stream().filter(e -> e.accion().equals("CONSUMO")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal(consumo.valorAnterior()).compareTo(new BigDecimal("100")));
        assertEquals(0, new BigDecimal(consumo.valorNuevo()).signum());
    }
}
