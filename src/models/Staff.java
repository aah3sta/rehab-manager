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
    public void performDuties() {
        System.out.println(getFullName() + " (" + role + ") performs general centre operations.");
    }

    /** Only clinical staff may conduct medical assessments. */
    public boolean canConductAssessment() {
        return false;
    }

    @Override
    public String getDetails() {
        return String.format("Staff #%d | %s | Role: %s | Contact: %s",
                getId(), getFullName(), role, getContact());
    }
}
