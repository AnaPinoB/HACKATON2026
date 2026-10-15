package com.Cyan.retoCacao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    @GetMapping
    public String listarLotes() {
        return "API de lotes de cacao funcionando";
    }
}