
package com.cyan.retocacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FichaPublicaDTO(
    String codigo,
    String productor,
    String origen,
    LocalDate fechaRecepcion,
    BigDecimal cantidadRecibida,
    String unidad,
    String estado,
    boolean disponibleParaVenta,
    List<EtapaPublicaDTO> trazabilidad
) {

    public record EtapaPublicaDTO(
        String etapa,
        LocalDate fecha,
        BigDecimal cantidadEntradaKg,
        BigDecimal cantidadSalidaKg
    ) {}
}
