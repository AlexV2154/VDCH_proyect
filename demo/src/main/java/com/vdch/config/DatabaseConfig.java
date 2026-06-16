package com.vdch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DatabaseConfig {
    private static final String URL = String.format(
            "jdbc:postgresql://%s:%s/%s?connectTimeout=3&socketTimeout=10",
            AppConfig.DB_HOST,
            AppConfig.DB_PORT,
            AppConfig.DB_NAME
    );
    private static final DataSource DATA_SOURCE = createDataSource();
    private static final JdbcTemplate JDBC_TEMPLATE = new JdbcTemplate(DATA_SOURCE);

    private DatabaseConfig() {
    }

    public static DataSource dataSource() {
        return DATA_SOURCE;
    }

    public static JdbcTemplate jdbcTemplate() {
        return JDBC_TEMPLATE;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            System.err.println("No se pudo conectar a la base de datos: " + e.getMessage());
            return false;
        }
    }

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(URL);
        config.setUsername(AppConfig.DB_USER);
        config.setPassword(AppConfig.DB_PASSWORD);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(3000);
        config.setValidationTimeout(2000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        config.setPoolName("vdch-db-pool");
        return new HikariDataSource(config);
    }
}
