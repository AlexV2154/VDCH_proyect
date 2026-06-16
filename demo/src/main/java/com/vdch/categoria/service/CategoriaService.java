package com.vdch.categoria.service;

import com.vdch.categoria.model.Categoria;
import com.vdch.categoria.repository.CategoriaRepository;
import java.util.List;

public class CategoriaService {
    private final CategoriaRepository categoriaRepository;

    public CategoriaService() {
        this(new CategoriaRepository());
    }

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public Long guardarCategoria(Categoria categoria) {
        return categoriaRepository.guardar(categoria);
    }

    public List<Categoria> listarCategoriasActivas() {
        return categoriaRepository.listarActivas();
    }

    public void cambiarEstadoCategoria(Long idCategoria, boolean estado) {
        categoriaRepository.cambiarEstado(idCategoria, estado);
    }
}
