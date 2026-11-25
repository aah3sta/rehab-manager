package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MedicationRecord {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private long recordId;
    private long patientId;
    private long staffId;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String instructions;
    private LocalDateTime recordedDate;

    public MedicationRecord(long recordId, long patientId, long staffId, String medicationName,
                            String dosage, String frequency, String instructions,
                            LocalDateTime recordedDate) {
        if (patientId <= 0) throw new IllegalArgumentException("A valid patient ID is required.");
        if (staffId <= 0) throw new IllegalArgumentException("A valid staff ID is required.");
        this.recordId = recordId;
        this.patientId = patientId;
        this.staffId = staffId;
        setMedicationName(medicationName);
        setDosage(dosage);
        setFrequency(frequency);
        setInstructions(instructions);
        this.recordedDate = recordedDate;
    }

    public long getRecordId() { return recordId; }
    public void setRecordId(long id) { this.recordId = id; }
    public long getPatientId() { return patientId; }
    public long getStaffId() { return staffId; }
    public LocalDateTime getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDateTime d) { this.recordedDate = d; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String name) {
        this.medicationName = required(name, "Medication name", 100);
    }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) {
        String d = required(dosage, "Dosage", 50);
        // A dose needs an amount, e.g. "500 mg", "10 ml", "2 tablets"
        if (!d.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Dosage must include an amount, e.g. 500 mg or 2 tablets.");
        }
        this.dosage = d;
    }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) {
        this.frequency = required(frequency, "Frequency", 50);
    }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) {
        this.instructions = (instructions == null || instructions.trim().isEmpty()) ? null : instructions.trim();
    }

    private static String required(String value, String field, int max) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        if (value.trim().length() > max) {
            throw new IllegalArgumentException(field + " cannot be longer than " + max + " characters.");
        }
        return value.trim();
    }

    public String getSummary() {
        return String.format("Medication #%d | Patient #%d | Staff #%d | %s%n"
                        + "   %s %s, %s%n   Instructions: %s",
                recordId, patientId, staffId, recordedDate != null ? recordedDate.format(FMT) : "-",
                medicationName, dosage, frequency, instructions != null ? instructions : "-");
    }

    @Override
    public String toString() { return getSummary(); }
}
