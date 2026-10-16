package com.cyan.retocacao;

import com.cyan.retocacao.model.Lote;
import com.cyan.retocacao.repository.LoteRepository;
import com.cyan.retocacao.service.LoteService;
import com.cyan.retocacao.service.RegistroOperacionService;
import com.cyan.retocacao.dto.OperacionSolicitud;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaLoteTests {
    @org.junit.jupiter.api.BeforeEach void autenticar() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "admin-prueba", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }
    @org.junit.jupiter.api.AfterEach void cerrarSesion() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    private ConfigurableApplicationContext iniciar() {
        return new SpringApplicationBuilder(RetoCacaoApplication.class)
            .run("--server.port=0", "--logging.level.root=WARN", "--spring.jpa.show-sql=false");
    }

    @Test void conservaLoteTrasCerrarYReabrirAplicacion() {
        String codigo = "RST-" + UUID.randomUUID().toString().substring(0, 12);
        var operacion = new OperacionSolicitud(UUID.randomUUID().toString(), codigo, "Etapa final", "Prueba reinicio",
            LocalDate.of(2026, 10, 15), new BigDecimal("10"), new BigDecimal("8"), null,
            new BigDecimal("160000"), new BigDecimal("25000"), "Materia prima y trabajo", null);
        try (var primera = iniciar()) {
            Lote lote = new Lote();
            lote.setCodigo(codigo);
            lote.setProductor("Prueba sintetica reinicio");
            lote.setOrigen("Rivera");
            lote.setFecha(LocalDate.of(2026, 10, 15));
            lote.setCantidad(new BigDecimal("12.50"));
            primera.getBean(LoteService.class).registrarLote(lote);
            primera.getBean(RegistroOperacionService.class).registrar(operacion);
        }
        try (var segunda = iniciar()) {
            var repository = segunda.getBean(LoteRepository.class);
            try {
                var recuperado = repository.findById(codigo).orElseThrow();
                assertEquals(0, new BigDecimal("12.50").compareTo(recuperado.getCantidad()));
                assertEquals("Prueba sintetica reinicio", recuperado.getProductor());
                assertEquals("PENDIENTE", recuperado.getEstadoLote());
                var operaciones = segunda.getBean(RegistroOperacionService.class);
                var traza = operaciones.trazabilidad(codigo);
                assertEquals(0, new BigDecimal("2.50").compareTo(traza.disponibleKg()));
                assertEquals(1, traza.operaciones().size());
                assertFalse(operaciones.registrar(operacion).creada());
                assertEquals(3, segunda.getBean(com.cyan.retocacao.service.AuditoriaService.class).porLote(codigo).size());
            } finally {
                // Solo eliminar el registro sintetico creado por esta prueba.
                segunda.getBean(org.springframework.jdbc.core.JdbcTemplate.class).update(
                    "DELETE FROM auditoria_cacao WHERE (tipo_registro='LOTE' AND registro_id=?) OR " +
                    "(tipo_registro='OPERACION' AND registro_id=?)", codigo, operacion.codigoOperacion());
                segunda.getBean(org.springframework.jdbc.core.JdbcTemplate.class)
                    .update("DELETE FROM registro_operaciones WHERE lote_codigo = ?", codigo);
                repository.deleteById(codigo);
            }
        }
    }
}
