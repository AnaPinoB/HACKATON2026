
package com.cyan.retocacao.controller;

import com.cyan.retocacao.dto.FichaPublicaDTO;
import com.cyan.retocacao.service.FichaPublicaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico/productos")
public class FichaPublicaController {

    private final FichaPublicaService fichaPublicaService;

    public FichaPublicaController(
            FichaPublicaService fichaPublicaService) {
        this.fichaPublicaService = fichaPublicaService;
    }

    // RF06: Consultar la ficha comercial publica
    @GetMapping("/{codigo}")
    public ResponseEntity<FichaPublicaDTO> consultar(
            @PathVariable String codigo) {

        FichaPublicaDTO ficha =
            fichaPublicaService.consultar(codigo);

        return ResponseEntity.ok(ficha);
    }
}
