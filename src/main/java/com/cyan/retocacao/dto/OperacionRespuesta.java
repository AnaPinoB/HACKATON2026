package com.cyan.retocacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionRespuesta(Long id, OperacionSolicitud datos, LocalDateTime fechaCreacion,
    BigDecimal mermaPorcentaje, boolean alertaMerma, String causaAlerta,
    BigDecimal costoUnitarioCopKg, BigDecimal margenSobreVentaPorcentaje,
    String formulaMerma, String formulaCosto, String formulaMargen, String supuestos) {}
