package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public class Patient extends Person {
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDateTime registrationDate;

    public Patient(long id, String fullName, String gender, LocalDate dateOfBirth,
                   String contact, LocalDateTime registrationDate) {
        super(id, fullName, contact);
        setGender(gender);
        setDateOfBirth(dateOfBirth);
        this.registrationDate = registrationDate;
    }

    public String getGender() { return gender; }
    public void setGender(String gender) {
        if (gender == null) {
            throw new IllegalArgumentException("Gender must be MALE, FEMALE, or OTHER.");
        }
        String g = gender.trim().toUpperCase();
        // Accept the short forms M / F / O as a convenience
        if (g.equals("M")) g = "MALE";
        else if (g.equals("F")) g = "FEMALE";
        else if (g.equals("O")) g = "OTHER";
        if (!g.equals("MALE") && !g.equals("FEMALE") && !g.equals("OTHER")) {
            throw new IllegalArgumentException("Gender must be MALE, FEMALE, or OTHER.");
        }
        this.gender = g;
    }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dob) {
        if (dob == null || dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be empty or in the future.");
        }
        if (dob.isBefore(LocalDate.now().minusYears(130))) {
            throw new IllegalArgumentException("Date of birth is too far in the past.");
        }
        this.dateOfBirth = dob;
    }

    public LocalDateTime getRegistrationDate() { return registrationDate; }

    public int calculateAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @Override
    public String getDetails() {
        return String.format("Patient #%d | %s | %s | DOB: %s | Age: %d | Contact: %s",
                getId(), getFullName(), gender, dateOfBirth, calculateAge(), getContact());
    }
}
