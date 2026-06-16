package com.vdch.config;

public class AppConfig {
    public static final String DB_HOST = getEnv("VDCH_DB_HOST", "144.91.90.133");
    public static final String DB_PORT = getEnv("VDCH_DB_PORT", "3307");
    public static final String DB_NAME = getEnv("VDCH_DB_NAME", "oniamizoproyect");
    public static final String DB_USER = getEnv("VDCH_DB_USER", "postgres");
    public static final String DB_PASSWORD = getEnv("VDCH_DB_PASSWORD", "Mario14y15.");

    private AppConfig() {
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
