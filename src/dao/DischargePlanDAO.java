package dao;

import models.DischargePlan;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DischargePlanDAO extends BaseDAO<DischargePlan> {

    public long create(DischargePlan p) throws SQLException {
        long id = insert("INSERT INTO DISCHARGE_AFTERCARE_PLAN (PATIENT_ID, RECOVERY_GOALS, AFTERCARE_INSTRUCTIONS, "
                        + "DISCHARGE_DATE, FOLLOW_UP_DATE, EMERGENCY_CONTACT_NAME, EMERGENCY_CONTACT_PHONE) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                p.getPatientId(), p.getRecoveryGoals(), p.getAftercareInstructions(),
                p.getDischargeDate(), p.getFollowUpDate(),
                p.getEmergencyContactName(), p.getEmergencyContactPhone());
        p.setPlanId(id);
        return id;
    }

    public DischargePlan findById(long id) throws SQLException {
        return queryOne("SELECT * FROM DISCHARGE_AFTERCARE_PLAN WHERE PLAN_ID = ?", id);
    }

    /** A patient can have more than one plan (e.g. after readmission); newest first. */
    public List<DischargePlan> findByPatient(long patientId) throws SQLException {
        return queryList("SELECT * FROM DISCHARGE_AFTERCARE_PLAN WHERE PATIENT_ID = ? "
                + "ORDER BY CREATED_DATE DESC, PLAN_ID DESC", patientId);
    }

    public List<DischargePlan> getAll() throws SQLException {
        return queryList("SELECT * FROM DISCHARGE_AFTERCARE_PLAN ORDER BY PLAN_ID DESC");
    }

    /** Follow-ups from today onwards, soonest first. */
    public List<DischargePlan> findUpcomingFollowUps() throws SQLException {
        return queryList("SELECT * FROM DISCHARGE_AFTERCARE_PLAN WHERE FOLLOW_UP_DATE >= ? "
                + "ORDER BY FOLLOW_UP_DATE", LocalDate.now());
    }

    /** Recovery goals, aftercare instructions and emergency contact. */
    public boolean updateCarePlan(DischargePlan p) throws SQLException {
        return execute("UPDATE DISCHARGE_AFTERCARE_PLAN SET RECOVERY_GOALS = ?, AFTERCARE_INSTRUCTIONS = ?, "
                        + "EMERGENCY_CONTACT_NAME = ?, EMERGENCY_CONTACT_PHONE = ? WHERE PLAN_ID = ?",
                p.getRecoveryGoals(), p.getAftercareInstructions(),
                p.getEmergencyContactName(), p.getEmergencyContactPhone(), p.getPlanId());
    }

    /** Discharge date and follow-up date. */
    public boolean updateDates(DischargePlan p) throws SQLException {
        return execute("UPDATE DISCHARGE_AFTERCARE_PLAN SET DISCHARGE_DATE = ?, FOLLOW_UP_DATE = ? WHERE PLAN_ID = ?",
                p.getDischargeDate(), p.getFollowUpDate(), p.getPlanId());
    }

    @Override
    protected DischargePlan map(ResultSet rs) throws SQLException {
        return new DischargePlan(rs.getLong("PLAN_ID"), rs.getLong("PATIENT_ID"),
                rs.getString("RECOVERY_GOALS"), rs.getString("AFTERCARE_INSTRUCTIONS"),
                toDate(rs.getDate("DISCHARGE_DATE")), toDate(rs.getDate("FOLLOW_UP_DATE")),
                rs.getString("EMERGENCY_CONTACT_NAME"), rs.getString("EMERGENCY_CONTACT_PHONE"),
                toDateTime(rs.getTimestamp("CREATED_DATE")));
    }
}
