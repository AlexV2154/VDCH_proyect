package com.vdch.cliente.repository;

import com.vdch.cliente.dto.ClienteResumen;
import com.vdch.cliente.model.Cliente;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.util.List;

public class ClienteRepository {
    private final StoredProcedureExecutor executor;

    public ClienteRepository() {
        this(new StoredProcedureExecutor());
    }

    public ClienteRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public Long guardar(Cliente cliente) {
        return executor.callForLong(
                DbFunctions.GUARDAR_CLIENTE,
                cliente.getIdCliente(),
                cliente.getNombres(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getNotas()
        );
    }

    public List<ClienteResumen> buscar(String texto) {
        try {
            return executor.query(DbFunctions.BUSCAR_CLIENTES, (rs, rowNum) -> {
                ClienteResumen cliente = new ClienteResumen();
                cliente.setIdCliente(rs.getLong("id_cliente"));
                cliente.setNombres(rs.getString("nombres"));
                cliente.setTelefono(rs.getString("telefono"));
                cliente.setDireccion(rs.getString("direccion"));
                cliente.setNotas(rs.getString("notas"));
                cliente.setSaldoFiado(rs.getBigDecimal("saldo_fiado"));
                return cliente;
            }, texto);
        } catch (RuntimeException ex) {
            String filter = texto == null || texto.isBlank() ? null : "%" + texto + "%";
            return executor.querySql("""
                    SELECT c.id_cliente, c.nombres, c.telefono, c.direccion, c.notas,
                           COALESCE(SUM(
                               cr.saldo_pendiente
                               + CASE
                                   WHEN CURRENT_DATE > GREATEST(
                                       COALESCE(cr.fecha_limite, (cr.fecha_credito::date + INTERVAL '7 days')::date),
                                       (cr.fecha_credito::date + INTERVAL '7 days')::date
                                   )
                                   THEN ROUND(
                                       cr.saldo_pendiente
                                       * 0.02
                                       * CEIL((CURRENT_DATE - GREATEST(
                                           COALESCE(cr.fecha_limite, (cr.fecha_credito::date + INTERVAL '7 days')::date),
                                           (cr.fecha_credito::date + INTERVAL '7 days')::date
                                       )) / 7.0),
                                       2
                                   )
                                   ELSE 0
                               END
                           ) FILTER (WHERE cr.estado IN ('PENDIENTE', 'VENCIDO')), 0) AS saldo_fiado
                    FROM clientes c
                    LEFT JOIN creditos cr ON cr.id_cliente = c.id_cliente
                    WHERE c.estado = TRUE
                      AND (? IS NULL OR c.nombres ILIKE ? OR c.telefono ILIKE ?)
                    GROUP BY c.id_cliente
                    ORDER BY c.nombres
                    """, (rs, rowNum) -> {
            ClienteResumen cliente = new ClienteResumen();
            cliente.setIdCliente(rs.getLong("id_cliente"));
            cliente.setNombres(rs.getString("nombres"));
            cliente.setTelefono(rs.getString("telefono"));
            cliente.setDireccion(rs.getString("direccion"));
            cliente.setNotas(rs.getString("notas"));
            cliente.setSaldoFiado(rs.getBigDecimal("saldo_fiado"));
            return cliente;
            }, filter, filter, filter);
        }
    }

    public void cambiarEstado(Long idCliente, boolean estado) {
        executor.call(DbFunctions.CAMBIAR_ESTADO_CLIENTE, idCliente, estado);
    }
}
