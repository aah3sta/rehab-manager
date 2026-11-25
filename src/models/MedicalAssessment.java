package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MedicalAssessment {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private long assessmentId;
    private long patientId;
    private long staffId;
    private LocalDateTime assessmentDate;
    private String medicalHistory;
    private String assessmentNotes;
    private String diagnosis;

    public MedicalAssessment(long assessmentId, long patientId, long staffId,
                             LocalDateTime assessmentDate, String medicalHistory,
                             String assessmentNotes, String diagnosis) {
        if (patientId <= 0) throw new IllegalArgumentException("A valid patient ID is required.");
        if (staffId <= 0) throw new IllegalArgumentException("A valid staff ID is required.");
        this.assessmentId = assessmentId;
        this.patientId = patientId;
        this.staffId = staffId;
        setAssessmentDate(assessmentDate);
        setMedicalHistory(medicalHistory);
        setAssessmentNotes(assessmentNotes);
        setDiagnosis(diagnosis);
    }

    public long getAssessmentId() { return assessmentId; }
    public void setAssessmentId(long id) { this.assessmentId = id; }
    public long getPatientId() { return patientId; }
    public long getStaffId() { return staffId; }

    public LocalDateTime getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDateTime date) {
        if (date != null && date.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("Assessment date cannot be in the future.");
        }
        this.assessmentDate = date; // null = let the database use the current time
    }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String history) {
        // Optional field: blank becomes null
        this.medicalHistory = (history == null || history.trim().isEmpty()) ? null : history.trim();
    }

    public String getAssessmentNotes() { return assessmentNotes; }
    public void setAssessmentNotes(String notes) {
        if (notes == null || notes.trim().isEmpty()) {
            throw new IllegalArgumentException("Assessment notes cannot be empty.");
        }
        this.assessmentNotes = notes.trim();
    }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) {
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            throw new IllegalArgumentException("Diagnosis cannot be empty.");
        }
        if (diagnosis.trim().length() > 255) {
            throw new IllegalArgumentException("Diagnosis cannot be longer than 255 characters.");
        }
        this.diagnosis = diagnosis.trim();
    }

    public String getSummary() {
        return String.format("Assessment #%d | Patient #%d | Staff #%d | Date: %s%n"
                        + "   History:   %s%n   Notes:     %s%n   Diagnosis: %s",
                assessmentId, patientId, staffId,
                assessmentDate != null ? assessmentDate.format(FMT) : "-",
                medicalHistory != null ? medicalHistory : "-", assessmentNotes, diagnosis);
    }

    @Override
    public String toString() { return getSummary(); }
}
