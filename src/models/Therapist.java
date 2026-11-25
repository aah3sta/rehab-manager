package models;

public class Therapist extends Staff {
    public Therapist(long id, String fullName, String contact) {
        super(id, fullName, contact, "Therapist");
    }

    @Override
    public String getDutiesDescription() {
        return "Therapist " + getFullName() + " conducts rehabilitation therapy sessions.";
    }

    @Override
    public boolean canConductAssessment() { return true; }

    @Override public boolean canConductTherapy() { return true; }
}
