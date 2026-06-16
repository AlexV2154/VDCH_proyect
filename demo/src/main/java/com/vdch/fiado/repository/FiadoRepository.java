package com.vdch.fiado.repository;

import com.vdch.fiado.dto.FiadoResumen;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.math.BigDecimal;
import java.util.List;

public class FiadoRepository {
    private final StoredProcedureExecutor executor;

    public FiadoRepository() {
        this(new StoredProcedureExecutor());
    }

    public FiadoRepository(StoredProcedureExecutor executor) {
        this.executor = executor;
    }

    public List<FiadoResumen> listarPendientes() {
        try {
            return executor.query(DbFunctions.LISTAR_FIADOS, (rs, rowNum) -> mapFiado(rs));
        } catch (RuntimeException ex) {
            return executor.querySql("""
                    SELECT cr.id_credito, c.id_cliente, c.nombres AS cliente, c.telefono,
                           cr.monto_total, cr.saldo_pendiente, cr.estado,
                           cr.fecha_credito, cr.fecha_limite
                    FROM creditos cr
                    JOIN clientes c ON c.id_cliente = cr.id_cliente
                    WHERE cr.estado IN ('PENDIENTE', 'VENCIDO')
                    ORDER BY cr.fecha_credito DESC
                    """, (rs, rowNum) -> mapFiado(rs));
        }
    }

    private FiadoResumen mapFiado(java.sql.ResultSet rs) throws java.sql.SQLException {
            FiadoResumen fiado = new FiadoResumen();
            fiado.setIdCredito(rs.getLong("id_credito"));
            fiado.setIdCliente(rs.getLong("id_cliente"));
            fiado.setCliente(rs.getString("cliente"));
            fiado.setTelefono(rs.getString("telefono"));
            fiado.setMontoTotal(rs.getBigDecimal("monto_total"));
            fiado.setSaldoPendiente(rs.getBigDecimal("saldo_pendiente"));
            fiado.setEstado(rs.getString("estado"));
            fiado.setFechaCredito(rs.getTimestamp("fecha_credito").toLocalDateTime());
            fiado.setFechaLimite(rs.getDate("fecha_limite") == null ? null : rs.getDate("fecha_limite").toLocalDate());
            return fiado;
    }

    public Long registrarPago(Long idCredito, BigDecimal montoPagado, String metodoPago, String observacion) {
        return executor.callForLong(
                DbFunctions.REGISTRAR_PAGO_CREDITO,
                idCredito,
                montoPagado,
                metodoPago,
                observacion
        );
    }
}
