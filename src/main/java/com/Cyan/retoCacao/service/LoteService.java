
package com.Cyan.retoCacao.service;

import com.Cyan.retoCacao.model.Lote;
import com.Cyan.retoCacao.repository.LoteRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LoteService {

    private final LoteRepository loteRepository;

    public LoteService(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    // RF01: Registrar un lote de cacao
    public Lote registrarLote(Lote lote) {

        // Validar identificador
        if (lote.getCodigo() == null ||
            lote.getCodigo().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El codigo del lote es obligatorio"
            );
        }

        // Validar codigo duplicado
        if (loteRepository.existsById(lote.getCodigo())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un lote con ese codigo"
            );
        }

        // Validar productor y origen
        if (lote.getProductor() == null ||
            lote.getProductor().isBlank() ||
            lote.getOrigen() == null ||
            lote.getOrigen().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El productor y el origen son obligatorios"
            );
        }

        // Validar fecha
        if (lote.getFecha() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha es obligatoria"
            );
        }

        // Validar cantidad mayor que cero
        if (lote.getCantidad() == null ||
            lote.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cantidad debe ser mayor que cero"
            );
        }

        // Validar unidad
        if (lote.getUnidad() == null ||
            lote.getUnidad().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La unidad es obligatoria"
            );
        }

        // Validar longitudes segun MySQL
        if (lote.getCodigo().length() > 20 ||
            lote.getProductor().length() > 120 ||
            lote.getOrigen().length() > 150 ||
            lote.getUnidad().length() > 10) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Uno de los campos supera la longitud permitida"
            );
        }

        // Evitar que el cliente establezca el estado inicial
        lote.setEstadoLote("PENDIENTE");

        // Guardar en MySQL
        return loteRepository.save(lote);
    }

    // Consultar todos los lotes
    public List<Lote> listarLotes() {
        return loteRepository.findAll();
    }
}
