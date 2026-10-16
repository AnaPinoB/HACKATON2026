package com.cyan.retocacao.service;

import com.cyan.retocacao.dto.*;
import com.cyan.retocacao.model.RegistroOperacion;
import com.cyan.retocacao.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
public class RegistroOperacionService {
    private final LoteRepository lotes;
    private final RegistroOperacionRepository operaciones;
    private final MermaService mermas;
    private final CostoService costos;
    private final EntityManager em;
    private final AuditoriaService auditoria;

    public RegistroOperacionService(LoteRepository lotes, RegistroOperacionRepository operaciones,
        MermaService mermas, CostoService costos, EntityManager em, AuditoriaService auditoria) {
        this.lotes = lotes; this.operaciones = operaciones; this.mermas = mermas;
        this.costos = costos; this.em = em;
        this.auditoria = auditoria;
    }

    public record ResultadoRegistro(boolean creada, OperacionRespuesta operacion) {}

    // READ_COMMITTED permite ver lo confirmado por la solicitud que tenia el bloqueo.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OPERADOR', 'ADMINISTRADOR')")
    public ResultadoRegistro registrar(@NotNull @Valid OperacionSolicitud solicitud) {
        var s = normalizar(solicitud);
        var lote = lotes.bloquearPorCodigo(s.loteCodigo()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "El lote no existe"));
        var existente = operaciones.findByCodigoOperacion(s.codigoOperacion());
        if (existente.isPresent()) {
            if (!normalizar(existente.get().datos()).equals(s)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El codigo de operacion ya existe con datos diferentes");
            }
            return new ResultadoRegistro(false, respuesta(existente.get()));
        }
        if (!"kg".equalsIgnoreCase(lote.getUnidad())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La transformacion requiere un lote en kg");
        }
        if ("BLOQUEADO".equals(lote.getEstadoLote())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede transformar un lote bloqueado");
        }
        if (s.fechaOperacion().isBefore(lote.getFecha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La operacion no puede preceder a la recepcion");
        }
        if (s.cantidadSalidaKg().compareTo(s.cantidadEntradaKg()) > 0 && s.justificacionBalance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Una salida mayor que la entrada requiere justificacion del balance");
        }
        var disponible = lote.getCantidad().subtract(operaciones.cantidadConsumida(lote.getCodigo()));
        if (s.cantidadEntradaKg().compareTo(disponible) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventario insuficiente. Disponible: " + disponible + " kg");
        }
        var operacion = operaciones.saveAndFlush(new RegistroOperacion(s));
        em.refresh(operacion);
        auditoria.registrar("TRANSFORMACION", "OPERACION", s.codigoOperacion(), "registro", null, s,
            "Operacion asociada al lote " + lote.getCodigo());
        auditoria.registrar("CONSUMO", "LOTE", lote.getCodigo(), "disponible_kg", disponible,
            disponible.subtract(s.cantidadEntradaKg()), "Operacion " + s.codigoOperacion());
        return new ResultadoRegistro(true, respuesta(operacion));
    }

    @Transactional(readOnly = true)
    public List<OperacionRespuesta> listar() {
        return operaciones.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(this::respuesta).toList();
    }

    // El mismo bloqueo evita combinar inventario y operaciones de instantes distintos.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TrazabilidadLote trazabilidad(String codigo) {
        var lote = lotes.bloquearPorCodigo(codigo).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "El lote no existe"));
        if (!"kg".equalsIgnoreCase(lote.getUnidad())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este balance requiere un lote en kg");
        }
        var registros = operaciones.findByLoteCodigoOrderByFechaOperacionAscIdAsc(codigo);
        var consumido = registros.stream().map(o -> o.datos().cantidadEntradaKg()).reduce(BigDecimal.ZERO, BigDecimal::add);
        var producido = registros.stream().map(o -> o.datos().cantidadSalidaKg()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TrazabilidadLote(lote, lote.getCantidad(), consumido, lote.getCantidad().subtract(consumido),
            producido, registros.stream().map(this::respuesta).toList());
    }

    private OperacionRespuesta respuesta(RegistroOperacion o) {
        var s = o.datos();
        boolean comparable = texto(s.justificacionBalance()) == null;
        BigDecimal merma = comparable ? mermas.calcularMerma(s.cantidadEntradaKg(), s.cantidadSalidaKg()) : null;
        boolean alerta = comparable && s.cantidadEntradaKg().subtract(s.cantidadSalidaKg()).multiply(new BigDecimal("100"))
            .compareTo(s.cantidadEntradaKg().multiply(new BigDecimal("15"))) > 0;
        BigDecimal unitario = s.cantidadSalidaKg().signum() > 0 ? costos.calcularCostoUnitario(s.costoTotal(), s.cantidadSalidaKg()) : null;
        BigDecimal margen = unitario != null && s.precioVentaKg() != null && s.precioVentaKg().signum() > 0
            ? costos.calcularMargen(s.precioVentaKg(), unitario) : null;
        return new OperacionRespuesta(o.getId(), s, o.getFechaCreacion(), merma, alerta,
            alerta ? "La perdida de masa supera el 15 % de la entrada (comparacion sin redondeo)" : null,
            unitario, margen, "(entrada - salida) / entrada * 100",
            "costo total COP / salida vendible kg", "(precio COP/kg - costo unitario COP/kg) / precio COP/kg * 100",
            (comparable ? "Mismo material y base de masa, sin insumos agregados. " :
                "Bases diferentes: no se calcula merma ni alerta automatica. Justificacion: " + s.justificacionBalance() + ". ") +
            "La salida declarada se considera vendible. Salida cero: costo unitario y margen no calculables. " +
            "El precio es opcional; sin precio no se calcula margen. Costos de esta operacion: " + s.costosIncluidos());
    }

    private static String texto(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static BigDecimal numero(BigDecimal n) { return n == null ? null : n.stripTrailingZeros(); }
    private static OperacionSolicitud normalizar(OperacionSolicitud s) {
        return new OperacionSolicitud(texto(s.codigoOperacion()), texto(s.loteCodigo()), texto(s.etapa()),
            texto(s.responsable()), s.fechaOperacion(), numero(s.cantidadEntradaKg()), numero(s.cantidadSalidaKg()),
            texto(s.observacion()), numero(s.costoTotal()), numero(s.precioVentaKg()), texto(s.costosIncluidos()), texto(s.justificacionBalance()));
    }
}
