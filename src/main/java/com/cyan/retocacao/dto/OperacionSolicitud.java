package com.cyan.retocacao.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OperacionSolicitud(
    @NotBlank @Size(max = 36) String codigoOperacion,
    @NotBlank @Size(max = 20) String loteCodigo,
    @NotBlank @Size(max = 100) String etapa,
    @NotBlank @Size(max = 120) String responsable,
    @NotNull LocalDate fechaOperacion,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal cantidadEntradaKg,
    @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal cantidadSalidaKg,
    @Size(max = 2000) String observacion,
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal costoTotal,
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal precioVentaKg,
    @NotBlank @Size(max = 500) String costosIncluidos,
    @Size(max = 500) String justificacionBalance
) {}
