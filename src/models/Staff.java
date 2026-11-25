package models;

/**
 * Concrete class so general roles (e.g. Receptionist) work too,
 * while Doctor, Therapist and Nurse extend it and override performDuties().
 */
public class Staff extends Person {
    private String role;

    public Staff(long id, String fullName, String contact, String role) {
        super(id, fullName, contact);
        setRole(role);
    }

    public String getRole() { return role; }
    public void setRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be empty.");
        }
        String r = role.trim();
        if (r.length() > 50) {
            throw new IllegalArgumentException("Role cannot be longer than 50 characters.");
        }
        // Store roles consistently: "doctor" -> "Doctor"
        this.role = r.substring(0, 1).toUpperCase() + r.substring(1).toLowerCase();
    }

    /** Overridden by Doctor, Therapist and Nurse (runtime polymorphism). */
    public String getDutiesDescription() {
        return getFullName() + " (" + role + ") performs general centre operations.";
    }

    /** Console version prints; the GUI shows getDutiesDescription() in a dialog. */
    public void performDuties() {
        System.out.println(getDutiesDescription());
    }

    // ---- What each role is allowed to do (overridden by subclasses) ----

    /** Doctor, Therapist, Nurse. */
    public boolean canConductAssessment() { return false; }

    /** Doctor, Nurse. */
    public boolean canRecordVitals() { return false; }

    /** Therapist. */
    public boolean canConductTherapy() { return false; }

    /** Doctor, Nurse. */
    public boolean canManageMedication() { return false; }

    public boolean isDoctor() { return false; }

    @Override
    public String getDetails() {
        return String.format("Staff #%d | %s | Role: %s | Contact: %s",
                getId(), getFullName(), role, getContact());
    }
}
