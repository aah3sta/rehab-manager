package main.models;

public class Staff extends Person {
    private String role;

    public Staff() {}

    public Staff(long id, String fullName, String contact, String role) {
        super(id, fullName, contact);
        this.role = role;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
