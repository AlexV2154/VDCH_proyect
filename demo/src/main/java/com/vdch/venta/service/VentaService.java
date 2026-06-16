package com.vdch.venta.service;

import com.vdch.venta.model.DetalleVenta;
import com.vdch.venta.model.Venta;
import com.vdch.venta.repository.VentaRepository;
import java.time.LocalDate;
import java.util.List;

public class VentaService {
    private final VentaRepository ventaRepository;

    public VentaService() {
        this(new VentaRepository());
    }

    public VentaService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    public Long registrarVenta(Venta venta) {
        return ventaRepository.registrar(venta);
    }

    public Long agregarProductoAVenta(Long idVenta, DetalleVenta detalle) {
        return ventaRepository.agregarDetalle(idVenta, detalle);
    }

    public Long registrarVentaConDetalles(Venta venta, List<DetalleVenta> detalles) {
        Long idVenta = ventaRepository.registrar(venta);
        for (DetalleVenta detalle : detalles) {
            ventaRepository.agregarDetalle(idVenta, detalle);
        }
        return idVenta;
    }

    public Long guardarVentaComoFiado(Venta venta, List<DetalleVenta> detalles, LocalDate fechaLimite, String observacion) {
        Long idVenta = registrarVentaConDetalles(venta, detalles);
        return ventaRepository.crearCreditoDesdeVenta(idVenta, fechaLimite, observacion);
    }

    public void anularVenta(Long idVenta) {
        ventaRepository.anular(idVenta);
    }
}
