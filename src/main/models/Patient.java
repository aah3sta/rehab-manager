package main.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient extends Person {
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDateTime registrationDate;

    public Patient() {}

    public Patient(long id, String fullName, String contact,
                   String gender, LocalDate dateOfBirth, LocalDateTime registrationDate) {
        super(id, fullName, contact);
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.registrationDate = registrationDate;
    }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
}
