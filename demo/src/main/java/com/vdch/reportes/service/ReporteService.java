package com.vdch.reportes.service;

import com.vdch.reportes.dto.ProductoVendidoReporte;
import com.vdch.reportes.dto.ResumenReporte;
import com.vdch.reportes.repository.ReporteRepository;
import java.time.LocalDate;
import java.util.List;

public class ReporteService {
    private final ReporteRepository reporteRepository;

    public ReporteService() {
        this(new ReporteRepository());
    }

    public ReporteService(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    public ResumenReporte obtenerResumenHoy() {
        return reporteRepository.obtenerResumenHoy();
    }

    public ResumenReporte obtenerResumen(LocalDate desde, LocalDate hastaExclusivo) {
        return reporteRepository.obtenerResumen(desde, hastaExclusivo);
    }

    public List<ProductoVendidoReporte> listarProductosMasVendidos(int limite) {
        return reporteRepository.listarProductosMasVendidos(limite);
    }

    public List<ProductoVendidoReporte> listarProductosMasVendidos(LocalDate desde, LocalDate hastaExclusivo, int limite) {
        return reporteRepository.listarProductosMasVendidos(desde, hastaExclusivo, limite);
    }
}
