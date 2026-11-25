package models;

public class Nurse extends Staff {
    public Nurse(long id, String fullName, String contact) {
        super(id, fullName, contact, "Nurse");
    }

    @Override
    public void performDuties() {
        System.out.println("Nurse " + getFullName() + " monitors patient vitals and administers medication.");
    }

    @Override
    public boolean canConductAssessment() { return true; }
}
