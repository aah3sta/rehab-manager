package dao;

import database.DatabaseConnection;
import models.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO {

    /** Factory: returns the right subclass for the role (used for polymorphism). */
    public static Staff buildStaff(long id, String name, String contact, String role) {
        if (role == null) role = "";
        switch (role.trim().toLowerCase()) {
            case "doctor":    return new Doctor(id, name, contact);
            case "therapist": return new Therapist(id, name, contact);
            case "nurse":     return new Nurse(id, name, contact);
            default:          return new Staff(id, name, contact, role);
        }
    }

    public List<String> getDutiesReport() throws SQLException {
        List<String> lines = new ArrayList<>();
        for (Staff s : getAll()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream oldOut = System.out;
            try (PrintStream ps = new PrintStream(baos)) {
                System.setOut(ps);
                s.performDuties();
            } finally {
                System.setOut(oldOut);
            }
            String dutyOutput = baos.toString().trim();
            lines.add(String.format("#%d %s (%s) -> %s",
                    s.getId(), s.getFullName(), s.getRole(), dutyOutput));
        }
        return lines;
    }

    public long register(Staff s) throws SQLException {
        String sql = "INSERT INTO STAFF (FULL_NAME, CONTACT, ROLE) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, s.getFullName());
            stmt.setString(2, s.getContact());
            stmt.setString(3, s.getRole());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    s.setId(rs.getLong(1));
                    return s.getId();
                }
            }
        }
        return -1;
    }

    public List<Staff> getAll() throws SQLException {
        List<Staff> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM STAFF ORDER BY STAFF_ID")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Staff findById(long id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM STAFF WHERE STAFF_ID = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * byRoleOnly = true  -> exact role match, case-insensitive ("Display staff by role")
     * byRoleOnly = false -> partial match on name or contact
     */
    public List<Staff> search(String keyword, boolean byRoleOnly) throws SQLException {
        List<Staff> list = new ArrayList<>();
        String sql = byRoleOnly
                ? "SELECT * FROM STAFF WHERE LOWER(ROLE) = LOWER(?) ORDER BY FULL_NAME"
                : "SELECT * FROM STAFF WHERE LOWER(FULL_NAME) LIKE LOWER(?) OR CONTACT LIKE ? ORDER BY FULL_NAME";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (byRoleOnly) {
                stmt.setString(1, keyword.trim());
            } else {
                stmt.setString(1, "%" + keyword.trim() + "%");
                stmt.setString(2, "%" + keyword.trim().replaceAll("[\\s-]", "") + "%");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Distinct roles currently in the table, for the "display by role" prompt. */
    public List<String> getRoles() throws SQLException {
        List<String> roles = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT ROLE FROM STAFF ORDER BY ROLE")) {
            while (rs.next()) roles.add(rs.getString(1));
        }
        return roles;
    }

    public boolean update(Staff s) throws SQLException {
        String sql = "UPDATE STAFF SET FULL_NAME=?, CONTACT=?, ROLE=? WHERE STAFF_ID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, s.getFullName());
            stmt.setString(2, s.getContact());
            stmt.setString(3, s.getRole());
            stmt.setLong(4, s.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM STAFF WHERE STAFF_ID = ?")) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Number of medical assessments this staff member conducted. */
    public int countLinkedRecords(long staffId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MEDICAL_ASSESSMENT WHERE STAFF_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, staffId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Array + runtime polymorphism: each object runs its own performDuties(). */
    public void showAllDuties() throws SQLException {
        Staff[] staffArray = getAll().toArray(new Staff[0]);
        if (staffArray.length == 0) {
            System.out.println("No staff registered.");
            return;
        }
        for (Staff member : staffArray) {
            member.performDuties();
        }
    }

    private Staff map(ResultSet rs) throws SQLException {
        return buildStaff(rs.getLong("STAFF_ID"), rs.getString("FULL_NAME"),
                rs.getString("CONTACT"), rs.getString("ROLE"));
    }
}