package com.vdch.fiado.repository;

import com.vdch.fiado.dto.FiadoResumen;
import com.vdch.shared.constants.DbFunctions;
import com.vdch.shared.util.StoredProcedureExecutor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
                           cr.monto_total,
                           cr.saldo_pendiente,
                           0 AS interes_generado,
                           0 AS dias_vencidos,
                           cr.estado,
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
            fiado.setInteresGenerado(hasColumn(rs, "interes_generado") ? rs.getBigDecimal("interes_generado") : BigDecimal.ZERO);
            fiado.setDiasVencidos(hasColumn(rs, "dias_vencidos") ? rs.getInt("dias_vencidos") : 0);
            fiado.setEstado(rs.getString("estado"));
            fiado.setFechaCredito(rs.getTimestamp("fecha_credito").toLocalDateTime());
            fiado.setFechaLimite(rs.getDate("fecha_limite") == null ? null : rs.getDate("fecha_limite").toLocalDate());
            if (fiado.getInteresGenerado() == null || fiado.getInteresGenerado().compareTo(BigDecimal.ZERO) == 0) {
                applyLocalInterestFallback(fiado);
            }
            return fiado;
    }

    private void applyLocalInterestFallback(FiadoResumen fiado) {
        if (fiado.getFechaCredito() == null || fiado.getSaldoPendiente() == null) {
            return;
        }
        LocalDate start = fiado.getFechaLimite() == null ? fiado.getFechaCredito().toLocalDate().plusDays(7) : fiado.getFechaLimite();
        long days = ChronoUnit.DAYS.between(start, LocalDate.now());
        if (days <= 0) {
            return;
        }
        BigDecimal weeks = BigDecimal.valueOf(Math.ceil(days / 7.0));
        BigDecimal interest = fiado.getSaldoPendiente()
                .multiply(new BigDecimal("0.02"))
                .multiply(weeks)
                .setScale(2, RoundingMode.HALF_UP);
        fiado.setInteresGenerado(interest);
        fiado.setDiasVencidos((int) days);
        fiado.setSaldoPendiente(fiado.getSaldoPendiente().add(interest));
        fiado.setEstado("VENCIDO");
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws java.sql.SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
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
