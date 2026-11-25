package dao;

import models.LabTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class LabTestDAO extends BaseDAO<LabTest> {

    /** Record a test under an existing assessment (the FK rejects unknown assessments). */
    public long record(LabTest t) throws SQLException {
        if (t.getTestDate() == null) t.setTestDate(LocalDateTime.now().withNano(0));
        long id = insert("INSERT INTO LAB_TESTS (ASSESSMENT_ID, TEST_NAME, TEST_DATE, RESULT, NOTES) "
                        + "VALUES (?, ?, ?, ?, ?)",
                t.getAssessmentId(), t.getTestName(), t.getTestDate(), t.getResult(), t.getNotes());
        t.setTestId(id);
        return id;
    }

    public LabTest findById(long id) throws SQLException {
        return queryOne("SELECT * FROM LAB_TESTS WHERE TEST_ID = ?", id);
    }

    public List<LabTest> findByAssessment(long assessmentId) throws SQLException {
        return queryList("SELECT * FROM LAB_TESTS WHERE ASSESSMENT_ID = ? ORDER BY TEST_DATE DESC", assessmentId);
    }

    /** A patient's tests, reached through their assessments. */
    public List<LabTest> findByPatient(long patientId) throws SQLException {
        return queryList("SELECT t.* FROM LAB_TESTS t "
                        + "JOIN MEDICAL_ASSESSMENT a ON a.ASSESSMENT_ID = t.ASSESSMENT_ID "
                        + "WHERE a.PATIENT_ID = ? ORDER BY t.TEST_DATE DESC", patientId);
    }

    public List<LabTest> getAll() throws SQLException {
        return queryList("SELECT * FROM LAB_TESTS ORDER BY TEST_DATE DESC");
    }

    /** Update results or notes (authorization is checked by the caller). */
    public boolean update(LabTest t) throws SQLException {
        return execute("UPDATE LAB_TESTS SET TEST_NAME = ?, RESULT = ?, NOTES = ? WHERE TEST_ID = ?",
                t.getTestName(), t.getResult(), t.getNotes(), t.getTestId());
    }

    /** The staff member who owns a test = whoever conducted its assessment. */
    public long findOwnerStaffId(long testId) throws SQLException {
        return queryLong("SELECT a.STAFF_ID FROM LAB_TESTS t "
                + "JOIN MEDICAL_ASSESSMENT a ON a.ASSESSMENT_ID = t.ASSESSMENT_ID WHERE t.TEST_ID = ?", testId);
    }

    @Override
    protected LabTest map(ResultSet rs) throws SQLException {
        return new LabTest(rs.getLong("TEST_ID"), rs.getLong("ASSESSMENT_ID"), rs.getString("TEST_NAME"),
                toDateTime(rs.getTimestamp("TEST_DATE")), rs.getString("RESULT"), rs.getString("NOTES"));
    }
}
