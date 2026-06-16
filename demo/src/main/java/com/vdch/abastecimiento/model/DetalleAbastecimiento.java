package com.vdch.abastecimiento.model;

import java.math.BigDecimal;

public class DetalleAbastecimiento {
    private Long idDetalleAbastecimiento;
    private Long idAbastecimiento;
    private Long idProducto;
    private BigDecimal cantidad;
    private String unidadCompra;
    private BigDecimal costoUnitario;
    private BigDecimal precioVenta;
    private BigDecimal subtotal;

    public Long getIdDetalleAbastecimiento() {
        return idDetalleAbastecimiento;
    }

    public void setIdDetalleAbastecimiento(Long idDetalleAbastecimiento) {
        this.idDetalleAbastecimiento = idDetalleAbastecimiento;
    }

    public Long getIdAbastecimiento() {
        return idAbastecimiento;
    }

    public void setIdAbastecimiento(Long idAbastecimiento) {
        this.idAbastecimiento = idAbastecimiento;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadCompra() {
        return unidadCompra;
    }

    public void setUnidadCompra(String unidadCompra) {
        this.unidadCompra = unidadCompra;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
