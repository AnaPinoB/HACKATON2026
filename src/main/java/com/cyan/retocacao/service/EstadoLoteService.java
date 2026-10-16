package com.cyan.retocacao.service;

import com.cyan.retocacao.dto.CambioEstadoSolicitud;
import com.cyan.retocacao.model.Lote;
import com.cyan.retocacao.repository.LoteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EstadoLoteService {
    private final LoteRepository lotes;
    private final AuditoriaService auditoria;
    public EstadoLoteService(LoteRepository lotes, AuditoriaService auditoria) { this.lotes = lotes; this.auditoria = auditoria; }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Lote cambiar(String codigo, @NotNull @Valid CambioEstadoSolicitud solicitud) {
        var lote = lotes.bloquearPorCodigo(codigo).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El lote no existe"));
        String nuevo = solicitud.estado().name();
        String motivo = solicitud.motivo().trim();
        if (nuevo.equals(lote.getEstadoLote()) && motivo.equals(lote.getMotivoEstado())) return lote;
        var anterior = new EstadoAnterior(lote.getEstadoLote(), lote.getMotivoEstado());
        lote.setEstadoLote(nuevo);
        lote.setMotivoEstado(motivo);
        lotes.flush();
        auditoria.registrar("CAMBIO_ESTADO", "LOTE", codigo, "estado_y_motivo", anterior,
            new EstadoAnterior(nuevo, motivo), motivo);
        return lote;
    }
    public record EstadoAnterior(String estado, String motivo) {}
}
