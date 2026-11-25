package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Discharge and aftercare plan for a patient. */
public class DischargePlan {
    private long planId;
    private long patientId;
    private String recoveryGoals;
    private String aftercareInstructions;
    private LocalDate dischargeDate;   // null until discharged
    private LocalDate followUpDate;    // null until scheduled
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDateTime createdDate;

    public DischargePlan(long planId, long patientId, String recoveryGoals, String aftercareInstructions,
                         LocalDate dischargeDate, LocalDate followUpDate,
                         String emergencyContactName, String emergencyContactPhone,
                         LocalDateTime createdDate) {
        if (patientId <= 0) throw new IllegalArgumentException("A valid patient ID is required.");
        this.planId = planId;
        this.patientId = patientId;
        setRecoveryGoals(recoveryGoals);
        setAftercareInstructions(aftercareInstructions);
        setDates(dischargeDate, followUpDate);
        setEmergencyContact(emergencyContactName, emergencyContactPhone);
        this.createdDate = createdDate;
    }

    public long getPlanId() { return planId; }
    public void setPlanId(long id) { this.planId = id; }
    public long getPatientId() { return patientId; }
    public LocalDateTime getCreatedDate() { return createdDate; }

    public String getRecoveryGoals() { return recoveryGoals; }
    public void setRecoveryGoals(String goals) {
        if (goals == null || goals.trim().isEmpty()) {
            throw new IllegalArgumentException("Recovery goals cannot be empty.");
        }
        this.recoveryGoals = goals.trim();
    }

    public String getAftercareInstructions() { return aftercareInstructions; }
    public void setAftercareInstructions(String instructions) {
        if (instructions == null || instructions.trim().isEmpty()) {
            throw new IllegalArgumentException("Aftercare instructions cannot be empty.");
        }
        this.aftercareInstructions = instructions.trim();
    }

    public LocalDate getDischargeDate() { return dischargeDate; }
    public LocalDate getFollowUpDate() { return followUpDate; }

    /** Both dates are optional, but a follow-up can't come before discharge. */
    public void setDates(LocalDate discharge, LocalDate followUp) {
        if (discharge != null && discharge.isBefore(LocalDate.now().minusYears(5))) {
            throw new IllegalArgumentException("Discharge date is too far in the past.");
        }
        if (discharge != null && followUp != null && followUp.isBefore(discharge)) {
            throw new IllegalArgumentException("Follow-up date cannot be before the discharge date.");
        }
        this.dischargeDate = discharge;
        this.followUpDate = followUp;
    }

    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }

    /** Uses the same name and phone rules as Person. */
    public void setEmergencyContact(String name, String phone) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Emergency contact name cannot be empty.");
        }
        if (!name.trim().matches("[A-Za-z][A-Za-z .'-]*") || name.trim().length() > 100) {
            throw new IllegalArgumentException("Emergency contact name may only contain letters, spaces, apostrophes, dots and hyphens.");
        }
        if (phone == null) throw new IllegalArgumentException("Emergency contact phone cannot be empty.");
        String cleaned = phone.trim().replaceAll("[\\s-]", "");
        if (!cleaned.matches("\\+?\\d{10,15}")) {
            throw new IllegalArgumentException("Emergency contact phone must be 10 to 15 digits.");
        }
        this.emergencyContactName = name.trim();
        this.emergencyContactPhone = cleaned;
    }

    public boolean isDischarged() {
        return dischargeDate != null && !dischargeDate.isAfter(LocalDate.now());
    }

    public String getStatus() {
        if (dischargeDate == null) return "In treatment";
        return isDischarged() ? "Discharged" : "Discharge planned";
    }

    public String getSummary() {
        return String.format("Plan #%d | Patient #%d | %s%n"
                        + "   Discharge: %s | Follow-up: %s%n"
                        + "   Recovery goals: %s%n   Aftercare: %s%n"
                        + "   Emergency relapse contact: %s, %s",
                planId, patientId, getStatus(),
                dischargeDate != null ? dischargeDate : "not set",
                followUpDate != null ? followUpDate : "not set",
                recoveryGoals, aftercareInstructions, emergencyContactName, emergencyContactPhone);
    }

    @Override
    public String toString() { return getSummary(); }
}
