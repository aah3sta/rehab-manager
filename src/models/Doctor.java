package models;

public class Doctor extends Staff {
    public Doctor(long id, String fullName, String contact) {
        super(id, fullName, contact, "Doctor");
    }

    @Override
    public void performDuties() {
        System.out.println("Dr. " + getFullName() + " conducts medical assessments and diagnoses.");
    }

    @Override
    public boolean canConductAssessment() { return true; }
}
