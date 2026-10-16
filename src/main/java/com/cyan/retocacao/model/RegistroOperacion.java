package com.cyan.retocacao.model;

import com.cyan.retocacao.dto.OperacionSolicitud;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_operaciones")
public class RegistroOperacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro") private Long id;
    @Column(name = "codigo_operacion", nullable = false, unique = true, length = 36) private String codigoOperacion;
    @Column(name = "lote_codigo", nullable = false, length = 20) private String loteCodigo;
    @Column(nullable = false, length = 100) private String etapa;
    @Column(nullable = false, length = 120) private String responsable;
    @Column(name = "fecha_operacion", nullable = false) private LocalDate fechaOperacion;
    @Column(name = "cantidad_entrada_kg", nullable = false, precision = 10, scale = 2) private BigDecimal cantidadEntradaKg;
    @Column(name = "cantidad_salida_kg", nullable = false, precision = 10, scale = 2) private BigDecimal cantidadSalidaKg;
    @Column(columnDefinition = "TEXT") private String observacion;
    @Column(name = "costo_total", nullable = false, precision = 12, scale = 2) private BigDecimal costoTotal;
    @Column(name = "precio_venta_kg", precision = 12, scale = 2) private BigDecimal precioVentaKg;
    @Column(name = "costos_incluidos", length = 500) private String costosIncluidos;
    @Column(name = "justificacion_balance", length = 500) private String justificacionBalance;
    @Column(name = "fecha_creacion_registro", insertable = false, updatable = false) private LocalDateTime fechaCreacion;

    protected RegistroOperacion() {}

    public RegistroOperacion(OperacionSolicitud s) {
        codigoOperacion = s.codigoOperacion(); loteCodigo = s.loteCodigo();
        etapa = s.etapa(); responsable = s.responsable(); fechaOperacion = s.fechaOperacion();
        cantidadEntradaKg = s.cantidadEntradaKg(); cantidadSalidaKg = s.cantidadSalidaKg();
        observacion = s.observacion(); costoTotal = s.costoTotal(); precioVentaKg = s.precioVentaKg();
        costosIncluidos = s.costosIncluidos(); justificacionBalance = s.justificacionBalance();
    }

    public OperacionSolicitud datos() {
        return new OperacionSolicitud(codigoOperacion, loteCodigo, etapa, responsable, fechaOperacion,
            cantidadEntradaKg, cantidadSalidaKg, observacion, costoTotal, precioVentaKg,
            costosIncluidos, justificacionBalance);
    }
    public Long getId() { return id; }
    public String getCodigoOperacion() { return codigoOperacion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
