package models;

public class Nurse extends Staff {
    public Nurse(long id, String fullName, String contact) {
        super(id, fullName, contact, "Nurse");
    }

    @Override
    public String getDutiesDescription() {
        return "Nurse " + getFullName() + " monitors patient vitals and administers medication.";
    }

    @Override
    public boolean canConductAssessment() { return true; }

    @Override public boolean canRecordVitals() { return true; }
    @Override public boolean canManageMedication() { return true; }
}
