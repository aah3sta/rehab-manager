package dao;

import models.TherapySession;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TherapySessionDAO extends BaseDAO<TherapySession> {

    public long record(TherapySession s) throws SQLException {
        long id = insert("INSERT INTO THERAPY_SESSION (PATIENT_ID, STAFF_ID, SESSION_DATE, DURATION_MINUTES, NOTES) "
                        + "VALUES (?, ?, ?, ?, ?)",
                s.getPatientId(), s.getStaffId(), s.getSessionDate(), s.getDurationMinutes(), s.getNotes());
        s.setSessionId(id);
        return id;
    }

    public TherapySession findById(long id) throws SQLException {
        return queryOne("SELECT * FROM THERAPY_SESSION WHERE SESSION_ID = ?", id);
    }

    /** A patient's therapy-session history, newest first. */
    public List<TherapySession> findByPatient(long patientId) throws SQLException {
        return queryList("SELECT * FROM THERAPY_SESSION WHERE PATIENT_ID = ? ORDER BY SESSION_DATE DESC", patientId);
    }

    public List<TherapySession> findByStaff(long staffId) throws SQLException {
        return queryList("SELECT * FROM THERAPY_SESSION WHERE STAFF_ID = ? ORDER BY SESSION_DATE DESC", staffId);
    }

    public List<TherapySession> findByDate(LocalDate date) throws SQLException {
        Object[] r = dayRange(date);
        return queryList("SELECT * FROM THERAPY_SESSION WHERE SESSION_DATE >= ? AND SESSION_DATE < ? "
                + "ORDER BY SESSION_DATE", r[0], r[1]);
    }

    public List<TherapySession> getAll() throws SQLException {
        return queryList("SELECT * FROM THERAPY_SESSION ORDER BY SESSION_DATE DESC");
    }

    /** Total recorded therapy time for a patient, in minutes (0 if none). */
    public long totalMinutes(long patientId) throws SQLException {
        return queryLong("SELECT COALESCE(SUM(DURATION_MINUTES), 0) FROM THERAPY_SESSION WHERE PATIENT_ID = ?",
                patientId);
    }

    @Override
    protected TherapySession map(ResultSet rs) throws SQLException {
        return new TherapySession(rs.getLong("SESSION_ID"), rs.getLong("PATIENT_ID"), rs.getLong("STAFF_ID"),
                toDateTime(rs.getTimestamp("SESSION_DATE")), rs.getInt("DURATION_MINUTES"), rs.getString("NOTES"));
    }
}
