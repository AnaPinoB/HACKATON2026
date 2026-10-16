package com.Cyan.retoCacao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import com.Cyan.retoCacao.model.Lote;
import com.Cyan.retoCacao.service.LoteService;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {
    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @GetMapping
    public List<Lote> listarLotes() {
        return loteService.listarLotes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Lote registrarLote(@RequestBody Lote lote) {
        return loteService.registrarLote(lote);
    }
}
