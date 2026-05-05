package com.project.artconnect.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized JDBC configuration.
 *
 * Values are resolved in this order:
 * 1. JVM system properties
 * 2. Environment variables
 * 3. src/main/resources/database.properties
 * 4. Hardcoded defaults
 */
public final class DatabaseConfig {
    private static final Properties PROPERTIES = loadProperties();

    public static final boolean JDBC_ENABLED = Boolean.parseBoolean(
            resolve("db.enabled", "ARTCONNECT_DB_ENABLED", "true"));
    public static final String URL = resolve(
            "db.url",
            "ARTCONNECT_DB_URL",
            "jdbc:mysql://localhost:3306/artconnect_db?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");
    public static final String USER = resolve("db.user", "ARTCONNECT_DB_USER", "root");
    public static final String PASSWORD = resolve("db.password", "ARTCONNECT_DB_PASSWORD", "password");

    private DatabaseConfig() {
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load database.properties.", e);
        }
        return properties;
    }

    private static String resolve(String propertyKey, String envKey, String defaultValue) {
        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String fileValue = PROPERTIES.getProperty(propertyKey);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue;
        }

        return defaultValue;
    }
}
