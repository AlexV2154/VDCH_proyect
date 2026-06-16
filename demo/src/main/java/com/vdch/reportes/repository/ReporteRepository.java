package com.vdch.reportes.repository;

import com.vdch.reportes.dto.ProductoVendidoReporte;
import com.vdch.reportes.dto.ResumenReporte;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.util.List;

public class ReporteRepository {
    private final StoredProcedureExecutor executor;

    public ReporteRepository() {
        this(new StoredProcedureExecutor());
    }

    public ReporteRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public ResumenReporte obtenerResumenHoy() {
        List<ResumenReporte> resumen;
        try {
            resumen = executor.query(DbFunctions.RESUMEN_HOY, (rs, rowNum) -> {
                ResumenReporte reporte = new ResumenReporte();
                reporte.setVentas(rs.getBigDecimal("ventas"));
                reporte.setGanancia(rs.getBigDecimal("ganancia"));
                reporte.setFiados(rs.getBigDecimal("fiados"));
                reporte.setPagado(rs.getBigDecimal("pagado"));
                return reporte;
            });
        } catch (RuntimeException ex) {
            resumen = executor.querySql("""
                    WITH ventas_hoy AS (
                        SELECT *
                        FROM ventas
                        WHERE estado <> 'ANULADA'
                          AND fecha_venta::date = CURRENT_DATE
                    ),
                    totales AS (
                        SELECT
                            COALESCE(SUM(total), 0) AS ventas,
                            COALESCE(SUM(saldo_pendiente), 0) AS fiados,
                            COALESCE(SUM(monto_pagado), 0) AS pagado
                        FROM ventas_hoy
                    ),
                    ganancias AS (
                        SELECT COALESCE(SUM((dv.precio_unitario - p.precio_compra) * dv.cantidad), 0) AS ganancia
                        FROM detalle_ventas dv
                        JOIN ventas_hoy v ON v.id_venta = dv.id_venta
                        JOIN productos p ON p.id_producto = dv.id_producto
                    )
                    SELECT t.ventas, g.ganancia, t.fiados, t.pagado
                    FROM totales t
                    CROSS JOIN ganancias g
                    """, (rs, rowNum) -> {
            ResumenReporte reporte = new ResumenReporte();
            reporte.setVentas(rs.getBigDecimal("ventas"));
            reporte.setGanancia(rs.getBigDecimal("ganancia"));
            reporte.setFiados(rs.getBigDecimal("fiados"));
            reporte.setPagado(rs.getBigDecimal("pagado"));
            return reporte;
            });
        }
        return resumen.isEmpty() ? new ResumenReporte() : resumen.get(0);
    }

    public List<ProductoVendidoReporte> listarProductosMasVendidos(int limite) {
        try {
            return executor.query(DbFunctions.PRODUCTOS_MAS_VENDIDOS, (rs, rowNum) -> {
                ProductoVendidoReporte producto = new ProductoVendidoReporte();
                producto.setIdProducto(rs.getLong("id_producto"));
                producto.setNombre(rs.getString("nombre"));
                producto.setCantidadVendida(rs.getBigDecimal("cantidad_vendida"));
                producto.setTotalVendido(rs.getBigDecimal("total_vendido"));
                return producto;
            }, limite);
        } catch (RuntimeException ex) {
            return executor.querySql("""
                    SELECT p.id_producto, p.nombre,
                           COALESCE(SUM(dv.cantidad), 0) AS cantidad_vendida,
                           COALESCE(SUM(dv.subtotal), 0) AS total_vendido
                    FROM detalle_ventas dv
                    JOIN ventas v ON v.id_venta = dv.id_venta
                    JOIN productos p ON p.id_producto = dv.id_producto
                    WHERE v.estado <> 'ANULADA'
                    GROUP BY p.id_producto, p.nombre
                    ORDER BY cantidad_vendida DESC, total_vendido DESC
                    LIMIT ?
                    """, (rs, rowNum) -> {
            ProductoVendidoReporte producto = new ProductoVendidoReporte();
            producto.setIdProducto(rs.getLong("id_producto"));
            producto.setNombre(rs.getString("nombre"));
            producto.setCantidadVendida(rs.getBigDecimal("cantidad_vendida"));
            producto.setTotalVendido(rs.getBigDecimal("total_vendido"));
            return producto;
            }, limite);
        }
    }
}
