package com.vdch.cliente.dto;

import java.math.BigDecimal;

public class ClienteResumen {
    private Long idCliente;
    private String nombres;
    private String telefono;
    private String direccion;
    private String notas;
    private BigDecimal saldoFiado;

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public BigDecimal getSaldoFiado() {
        return saldoFiado;
    }

    public void setSaldoFiado(BigDecimal saldoFiado) {
        this.saldoFiado = saldoFiado;
    }
}
