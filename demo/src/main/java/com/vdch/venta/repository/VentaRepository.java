package com.vdch.venta.repository;

import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import com.vdch.venta.model.DetalleVenta;
import com.vdch.venta.model.Venta;
import java.sql.Date;
import java.time.LocalDate;

public class VentaRepository {
    private final StoredProcedureExecutor executor;

    public VentaRepository() {
        this(new StoredProcedureExecutor());
    }

    public VentaRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public Long registrar(Venta venta) {
        return executor.callForLong(
                DbFunctions.REGISTRAR_VENTA,
                venta.getIdCliente(),
                venta.getIdUsuario(),
                venta.getCanalVenta(),
                venta.getTipoPago(),
                venta.getTotal(),
                venta.getMontoPagado()
        );
    }

    public Long agregarDetalle(Long idVenta, DetalleVenta detalle) {
        return executor.callForLong(
                DbFunctions.AGREGAR_DETALLE_VENTA,
                idVenta,
                detalle.getIdProducto(),
                detalle.getCantidad(),
                detalle.getUnidadVenta(),
                detalle.getCantidadTexto(),
                detalle.getPrecioUnitario()
        );
    }

    public Long crearCreditoDesdeVenta(Long idVenta, LocalDate fechaLimite, String observacion) {
        Date fechaSql = fechaLimite == null ? null : Date.valueOf(fechaLimite);
        return executor.callForLong(DbFunctions.CREAR_CREDITO_DESDE_VENTA, idVenta, fechaSql, observacion);
    }

    public void anular(Long idVenta) {
        executor.call(DbFunctions.ANULAR_VENTA, idVenta);
    }
}
