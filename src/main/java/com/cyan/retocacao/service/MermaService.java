
package com.cyan.retocacao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class MermaService {

    // RF04: Calcular el porcentaje de merma
    public BigDecimal calcularMerma(
            BigDecimal cantidadEntrada,
            BigDecimal cantidadSalida) {

        if (cantidadEntrada == null ||
            cantidadSalida == null ||
            cantidadEntrada.compareTo(BigDecimal.ZERO) <= 0 ||
            cantidadSalida.compareTo(BigDecimal.ZERO) < 0 ||
            cantidadSalida.compareTo(cantidadEntrada) > 0) {

            throw new IllegalArgumentException(
                "Las cantidades de entrada y salida no son validas"
            );
        }

        BigDecimal diferencia =
                cantidadEntrada.subtract(cantidadSalida);

        return diferencia
                .multiply(new BigDecimal("100"))
                .divide(cantidadEntrada, 2, RoundingMode.HALF_UP);
    }

    // RF04: Generar alerta si la merma supera el 15%
    public boolean generarAlerta(BigDecimal porcentajeMerma) {

        if (porcentajeMerma == null) {
            throw new IllegalArgumentException(
                "El porcentaje de merma es obligatorio"
            );
        }

        return porcentajeMerma.compareTo(
                new BigDecimal("15")) > 0;
    }
}

