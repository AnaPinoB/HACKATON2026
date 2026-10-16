package com.cyan.retocacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambioEstadoSolicitud(@NotNull Estado estado, @NotBlank @Size(max = 255) String motivo) {
    public enum Estado { LIBERADO, BLOQUEADO }
}
