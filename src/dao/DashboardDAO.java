package dao;

import database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;

/** Read-only counts for the dashboard home screen. */
public class DashboardDAO {

    public int countPatients() throws SQLException {
        return count("SELECT COUNT(*) FROM PATIENT");
    }

    public int countStaff() throws SQLException {
        return count("SELECT COUNT(*) FROM STAFF");
    }

    public int countAssessments() throws SQLException {
        return count("SELECT COUNT(*) FROM MEDICAL_ASSESSMENT");
    }

    public int countAssessmentsOn(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MEDICAL_ASSESSMENT WHERE ASSESSMENT_DATE >= ? AND ASSESSMENT_DATE < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(date.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
