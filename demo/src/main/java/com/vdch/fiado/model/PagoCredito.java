package com.vdch.fiado.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagoCredito {
    private Long idPagoCredito;
    private Long idCredito;
    private BigDecimal montoPagado;
    private String metodoPago;
    private LocalDateTime fechaPago;
    private String observacion;

    public Long getIdPagoCredito() {
        return idPagoCredito;
    }

    public void setIdPagoCredito(Long idPagoCredito) {
        this.idPagoCredito = idPagoCredito;
    }

    public Long getIdCredito() {
        return idCredito;
    }

    public void setIdCredito(Long idCredito) {
        this.idCredito = idCredito;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
