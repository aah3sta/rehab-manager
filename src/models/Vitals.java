package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One set of vital signs. Setters reject impossible values (typing mistakes);
 * getAlerts() flags values that are possible but outside the usual adult range.
 */
public class Vitals {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private long vitalsId;
    private long patientId;
    private long staffId;
    private LocalDateTime recordedAt;
    private double temperature;   // C
    private int pulse;            // beats/min
    private int breathingRate;    // breaths/min
    private double glucose;       // mmol/L
    private int systolic;         // mmHg
    private int diastolic;        // mmHg

    public Vitals(long vitalsId, long patientId, long staffId, LocalDateTime recordedAt,
                  double temperature, int pulse, int breathingRate, double glucose,
                  int systolic, int diastolic) {
        if (patientId <= 0) throw new IllegalArgumentException("A valid patient ID is required.");
        if (staffId <= 0) throw new IllegalArgumentException("A valid staff ID is required.");
        this.vitalsId = vitalsId;
        this.patientId = patientId;
        this.staffId = staffId;
        setRecordedAt(recordedAt);
        setTemperature(temperature);
        setPulse(pulse);
        setBreathingRate(breathingRate);
        setGlucose(glucose);
        setBloodPressure(systolic, diastolic);
    }

    public long getVitalsId() { return vitalsId; }
    public void setVitalsId(long id) { this.vitalsId = id; }
    public long getPatientId() { return patientId; }
    public long getStaffId() { return staffId; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime t) {
        if (t != null && t.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("Vitals cannot be recorded in the future.");
        }
        this.recordedAt = t;
    }

    public double getTemperature() { return temperature; }
    public void setTemperature(double t) {
        if (t < 30 || t > 45) throw new IllegalArgumentException("Temperature must be between 30 and 45 C.");
        this.temperature = Math.round(t * 10) / 10.0;
    }

    public int getPulse() { return pulse; }
    public void setPulse(int p) {
        if (p < 20 || p > 250) throw new IllegalArgumentException("Pulse must be between 20 and 250 beats per minute.");
        this.pulse = p;
    }

    public int getBreathingRate() { return breathingRate; }
    public void setBreathingRate(int r) {
        if (r < 4 || r > 70) throw new IllegalArgumentException("Breathing rate must be between 4 and 70 breaths per minute.");
        this.breathingRate = r;
    }

    public double getGlucose() { return glucose; }
    public void setGlucose(double g) {
        if (g < 1 || g > 40) throw new IllegalArgumentException("Glucose must be between 1.0 and 40.0 mmol/L.");
        this.glucose = Math.round(g * 10) / 10.0;
    }

    public int getSystolic() { return systolic; }
    public int getDiastolic() { return diastolic; }
    public void setBloodPressure(int sys, int dia) {
        if (sys < 50 || sys > 260) throw new IllegalArgumentException("Systolic pressure must be between 50 and 260 mmHg.");
        if (dia < 30 || dia > 160) throw new IllegalArgumentException("Diastolic pressure must be between 30 and 160 mmHg.");
        if (sys <= dia) throw new IllegalArgumentException("Systolic pressure must be higher than diastolic.");
        this.systolic = sys;
        this.diastolic = dia;
    }

    public String getBloodPressure() { return systolic + "/" + diastolic; }

    /** Readings outside typical resting adult ranges, for the nurse's attention. */
    public List<String> getAlerts() {
        List<String> alerts = new ArrayList<>();
        if (temperature >= 38.0) alerts.add("Fever (" + temperature + " C)");
        else if (temperature < 35.0) alerts.add("Low temperature (" + temperature + " C)");
        if (pulse > 100) alerts.add("High pulse (" + pulse + ")");
        else if (pulse < 50) alerts.add("Low pulse (" + pulse + ")");
        if (breathingRate > 20) alerts.add("Fast breathing (" + breathingRate + ")");
        else if (breathingRate < 12) alerts.add("Slow breathing (" + breathingRate + ")");
        if (glucose > 11.0) alerts.add("High glucose (" + glucose + " mmol/L)");
        else if (glucose < 4.0) alerts.add("Low glucose (" + glucose + " mmol/L)");
        if (systolic >= 140 || diastolic >= 90) alerts.add("High blood pressure (" + getBloodPressure() + ")");
        else if (systolic < 90 || diastolic < 60) alerts.add("Low blood pressure (" + getBloodPressure() + ")");
        return alerts;
    }

    public String getSummary() {
        List<String> alerts = getAlerts();
        return String.format("Vitals #%d | Patient #%d | Staff #%d | %s%n"
                        + "   Temp %.1f C | Pulse %d | Breathing %d | Glucose %.1f mmol/L | BP %s%n   %s",
                vitalsId, patientId, staffId, recordedAt != null ? recordedAt.format(FMT) : "-",
                temperature, pulse, breathingRate, glucose, getBloodPressure(),
                alerts.isEmpty() ? "All readings in normal range" : "ALERT: " + String.join(", ", alerts));
    }

    @Override
    public String toString() { return getSummary(); }
}
