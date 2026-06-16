package com.vdch.abastecimiento.service;

import com.vdch.abastecimiento.model.Abastecimiento;
import com.vdch.abastecimiento.model.DetalleAbastecimiento;
import com.vdch.abastecimiento.repository.AbastecimientoRepository;
import java.util.List;

public class AbastecimientoService {
    private final AbastecimientoRepository abastecimientoRepository;

    public AbastecimientoService() {
        this(new AbastecimientoRepository());
    }

    public AbastecimientoService(AbastecimientoRepository abastecimientoRepository) {
        this.abastecimientoRepository = abastecimientoRepository;
    }

    public Long guardarAbastecimiento(Abastecimiento abastecimiento) {
        return abastecimientoRepository.guardar(abastecimiento);
    }

    public Long agregarProducto(Long idAbastecimiento, DetalleAbastecimiento detalle) {
        return abastecimientoRepository.agregarDetalle(idAbastecimiento, detalle);
    }

    public Long guardarAbastecimientoConDetalles(Abastecimiento abastecimiento, List<DetalleAbastecimiento> detalles) {
        Long idAbastecimiento = abastecimientoRepository.guardar(abastecimiento);
        for (DetalleAbastecimiento detalle : detalles) {
            abastecimientoRepository.agregarDetalle(idAbastecimiento, detalle);
        }
        return idAbastecimiento;
    }
}
