package dao;

import database.DatabaseConnection;
import models.MedicalAssessment;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicalAssessmentDAO {

    public long record(MedicalAssessment a) throws SQLException {
        String sql = "INSERT INTO MEDICAL_ASSESSMENT "
                + "(PATIENT_ID, STAFF_ID, ASSESSMENT_DATE, MEDICAL_HISTORY, ASSESSMENT_NOTES, DIAGNOSIS) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        if (a.getAssessmentDate() == null) a.setAssessmentDate(LocalDateTime.now().withNano(0));
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, a.getPatientId());
            stmt.setLong(2, a.getStaffId());
            stmt.setTimestamp(3, Timestamp.valueOf(a.getAssessmentDate()));
            stmt.setString(4, a.getMedicalHistory());
            stmt.setString(5, a.getAssessmentNotes());
            stmt.setString(6, a.getDiagnosis());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setAssessmentId(rs.getLong(1));
                    return a.getAssessmentId();
                }
            }
        }
        return -1;
    }

    public List<MedicalAssessment> search(Long patientId, Long staffId, LocalDate date) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT ASSESSMENT_ID, PATIENT_ID, STAFF_ID, ASSESSMENT_DATE, "
                + "MEDICAL_HISTORY, ASSESSMENT_NOTES, DIAGNOSIS "
                + "FROM MEDICAL_ASSESSMENT WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (patientId != null) {
            sql.append(" AND PATIENT_ID = ?");
            params.add(patientId);
        }
        if (staffId != null) {
            sql.append(" AND STAFF_ID = ?");
            params.add(staffId);
        }
        if (date != null) {
            sql.append(" AND ASSESSMENT_DATE >= ? AND ASSESSMENT_DATE < ?");
            params.add(Timestamp.valueOf(date.atStartOfDay()));
            params.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
        }
        sql.append(" ORDER BY ASSESSMENT_DATE DESC, ASSESSMENT_ID DESC");

        return queryList(sql.toString(), params.toArray());
    }

    public MedicalAssessment findById(long id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM MEDICAL_ASSESSMENT WHERE ASSESSMENT_ID = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<MedicalAssessment> findByPatient(long patientId) throws SQLException {
        return queryList("SELECT * FROM MEDICAL_ASSESSMENT WHERE PATIENT_ID = ? ORDER BY ASSESSMENT_DATE DESC",
                patientId);
    }

    public List<MedicalAssessment> findByStaff(long staffId) throws SQLException {
        return queryList("SELECT * FROM MEDICAL_ASSESSMENT WHERE STAFF_ID = ? ORDER BY ASSESSMENT_DATE DESC",
                staffId);
    }

    /** All assessments on one calendar day. A range keeps the date index usable. */
    public List<MedicalAssessment> findByDate(LocalDate date) throws SQLException {
        return queryList("SELECT * FROM MEDICAL_ASSESSMENT WHERE ASSESSMENT_DATE >= ? AND ASSESSMENT_DATE < ? "
                        + "ORDER BY ASSESSMENT_DATE",
                Timestamp.valueOf(date.atStartOfDay()), Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
    }

    /** Updates history, notes and diagnosis. The model's setters have already validated them. */
    public boolean update(MedicalAssessment a) throws SQLException {
        String sql = "UPDATE MEDICAL_ASSESSMENT SET MEDICAL_HISTORY=?, ASSESSMENT_NOTES=?, DIAGNOSIS=? "
                + "WHERE ASSESSMENT_ID=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getMedicalHistory());
            stmt.setString(2, a.getAssessmentNotes());
            stmt.setString(3, a.getDiagnosis());
            stmt.setLong(4, a.getAssessmentId());
            return stmt.executeUpdate() > 0;
        }
    }

    private List<MedicalAssessment> queryList(String sql, Object... params) throws SQLException {
        List<MedicalAssessment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private MedicalAssessment map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("ASSESSMENT_DATE");
        return new MedicalAssessment(rs.getLong("ASSESSMENT_ID"), rs.getLong("PATIENT_ID"),
                rs.getLong("STAFF_ID"), ts != null ? ts.toLocalDateTime() : null,
                rs.getString("MEDICAL_HISTORY"), rs.getString("ASSESSMENT_NOTES"),
                rs.getString("DIAGNOSIS"));
    }
}