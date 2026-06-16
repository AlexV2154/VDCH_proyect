package com.vdch.abastecimiento.repository;

import com.vdch.abastecimiento.model.Abastecimiento;
import com.vdch.abastecimiento.model.DetalleAbastecimiento;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;

public class AbastecimientoRepository {
    private final StoredProcedureExecutor executor;

    public AbastecimientoRepository() {
        this(new StoredProcedureExecutor());
    }

    public AbastecimientoRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public Long guardar(Abastecimiento abastecimiento) {
        return executor.callForLong(
                DbFunctions.GUARDAR_ABASTECIMIENTO,
                abastecimiento.getIdAbastecimiento(),
                abastecimiento.getIdUsuario(),
                abastecimiento.getProveedor(),
                abastecimiento.getLugarCompra(),
                abastecimiento.getTipoAbastecimiento(),
                abastecimiento.getContacto(),
                abastecimiento.getComprobante(),
                abastecimiento.getObservacion()
        );
    }

    public Long agregarDetalle(Long idAbastecimiento, DetalleAbastecimiento detalle) {
        return executor.callForLong(
                DbFunctions.AGREGAR_DETALLE_ABASTECIMIENTO,
                idAbastecimiento,
                detalle.getIdProducto(),
                detalle.getCantidad(),
                detalle.getUnidadCompra(),
                detalle.getCostoUnitario(),
                detalle.getPrecioVenta()
        );
    }
}
