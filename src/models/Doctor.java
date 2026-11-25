package models;

public class Doctor extends Staff {
    public Doctor(long id, String fullName, String contact) {
        super(id, fullName, contact, "Doctor");
    }

    @Override
    public String getDutiesDescription() {
        return "Dr. " + getFullName() + " conducts medical assessments and diagnoses.";
    }

    @Override
    public boolean canConductAssessment() { return true; }

    @Override public boolean canRecordVitals() { return true; }
    @Override public boolean canManageMedication() { return true; }
    @Override public boolean isDoctor() { return true; }
}
