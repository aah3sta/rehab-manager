package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A laboratory test ordered under an existing medical assessment. */
public class LabTest {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private long testId;
    private long assessmentId;
    private String testName;
    private LocalDateTime testDate;
    private String result;   // null = pending
    private String notes;

    public LabTest(long testId, long assessmentId, String testName,
                   LocalDateTime testDate, String result, String notes) {
        if (assessmentId <= 0) throw new IllegalArgumentException("A valid assessment ID is required.");
        this.testId = testId;
        this.assessmentId = assessmentId;
        setTestName(testName);
        setTestDate(testDate);
        setResult(result);
        setNotes(notes);
    }

    public long getTestId() { return testId; }
    public void setTestId(long id) { this.testId = id; }
    public long getAssessmentId() { return assessmentId; }

    public String getTestName() { return testName; }
    public void setTestName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Test name cannot be empty.");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Test name cannot be longer than 100 characters.");
        }
        this.testName = name.trim();
    }

    public LocalDateTime getTestDate() { return testDate; }
    public void setTestDate(LocalDateTime date) {
        if (date != null && date.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("Test date cannot be in the future.");
        }
        this.testDate = date; // null = now, set when saved
    }

    public String getResult() { return result; }
    public void setResult(String result) {
        if (result != null && result.trim().length() > 255) {
            throw new IllegalArgumentException("Result cannot be longer than 255 characters.");
        }
        this.result = blankToNull(result);
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = blankToNull(notes); }

    public boolean isPending() { return result == null; }

    public String getStatus() { return isPending() ? "Pending" : "Completed"; }

    public String getSummary() {
        return String.format("Lab test #%d | Assessment #%d | %s | %s | %s%n   Result: %s%n   Notes:  %s",
                testId, assessmentId, testName,
                testDate != null ? testDate.format(FMT) : "-", getStatus(),
                result != null ? result : "(awaiting result)", notes != null ? notes : "-");
    }

    @Override
    public String toString() { return getSummary(); }

    static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
