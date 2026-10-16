
package com.Cyan.retoCacao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class CostoService {

    // RF05 y RF12: Calcular costo por unidad producida
    public BigDecimal calcularCostoUnitario(
            BigDecimal costoTotal,
            BigDecimal cantidadProducida) {

        if (costoTotal == null ||
            cantidadProducida == null ||
            costoTotal.compareTo(BigDecimal.ZERO) < 0 ||
            cantidadProducida.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "El costo no puede ser negativo y la cantidad debe ser mayor que cero"
            );
        }

        return costoTotal.divide(
                cantidadProducida,
                2,
                RoundingMode.HALF_UP
        );
    }

    // RF12: Calcular margen estimado de ganancia
    public BigDecimal calcularMargen(
            BigDecimal precioVentaUnitario,
            BigDecimal costoUnitario) {

        if (precioVentaUnitario == null ||
            costoUnitario == null ||
            precioVentaUnitario.compareTo(BigDecimal.ZERO) <= 0 ||
            costoUnitario.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                "El precio debe ser mayor que cero y el costo no puede ser negativo"
            );
        }

        BigDecimal ganancia =
                precioVentaUnitario.subtract(costoUnitario);

        return ganancia
                .multiply(new BigDecimal("100"))
                .divide(precioVentaUnitario, 2, RoundingMode.HALF_UP);
    }
}
