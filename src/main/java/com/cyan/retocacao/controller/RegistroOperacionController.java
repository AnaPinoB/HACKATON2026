package com.cyan.retocacao.controller;

import com.cyan.retocacao.dto.*;
import com.cyan.retocacao.service.RegistroOperacionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RegistroOperacionController {
    private final RegistroOperacionService service;
    public RegistroOperacionController(RegistroOperacionService service) { this.service = service; }

    @PostMapping("/operaciones")
    public ResponseEntity<OperacionRespuesta> registrar(@Valid @RequestBody OperacionSolicitud solicitud) {
        var resultado = service.registrar(solicitud);
        return ResponseEntity.status(resultado.creada() ? 201 : 200).body(resultado.operacion());
    }
    @GetMapping("/operaciones")
    public List<OperacionRespuesta> listar() { return service.listar(); }

    @GetMapping("/lotes/{codigo}/operaciones")
    public List<OperacionRespuesta> porLote(@PathVariable String codigo) { return service.trazabilidad(codigo).operaciones(); }

    @GetMapping("/lotes/{codigo}/trazabilidad")
    public TrazabilidadLote trazabilidad(@PathVariable String codigo) { return service.trazabilidad(codigo); }
}
