
package com.cyan.retocacao.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @Column(name = "codigo", length = 20)
    private String codigo;

    @Column(name = "productor", nullable = false, length = 120)
    private String productor;

    @Column(name = "origen", nullable = false, length = 150)
    private String origen;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "unidad", nullable = false, length = 10)
    private String unidad = "kg";

    @Column(name = "estado_lote", nullable = false, length = 20)
    private String estadoLote = "PENDIENTE";

    @Column(name = "motivo_estado", length = 255)
    private String motivoEstado;

    @Column(name = "observacion", length = 2000)
    private String observacion;

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getMotivoEstado() { return motivoEstado; }
    public void setMotivoEstado(String motivoEstado) { this.motivoEstado = motivoEstado; }

    @Column(name = "fecha_creacion_registro_lote",
            insertable = false, updatable = false)
    private LocalDateTime fechaCreacionRegistroLote;

    public Lote() {
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getProductor() {
        return productor;
    }

    public void setProductor(String productor) {
        this.productor = productor;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getEstadoLote() {
        return estadoLote;
    }

    public void setEstadoLote(String estadoLote) {
        this.estadoLote = estadoLote;
    }

    public LocalDateTime getFechaCreacionRegistroLote() {
        return fechaCreacionRegistroLote;
    }
}
