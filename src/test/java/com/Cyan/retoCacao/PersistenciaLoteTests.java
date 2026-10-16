package com.Cyan.retoCacao;

import com.Cyan.retoCacao.model.Lote;
import com.Cyan.retoCacao.repository.LoteRepository;
import com.Cyan.retoCacao.service.LoteService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaLoteTests {
    private ConfigurableApplicationContext iniciar() {
        return new SpringApplicationBuilder(RetoCacaoApplication.class)
            .run("--server.port=0", "--logging.level.root=WARN", "--spring.jpa.show-sql=false");
    }

    @Test void conservaLoteTrasCerrarYReabrirAplicacion() {
        String codigo = "RST-" + UUID.randomUUID().toString().substring(0, 12);
        try (var primera = iniciar()) {
            Lote lote = new Lote();
            lote.setCodigo(codigo);
            lote.setProductor("Prueba sintetica reinicio");
            lote.setOrigen("Rivera");
            lote.setFecha(LocalDate.of(2026, 10, 15));
            lote.setCantidad(new BigDecimal("12.50"));
            primera.getBean(LoteService.class).registrarLote(lote);
        }
        try (var segunda = iniciar()) {
            var repository = segunda.getBean(LoteRepository.class);
            try {
                var recuperado = repository.findById(codigo).orElseThrow();
                assertEquals(0, new BigDecimal("12.50").compareTo(recuperado.getCantidad()));
                assertEquals("Prueba sintetica reinicio", recuperado.getProductor());
                assertEquals("PENDIENTE", recuperado.getEstadoLote());
            } finally {
                // Solo eliminar el registro sintetico creado por esta prueba.
                repository.deleteById(codigo);
            }
        }
    }
}
