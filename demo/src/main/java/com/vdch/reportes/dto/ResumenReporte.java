package com.vdch.reportes.dto;

import java.math.BigDecimal;

public class ResumenReporte {
    private BigDecimal ventas;
    private BigDecimal ganancia;
    private BigDecimal fiados;
    private BigDecimal pagado;

    public BigDecimal getVentas() {
        return ventas;
    }

    public void setVentas(BigDecimal ventas) {
        this.ventas = ventas;
    }

    public BigDecimal getGanancia() {
        return ganancia;
    }

    public void setGanancia(BigDecimal ganancia) {
        this.ganancia = ganancia;
    }

    public BigDecimal getFiados() {
        return fiados;
    }

    public void setFiados(BigDecimal fiados) {
        this.fiados = fiados;
    }

    public BigDecimal getPagado() {
        return pagado;
    }

    public void setPagado(BigDecimal pagado) {
        this.pagado = pagado;
    }
}
