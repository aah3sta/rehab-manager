package dao;

import database.DatabaseConnection;
import models.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public long register(Patient p) throws SQLException {
        String sql = "INSERT INTO PATIENT (FULL_NAME, GENDER, DATE_OF_BIRTH, CONTACT) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.getFullName());
            stmt.setString(2, p.getGender());
            stmt.setDate(3, Date.valueOf(p.getDateOfBirth()));
            stmt.setString(4, p.getContact());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getLong(1));
                    return p.getId();
                }
            }
        }
        return -1;
    }

    public List<Patient> getAll() throws SQLException {
        List<Patient> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM PATIENT ORDER BY PATIENT_ID")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Patient findById(long id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM PATIENT WHERE PATIENT_ID = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Search by (partial) name or contact. Use findById for ID searches. */
    public List<Patient> search(String keyword) throws SQLException {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM PATIENT WHERE LOWER(FULL_NAME) LIKE LOWER(?) OR CONTACT LIKE ? ORDER BY FULL_NAME";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String q = "%" + keyword.trim() + "%";
            stmt.setString(1, q);
            stmt.setString(2, "%" + keyword.trim().replaceAll("[\\s-]", "") + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean update(Patient p) throws SQLException {
        String sql = "UPDATE PATIENT SET FULL_NAME=?, GENDER=?, DATE_OF_BIRTH=?, CONTACT=? WHERE PATIENT_ID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getFullName());
            stmt.setString(2, p.getGender());
            stmt.setDate(3, Date.valueOf(p.getDateOfBirth()));
            stmt.setString(4, p.getContact());
            stmt.setLong(5, p.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * "Delete when permitted": a patient with linked assessments cannot be deleted.
     * Callers should check hasLinkedRecords() first to show a friendly message;
     * the foreign key (ON DELETE RESTRICT) is the safety net.
     */
    public boolean delete(long id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM PATIENT WHERE PATIENT_ID = ?")) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Number of medical assessments linked to this patient. */
    public int countLinkedRecords(long patientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MEDICAL_ASSESSMENT WHERE PATIENT_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Patient map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("REGISTRATION_DATE");
        return new Patient(rs.getLong("PATIENT_ID"), rs.getString("FULL_NAME"),
                rs.getString("GENDER"), rs.getDate("DATE_OF_BIRTH").toLocalDate(),
                rs.getString("CONTACT"), ts != null ? ts.toLocalDateTime() : null);
    }
}
