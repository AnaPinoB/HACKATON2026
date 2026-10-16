package com.cyan.retocacao.dto;

import com.cyan.retocacao.model.Lote;
import java.math.BigDecimal;
import java.util.List;

public record TrazabilidadLote(Lote lote, BigDecimal recibidoKg, BigDecimal consumidoKg,
    BigDecimal disponibleKg, BigDecimal salidaProducidaKg, List<OperacionRespuesta> operaciones) {}
