package models;

public class Therapist extends Staff {
    public Therapist(long id, String fullName, String contact) {
        super(id, fullName, contact, "Therapist");
    }

    @Override
    public void performDuties() {
        System.out.println("Therapist " + getFullName() + " conducts rehabilitation therapy sessions.");
    }

    @Override
    public boolean canConductAssessment() { return true; }
}
