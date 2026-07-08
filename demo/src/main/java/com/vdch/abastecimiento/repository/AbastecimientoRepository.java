package com.vdch.abastecimiento.repository;

import com.vdch.abastecimiento.model.Abastecimiento;
import com.vdch.abastecimiento.model.DetalleAbastecimiento;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.math.BigDecimal;
import java.sql.Date;

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
        BigDecimal cantidadStock = executor.jdbcTemplate().queryForObject(
                "SELECT fn_normalizar_cantidad(?, ?, ?)",
                BigDecimal.class,
                detalle.getIdProducto(),
                detalle.getCantidad(),
                detalle.getUnidadCompra()
        );
        BigDecimal subtotal = cantidadStock.multiply(detalle.getCostoUnitario());
        Date fechaVencimiento = detalle.getFechaVencimiento() == null ? null : Date.valueOf(detalle.getFechaVencimiento());
        Long idDetalle = executor.jdbcTemplate().queryForObject("""
                INSERT INTO detalle_abastecimientos (
                    id_abastecimiento, id_producto, cantidad, unidad_compra, costo_unitario, precio_venta,
                    lote, fecha_vencimiento, subtotal
                ) VALUES (?, ?, ?, UPPER(COALESCE(?, 'UNIDAD')), ?, COALESCE(?, 0), ?, ?, ?)
                RETURNING id_detalle_abastecimiento
                """, Long.class,
                idAbastecimiento,
                detalle.getIdProducto(),
                detalle.getCantidad(),
                detalle.getUnidadCompra(),
                detalle.getCostoUnitario(),
                detalle.getPrecioVenta(),
                detalle.getLote(),
                fechaVencimiento,
                subtotal
        );
        executor.jdbcTemplate().update("""
                UPDATE productos
                SET stock_actual = stock_actual + ?,
                    precio_compra = ?,
                    precio_venta = COALESCE(NULLIF(?, 0), precio_venta),
                    lote = COALESCE(NULLIF(?, ''), lote),
                    fecha_vencimiento = COALESCE(?, fecha_vencimiento)
                WHERE id_producto = ?
                """, cantidadStock, detalle.getCostoUnitario(), detalle.getPrecioVenta(), detalle.getLote(), fechaVencimiento, detalle.getIdProducto());
        executor.jdbcTemplate().update("""
                UPDATE abastecimientos
                SET total = (
                    SELECT COALESCE(SUM(subtotal), 0)
                    FROM detalle_abastecimientos
                    WHERE id_abastecimiento = ?
                )
                WHERE id_abastecimiento = ?
                """, idAbastecimiento, idAbastecimiento);
        return idDetalle;
    }
}
