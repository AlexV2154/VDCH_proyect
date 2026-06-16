package com.vdch.reportes.service;

import com.vdch.reportes.dto.ProductoVendidoReporte;
import com.vdch.reportes.dto.ResumenReporte;
import com.vdch.reportes.repository.ReporteRepository;
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

    public List<ProductoVendidoReporte> listarProductosMasVendidos(int limite) {
        return reporteRepository.listarProductosMasVendidos(limite);
    }
}
