package dao;

import models.Vitals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class VitalsDAO extends BaseDAO<Vitals> {

    public long record(Vitals v) throws SQLException {
        if (v.getRecordedAt() == null) v.setRecordedAt(LocalDateTime.now().withNano(0));
        long id = insert("INSERT INTO VITALS (PATIENT_ID, STAFF_ID, RECORDED_AT, TEMPERATURE, PULSE, "
                        + "BREATHING_RATE, GLUCOSE, SYSTOLIC_BP, DIASTOLIC_BP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                v.getPatientId(), v.getStaffId(), v.getRecordedAt(), v.getTemperature(), v.getPulse(),
                v.getBreathingRate(), v.getGlucose(), v.getSystolic(), v.getDiastolic());
        v.setVitalsId(id);
        return id;
    }

    public Vitals findById(long id) throws SQLException {
        return queryOne("SELECT * FROM VITALS WHERE VITALS_ID = ?", id);
    }

    public List<Vitals> findByPatient(long patientId) throws SQLException {
        return queryList("SELECT * FROM VITALS WHERE PATIENT_ID = ? ORDER BY RECORDED_AT DESC", patientId);
    }

    /** Latest reading for a patient, or null if none. */
    public Vitals findLatest(long patientId) throws SQLException {
        return queryOne("SELECT * FROM VITALS WHERE PATIENT_ID = ? "
                + "ORDER BY RECORDED_AT DESC, VITALS_ID DESC LIMIT 1", patientId);
    }

    public List<Vitals> findByStaff(long staffId) throws SQLException {
        return queryList("SELECT * FROM VITALS WHERE STAFF_ID = ? ORDER BY RECORDED_AT DESC", staffId);
    }

    public List<Vitals> findByDate(LocalDate date) throws SQLException {
        Object[] r = dayRange(date);
        return queryList("SELECT * FROM VITALS WHERE RECORDED_AT >= ? AND RECORDED_AT < ? "
                + "ORDER BY RECORDED_AT", r[0], r[1]);
    }

    public List<Vitals> getAll() throws SQLException {
        return queryList("SELECT * FROM VITALS ORDER BY RECORDED_AT DESC");
    }

    /** Any argument may be null (= no filter). Used by the GUI. */
    public List<Vitals> search(Long patientId, Long staffId, LocalDate date) throws SQLException {
        return filter("VITALS", "RECORDED_AT", patientId, staffId, date);
    }

    @Override
    protected Vitals map(ResultSet rs) throws SQLException {
        return new Vitals(rs.getLong("VITALS_ID"), rs.getLong("PATIENT_ID"), rs.getLong("STAFF_ID"),
                toDateTime(rs.getTimestamp("RECORDED_AT")), rs.getDouble("TEMPERATURE"), rs.getInt("PULSE"),
                rs.getInt("BREATHING_RATE"), rs.getDouble("GLUCOSE"),
                rs.getInt("SYSTOLIC_BP"), rs.getInt("DIASTOLIC_BP"));
    }
}
