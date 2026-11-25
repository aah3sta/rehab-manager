package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String URL = setting("db.url", "DB_URL",
            "jdbc:mysql://localhost:3306/REHAB_DB?serverTimezone=Africa/Nairobi");
    private static final String USER = setting("db.user", "DB_USER", "root");
    private static final String PASSWORD = setting("db.password", "DB_PASSWORD", "Ngigi@2012");

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Quick check used at start-up so the user gets a clear message instead of a stack trace. */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(3);
        } catch (SQLException e) {
            System.out.println("Could not connect to the database: " + e.getMessage());
            System.out.println("Check that MySQL is running, database/rehab-tables.sql has been run, "
                    + "and the URL/user/password are correct.");
            return false;
        }
    }

    private static String setting(String property, String envVar, String fallback) {
        String value = System.getProperty(property);
        if (value == null) value = System.getenv(envVar);
        return value != null ? value : fallback;
    }
}