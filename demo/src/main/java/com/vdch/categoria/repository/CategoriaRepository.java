package com.vdch.categoria.repository;

import com.vdch.categoria.model.Categoria;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.util.List;

public class CategoriaRepository {
    private final StoredProcedureExecutor executor;

    public CategoriaRepository() {
        this(new StoredProcedureExecutor());
    }

    public CategoriaRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public Long guardar(Categoria categoria) {
        return executor.callForLong(
                DbFunctions.GUARDAR_CATEGORIA,
                categoria.getIdCategoria(),
                categoria.getNombre(),
                categoria.getDescripcion()
        );
    }

    public List<Categoria> listarActivas() {
        try {
            return executor.query(DbFunctions.LISTAR_CATEGORIAS, (rs, rowNum) -> {
                Categoria categoria = new Categoria();
                categoria.setIdCategoria(rs.getLong("id_categoria"));
                categoria.setNombre(rs.getString("nombre"));
                categoria.setDescripcion(rs.getString("descripcion"));
                categoria.setEstado(rs.getBoolean("estado"));
                return categoria;
            });
        } catch (RuntimeException ex) {
            return executor.querySql("""
                    SELECT id_categoria, nombre, descripcion, estado
                    FROM categorias
                    WHERE estado = TRUE
                    ORDER BY nombre
                    """, (rs, rowNum) -> {
            Categoria categoria = new Categoria();
            categoria.setIdCategoria(rs.getLong("id_categoria"));
            categoria.setNombre(rs.getString("nombre"));
            categoria.setDescripcion(rs.getString("descripcion"));
            categoria.setEstado(rs.getBoolean("estado"));
            return categoria;
            });
        }
    }

    public void cambiarEstado(Long idCategoria, boolean estado) {
        executor.call(DbFunctions.CAMBIAR_ESTADO_CATEGORIA, idCategoria, estado);
    }
}
