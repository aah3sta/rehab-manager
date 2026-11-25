package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TherapySession {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private long sessionId;
    private long patientId;
    private long staffId;
    private LocalDateTime sessionDate;
    private int durationMinutes;
    private String notes;

    public TherapySession(long sessionId, long patientId, long staffId,
                          LocalDateTime sessionDate, int durationMinutes, String notes) {
        if (patientId <= 0) throw new IllegalArgumentException("A valid patient ID is required.");
        if (staffId <= 0) throw new IllegalArgumentException("A valid staff ID is required.");
        this.sessionId = sessionId;
        this.patientId = patientId;
        this.staffId = staffId;
        setSessionDate(sessionDate);
        setDurationMinutes(durationMinutes);
        setNotes(notes);
    }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long id) { this.sessionId = id; }
    public long getPatientId() { return patientId; }
    public long getStaffId() { return staffId; }

    public LocalDateTime getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDateTime date) {
        if (date == null) throw new IllegalArgumentException("Session date is required.");
        if (date.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("Only sessions that have taken place can be recorded.");
        }
        this.sessionDate = date;
    }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int minutes) {
        if (minutes < 5 || minutes > 480) {
            throw new IllegalArgumentException("Duration must be between 5 and 480 minutes.");
        }
        this.durationMinutes = minutes;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) {
        if (notes == null || notes.trim().isEmpty()) {
            throw new IllegalArgumentException("Session notes cannot be empty.");
        }
        this.notes = notes.trim();
    }

    /** e.g. 95 -> "1 h 35 min" */
    public static String formatMinutes(long minutes) {
        long h = minutes / 60, m = minutes % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    public String getSummary() {
        return String.format("Session #%d | Patient #%d | Therapist #%d | %s | %s%n   Notes: %s",
                sessionId, patientId, staffId, sessionDate.format(FMT),
                formatMinutes(durationMinutes), notes);
    }

    @Override
    public String toString() { return getSummary(); }
}
