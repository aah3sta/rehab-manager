package dao;

import models.MedicationRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MedicationDAO extends BaseDAO<MedicationRecord> {

    public long record(MedicationRecord m) throws SQLException {
        if (m.getRecordedDate() == null) m.setRecordedDate(LocalDateTime.now().withNano(0));
        long id = insert("INSERT INTO MEDICATION_RECORD (PATIENT_ID, STAFF_ID, MEDICATION_NAME, DOSAGE, "
                        + "FREQUENCY, INSTRUCTIONS, RECORDED_DATE) VALUES (?, ?, ?, ?, ?, ?, ?)",
                m.getPatientId(), m.getStaffId(), m.getMedicationName(), m.getDosage(),
                m.getFrequency(), m.getInstructions(), m.getRecordedDate());
        m.setRecordId(id);
        return id;
    }

    public MedicationRecord findById(long id) throws SQLException {
        return queryOne("SELECT * FROM MEDICATION_RECORD WHERE RECORD_ID = ?", id);
    }

    /** A patient's medication history, newest first. */
    public List<MedicationRecord> findByPatient(long patientId) throws SQLException {
        return queryList("SELECT * FROM MEDICATION_RECORD WHERE PATIENT_ID = ? ORDER BY RECORDED_DATE DESC",
                patientId);
    }

    /** Partial, case-insensitive match: "amox" finds "Amoxicillin". */
    public List<MedicationRecord> searchByName(String name) throws SQLException {
        return queryList("SELECT * FROM MEDICATION_RECORD WHERE LOWER(MEDICATION_NAME) LIKE LOWER(?) "
                + "ORDER BY RECORDED_DATE DESC", "%" + name.trim() + "%");
    }

    public List<MedicationRecord> findByStaff(long staffId) throws SQLException {
        return queryList("SELECT * FROM MEDICATION_RECORD WHERE STAFF_ID = ? ORDER BY RECORDED_DATE DESC", staffId);
    }

    public List<MedicationRecord> getAll() throws SQLException {
        return queryList("SELECT * FROM MEDICATION_RECORD ORDER BY RECORDED_DATE DESC");
    }

    /** Update medication details or instructions (authorization is checked by the caller). */
    public boolean update(MedicationRecord m) throws SQLException {
        return execute("UPDATE MEDICATION_RECORD SET MEDICATION_NAME = ?, DOSAGE = ?, FREQUENCY = ?, "
                        + "INSTRUCTIONS = ? WHERE RECORD_ID = ?",
                m.getMedicationName(), m.getDosage(), m.getFrequency(), m.getInstructions(), m.getRecordId());
    }

    /** Any argument may be null/empty (= no filter). Used by the GUI. */
    public List<MedicationRecord> search(Long patientId, Long staffId, String name) throws SQLException {
        List<MedicationRecord> list = filter("MEDICATION_RECORD", "RECORDED_DATE", patientId, staffId, null);
        if (name == null || name.trim().isEmpty()) return list;
        String needle = name.trim().toLowerCase();
        list.removeIf(m -> !m.getMedicationName().toLowerCase().contains(needle));
        return list;
    }

    @Override
    protected MedicationRecord map(ResultSet rs) throws SQLException {
        return new MedicationRecord(rs.getLong("RECORD_ID"), rs.getLong("PATIENT_ID"), rs.getLong("STAFF_ID"),
                rs.getString("MEDICATION_NAME"), rs.getString("DOSAGE"), rs.getString("FREQUENCY"),
                rs.getString("INSTRUCTIONS"), toDateTime(rs.getTimestamp("RECORDED_DATE")));
    }
}
