package com.example.config;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {

    private static DbConnection instance;

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private DbConnection() {
        loadDriver();

        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Format Render: postgres://user:pass@host:port/dbname
            try {
                URI dbUri = new URI(databaseUrl);
                String[] userInfo = dbUri.getUserInfo().split(":", 2);
                this.username = userInfo[0];
                this.password = userInfo.length > 1 ? userInfo[1] : "";
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                String dbName = dbUri.getPath().substring(1);
                this.jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require", host, port, dbName);
            } catch (Exception e) {
                throw new RuntimeException("Erreur parsing DATABASE_URL", e);
            }
        } else {
            // Format local ou JDBC direct
            this.jdbcUrl = getEnvOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/carlocation");
            this.username = getEnvOrDefault("DATABASE_USERNAME", "postgres");
            this.password = getEnvOrDefault("DATABASE_PASSWORD", "postgres");
        }
    }

    public static synchronized DbConnection getInstance() {
        if (instance == null) {
            instance = new DbConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL Driver non trouvé", e);
        }
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}