package com.vdch.inventario.service;

import com.vdch.inventario.dto.ProductoStockBajo;
import com.vdch.inventario.model.Producto;
import com.vdch.inventario.repository.ProductoRepository;
import java.util.List;

public class ProductoService {
    private final ProductoRepository productoRepository;

    public ProductoService() {
        this(new ProductoRepository());
    }

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Long guardarProducto(Producto producto) {
        return productoRepository.guardar(producto);
    }

    public List<Producto> buscarProductos(String texto) {
        return productoRepository.buscar(texto);
    }

    public List<ProductoStockBajo> listarProductosStockBajo() {
        return productoRepository.listarStockBajo();
    }

    public void cambiarEstadoProducto(Long idProducto, boolean estado) {
        productoRepository.cambiarEstado(idProducto, estado);
    }
}
