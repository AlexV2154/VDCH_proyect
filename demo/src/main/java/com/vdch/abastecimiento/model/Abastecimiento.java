package com.vdch.abastecimiento.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Abastecimiento {
    private Long idAbastecimiento;
    private Long idUsuario;
    private String proveedor;
    private String lugarCompra;
    private String tipoAbastecimiento;
    private String contacto;
    private String comprobante;
    private BigDecimal total;
    private LocalDateTime fechaAbastecimiento;
    private String observacion;

    public Long getIdAbastecimiento() {
        return idAbastecimiento;
    }

    public void setIdAbastecimiento(Long idAbastecimiento) {
        this.idAbastecimiento = idAbastecimiento;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getLugarCompra() {
        return lugarCompra;
    }

    public void setLugarCompra(String lugarCompra) {
        this.lugarCompra = lugarCompra;
    }

    public String getTipoAbastecimiento() {
        return tipoAbastecimiento;
    }

    public void setTipoAbastecimiento(String tipoAbastecimiento) {
        this.tipoAbastecimiento = tipoAbastecimiento;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getComprobante() {
        return comprobante;
    }

    public void setComprobante(String comprobante) {
        this.comprobante = comprobante;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getFechaAbastecimiento() {
        return fechaAbastecimiento;
    }

    public void setFechaAbastecimiento(LocalDateTime fechaAbastecimiento) {
        this.fechaAbastecimiento = fechaAbastecimiento;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
