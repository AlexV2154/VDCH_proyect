package com.vdch.inventario.repository;

import com.vdch.inventario.dto.ProductoStockBajo;
import com.vdch.inventario.model.Producto;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ProductoRepository {
    private final StoredProcedureExecutor executor;

    public ProductoRepository() {
        this(new StoredProcedureExecutor());
    }

    public ProductoRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public Long guardar(Producto producto) {
        Date fechaVencimiento = producto.getFechaVencimiento() == null ? null : Date.valueOf(producto.getFechaVencimiento());
        return executor.callForLong(
                DbFunctions.GUARDAR_PRODUCTO,
                producto.getIdProducto(),
                producto.getIdCategoria(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getTipoVenta(),
                producto.getUnidadBase(),
                producto.getEquivalencia(),
                producto.getPrecioCompra(),
                producto.getPrecioVenta(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                fechaVencimiento
        );
    }

    public List<Producto> buscar(String texto) {
        try {
            return executor.query(DbFunctions.BUSCAR_PRODUCTOS, (rs, rowNum) -> mapProducto(rs), texto);
        } catch (RuntimeException ex) {
            return buscarDirecto(texto);
        }
    }

    public List<ProductoStockBajo> listarStockBajo() {
        try {
            return executor.query(DbFunctions.PRODUCTOS_STOCK_BAJO, (rs, rowNum) -> {
                ProductoStockBajo producto = new ProductoStockBajo();
                producto.setIdProducto(rs.getLong("id_producto"));
                producto.setNombre(rs.getString("nombre"));
                producto.setStockActual(rs.getBigDecimal("stock_actual"));
                producto.setStockMinimo(rs.getBigDecimal("stock_minimo"));
                producto.setUnidadBase(rs.getString("unidad_base"));
                return producto;
            });
        } catch (RuntimeException ex) {
            return executor.querySql("""
                    SELECT id_producto, nombre, stock_actual, stock_minimo, unidad_base
                    FROM productos
                    WHERE estado = TRUE
                      AND stock_actual <= stock_minimo
                    ORDER BY stock_actual ASC, nombre ASC
                    """, (rs, rowNum) -> {
                ProductoStockBajo producto = new ProductoStockBajo();
                producto.setIdProducto(rs.getLong("id_producto"));
                producto.setNombre(rs.getString("nombre"));
                producto.setStockActual(rs.getBigDecimal("stock_actual"));
                producto.setStockMinimo(rs.getBigDecimal("stock_minimo"));
                producto.setUnidadBase(rs.getString("unidad_base"));
                return producto;
            });
        }
    }

    private List<Producto> buscarDirecto(String texto) {
        String filter = texto == null || texto.isBlank() ? null : "%" + texto + "%";
        return executor.querySql("""
                SELECT id_producto, id_categoria, nombre, descripcion, tipo_venta, unidad_base,
                       precio_venta, stock_actual, stock_minimo
                FROM productos
                WHERE estado = TRUE
                  AND (? IS NULL OR nombre ILIKE ? OR descripcion ILIKE ?)
                ORDER BY nombre
                """, (rs, rowNum) -> mapProducto(rs), filter, filter, filter);
    }

    private Producto mapProducto(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(rs.getLong("id_producto"));
        producto.setIdCategoria(rs.getObject("id_categoria", Long.class));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setTipoVenta(rs.getString("tipo_venta"));
        producto.setUnidadBase(rs.getString("unidad_base"));
        producto.setEquivalencia(hasColumn(rs, "equivalencia") ? rs.getBigDecimal("equivalencia") : BigDecimal.ONE);
        producto.setPrecioCompra(hasColumn(rs, "precio_compra") ? rs.getBigDecimal("precio_compra") : BigDecimal.ZERO);
        producto.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        producto.setStockActual(rs.getBigDecimal("stock_actual"));
        producto.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        Date fechaVencimiento = hasColumn(rs, "fecha_vencimiento") ? rs.getDate("fecha_vencimiento") : null;
        producto.setFechaVencimiento(fechaVencimiento == null ? null : fechaVencimiento.toLocalDate());
        producto.setEstado(!hasColumn(rs, "estado") || rs.getBoolean("estado"));
        return producto;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    public void cambiarEstado(Long idProducto, boolean estado) {
        executor.call(DbFunctions.CAMBIAR_ESTADO_PRODUCTO, idProducto, estado);
    }
}
