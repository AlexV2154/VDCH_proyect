package com.vdch.fiado.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FiadoResumen {
    private Long idCredito;
    private Long idCliente;
    private String cliente;
    private String telefono;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;
    private BigDecimal interesGenerado;
    private int diasVencidos;
    private String estado;
    private LocalDateTime fechaCredito;
    private LocalDate fechaLimite;

    public Long getIdCredito() {
        return idCredito;
    }

    public void setIdCredito(Long idCredito) {
        this.idCredito = idCredito;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public BigDecimal getInteresGenerado() {
        return interesGenerado;
    }

    public void setInteresGenerado(BigDecimal interesGenerado) {
        this.interesGenerado = interesGenerado;
    }

    public int getDiasVencidos() {
        return diasVencidos;
    }

    public void setDiasVencidos(int diasVencidos) {
        this.diasVencidos = diasVencidos;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCredito() {
        return fechaCredito;
    }

    public void setFechaCredito(LocalDateTime fechaCredito) {
        this.fechaCredito = fechaCredito;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }
}
