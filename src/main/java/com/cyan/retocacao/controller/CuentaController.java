package com.cyan.retocacao.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CuentaController {
    public record Token(String token, String headerName) {}
    public record Cuenta(String correo, List<String> permisos) {}

    @GetMapping("/csrf")
    public Token csrf(CsrfToken token) { return new Token(token.getToken(), token.getHeaderName()); }

    @GetMapping("/cuenta")
    public Cuenta cuenta(Authentication usuario) {
        return new Cuenta(usuario.getName(), usuario.getAuthorities().stream().map(Object::toString).toList());
    }
}
