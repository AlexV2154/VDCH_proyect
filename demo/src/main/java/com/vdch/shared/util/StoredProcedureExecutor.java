package com.vdch.shared.util;

import com.vdch.config.DatabaseConfig;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class StoredProcedureExecutor {
    private final JdbcTemplate jdbcTemplate;

    public StoredProcedureExecutor() {
        this(DatabaseConfig.jdbcTemplate());
    }

    public StoredProcedureExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long callForLong(String functionName, Object... args) {
        return jdbcTemplate.queryForObject(select(functionName, args.length), Long.class, args);
    }

    public void call(String functionName, Object... args) {
        jdbcTemplate.queryForObject(select(functionName, args.length), Object.class, args);
    }

    public <T> List<T> query(String functionName, RowMapper<T> rowMapper, Object... args) {
        return jdbcTemplate.query(selectTable(functionName, args.length), rowMapper, args);
    }

    public <T> List<T> querySql(String sql, RowMapper<T> rowMapper, Object... args) {
        return jdbcTemplate.query(sql, rowMapper, args);
    }

    public <T> T queryForObjectSql(String sql, RowMapper<T> rowMapper, Object... args) {
        return jdbcTemplate.queryForObject(sql, rowMapper, args);
    }

    public JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    private String select(String functionName, int argumentCount) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(functionName).append("(");
        for (int i = 0; i < argumentCount; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    private String selectTable(String functionName, int argumentCount) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(functionName).append("(");
        for (int i = 0; i < argumentCount; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }
}
