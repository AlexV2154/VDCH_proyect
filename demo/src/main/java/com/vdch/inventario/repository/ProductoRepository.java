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
        if (producto.getIdProducto() == null) {
            return executor.jdbcTemplate().queryForObject("""
                    INSERT INTO productos (
                        id_categoria, nombre, descripcion, tipo_venta, unidad_base, equivalencia,
                        precio_compra, precio_venta, stock_actual, stock_minimo, lote, fecha_vencimiento
                    ) VALUES (?, ?, ?, UPPER(COALESCE(?, 'UNIDAD')), UPPER(COALESCE(?, 'UNIDAD')), COALESCE(?, 1),
                              COALESCE(?, 0), COALESCE(?, 0), COALESCE(?, 0), COALESCE(?, 0), ?, ?)
                    RETURNING id_producto
                    """, Long.class,
                    producto.getIdCategoria(), producto.getNombre(), producto.getDescripcion(), producto.getTipoVenta(), producto.getUnidadBase(),
                    producto.getEquivalencia(), producto.getPrecioCompra(), producto.getPrecioVenta(), producto.getStockActual(), producto.getStockMinimo(),
                    producto.getLote(), fechaVencimiento);
        }
        return executor.jdbcTemplate().queryForObject("""
                UPDATE productos
                SET id_categoria = ?, nombre = ?, descripcion = ?, tipo_venta = UPPER(COALESCE(?, 'UNIDAD')),
                    unidad_base = UPPER(COALESCE(?, 'UNIDAD')), equivalencia = COALESCE(?, 1),
                    precio_compra = COALESCE(?, 0), precio_venta = COALESCE(?, 0), stock_actual = COALESCE(?, 0),
                    stock_minimo = COALESCE(?, 0), lote = ?, fecha_vencimiento = ?
                WHERE id_producto = ?
                RETURNING id_producto
                """, Long.class,
                producto.getIdCategoria(), producto.getNombre(), producto.getDescripcion(), producto.getTipoVenta(), producto.getUnidadBase(),
                producto.getEquivalencia(), producto.getPrecioCompra(), producto.getPrecioVenta(), producto.getStockActual(), producto.getStockMinimo(),
                producto.getLote(), fechaVencimiento, producto.getIdProducto());
    }
    public List<Producto> buscar(String texto) {
        return buscarDirecto(texto);
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
                    WHERE COALESCE(estado, TRUE) = TRUE
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
        String baseSql = """
                SELECT id_producto, id_categoria, nombre, descripcion, tipo_venta, unidad_base,
                       equivalencia, precio_compra, precio_venta, stock_actual, stock_minimo, lote, fecha_vencimiento, estado
                FROM productos
                WHERE COALESCE(estado, TRUE) = TRUE
                """;
        if (texto == null || texto.isBlank()) {
            return executor.querySql(baseSql + " ORDER BY nombre", (rs, rowNum) -> mapProducto(rs));
        }
        String filter = "%" + texto.trim() + "%";
        return executor.querySql(baseSql + """
                  AND (nombre ILIKE ? OR COALESCE(descripcion, '') ILIKE ?)
                ORDER BY nombre
                """, (rs, rowNum) -> mapProducto(rs), filter, filter);
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
        producto.setLote(hasColumn(rs, "lote") ? rs.getString("lote") : null);
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
