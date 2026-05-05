package gearhub.website.gearhub.util;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    @Getter
    private final Connection connection;
    private String url;
    private String username;
    private String password;

    private DatabaseConnection() {
        loadEnvVariables();
        try {
            this.connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to database", e);
        }
    }

    private void loadEnvVariables() {
        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            props.load(reader);
            this.url = props.getProperty("DB_URL");
            this.username = props.getProperty("DB_USERNAME");
            this.password = props.getProperty("DB_PASSWORD");
        } catch (IOException e) {
            System.err.println("Warning: Could not load .env file. Falling back to system environment variables mapping.");
            this.url = System.getenv("DB_URL");
            this.username = System.getenv("DB_USERNAME");
            this.password = System.getenv("DB_PASSWORD");
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        } else {
            try {
                if (instance.getConnection().isClosed()) {
                    instance = new DatabaseConnection();
                }
            } catch (SQLException e) {
                instance = new DatabaseConnection();
            }
        }
        return instance;
    }

}
