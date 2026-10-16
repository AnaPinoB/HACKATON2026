
package com.cyan.retocacao.service;

import com.cyan.retocacao.model.Lote;
import com.cyan.retocacao.repository.LoteRepository;
import com.cyan.retocacao.repository.LoteRegistroRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

@Service
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR')")
public class LoteService {

    private final LoteRepository loteRepository;
    private final LoteRegistroRepository registroRepository;
    private final AuditoriaService auditoria;

    public LoteService(LoteRepository loteRepository, LoteRegistroRepository registroRepository, AuditoriaService auditoria) {
        this.loteRepository = loteRepository;
        this.registroRepository = registroRepository;
        this.auditoria = auditoria;
    }

    // RF01: Registrar un lote de cacao
    @org.springframework.transaction.annotation.Transactional
    public Lote registrarLote(Lote lote) {

        if (lote == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El lote es obligatorio");
        }

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
        if (lote.getCantidad().compareTo(new BigDecimal("99999999.99")) > 0 ||
            lote.getCantidad().stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La cantidad admite hasta 99999999.99 y un maximo de dos decimales");
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
        lote.setMotivoEstado(null);

        // Guardar en MySQL
        var guardado = registroRepository.insertar(lote);
        auditoria.registrar("RECEPCION", "LOTE", lote.getCodigo(), "registro", null, guardado, "Recepcion inicial");
        return guardado;
    }

    // Consultar todos los lotes
    public List<Lote> listarLotes() {
        return loteRepository.findAll();
    }
}
