package com.vdch.fiado.service;

import com.vdch.fiado.dto.FiadoResumen;
import com.vdch.fiado.model.PagoCredito;
import com.vdch.fiado.repository.FiadoRepository;
import java.math.BigDecimal;
import java.util.List;

public class FiadoService {
    private final FiadoRepository fiadoRepository;

    public FiadoService() {
        this(new FiadoRepository());
    }

    public FiadoService(FiadoRepository fiadoRepository) {
        this.fiadoRepository = fiadoRepository;
    }

    public List<FiadoResumen> listarFiadosPendientes() {
        return fiadoRepository.listarPendientes();
    }

    public List<PagoCredito> listarPagosPorCredito(Long idCredito) {
        return fiadoRepository.listarPagosPorCredito(idCredito);
    }
    public Long registrarPago(Long idCredito, BigDecimal montoPagado, String metodoPago, String observacion) {
        return fiadoRepository.registrarPago(idCredito, montoPagado, metodoPago, observacion);
    }
}
