package com.Cyan.retoCacao.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Lote {

    private String id;
    private String productor;
    private String origen;
    private LocalDate fecha;
    private BigDecimal cantidad;
    private String unidad;

    public Lote() {
    }

    public Lote(String id, String productor, String origen,
                LocalDate fecha, BigDecimal cantidad, String unidad) {
        this.id = id;
        this.productor = productor;
        this.origen = origen;
        this.fecha = fecha;
        this.cantidad = cantidad;
        this.unidad = unidad;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}