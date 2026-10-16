package com.cyan.retocacao.controller;

import com.cyan.retocacao.dto.CambioEstadoSolicitud;
import com.cyan.retocacao.model.Lote;
import com.cyan.retocacao.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lotes")
public class EstadoLoteController {
    private final EstadoLoteService estados;
    private final AuditoriaService auditoria;
    public EstadoLoteController(EstadoLoteService estados, AuditoriaService auditoria) {
        this.estados = estados; this.auditoria = auditoria;
    }
    @PatchMapping("/{codigo}/estado")
    public Lote cambiar(@PathVariable String codigo, @Valid @RequestBody CambioEstadoSolicitud solicitud) {
        return estados.cambiar(codigo, solicitud);
    }
    @GetMapping("/{codigo}/auditoria")
    public List<AuditoriaService.Cambio> historial(@PathVariable String codigo) { return auditoria.porLote(codigo); }
}
