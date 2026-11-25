package menus;

import dao.PatientDAO;
import models.Patient;
import util.InputHelper;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;

public class PatientMenu {
    private final PatientDAO patientDAO = new PatientDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== PATIENT MANAGEMENT =====");
            System.out.println("1. Register a new patient");
            System.out.println("2. Display all patients");
            System.out.println("3. Search patient by ID");
            System.out.println("4. Search patient by name or contact");
            System.out.println("5. Update patient details");
            System.out.println("6. Delete patient record");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 6);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: register(); break;
                    case 2: displayAll(); break;
                    case 3: searchById(); break;
                    case 4: searchByKeyword(); break;
                    case 5: update(); break;
                    case 6: delete(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void register() throws SQLException {
        System.out.println("\n-- Register Patient --");
        // Build with placeholder values, then let each setter validate the real input
        Patient p = new Patient(0, "Temp", "OTHER", LocalDate.now(), "0000000000", null);
        InputHelper.readValid("Full name: ", p::setFullName);
        InputHelper.readValid("Gender (MALE/FEMALE/OTHER): ", p::setGender);
        while (true) {
            try {
                p.setDateOfBirth(InputHelper.readDate("Date of birth"));
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
        InputHelper.readValid("Contact: ", p::setContact);

        long id = patientDAO.register(p);
        if (id > 0) System.out.println("Patient registered successfully with ID " + id + ".");
        else System.out.println("Registration failed.");
    }

    private void displayAll() throws SQLException {
        List<Patient> patients = patientDAO.getAll();
        System.out.println("\n-- All Patients (" + patients.size() + ") --");
        printList(patients);
    }

    private void searchById() throws SQLException {
        long id = InputHelper.readId("Enter patient ID: ");
        Patient p = patientDAO.findById(id);
        System.out.println(p != null ? p.getDetails() : "No patient found with ID " + id + ".");
    }

    private void searchByKeyword() throws SQLException {
        String keyword = InputHelper.readRequired("Enter name or contact (partial is fine): ");
        List<Patient> results = patientDAO.search(keyword);
        System.out.println("\n-- " + results.size() + " result(s) --");
        printList(results);
    }

    private void update() throws SQLException {
        long id = InputHelper.readId("Enter ID of the patient to update: ");
        Patient p = patientDAO.findById(id);
        if (p == null) {
            System.out.println("No patient found with ID " + id + ".");
            return;
        }
        System.out.println("Current: " + p.getDetails());
        System.out.println("Press Enter to keep the current value.");

        InputHelper.readValid("Full name [" + p.getFullName() + "]: ",
                s -> p.setFullName(s.isEmpty() ? p.getFullName() : s));
        InputHelper.readValid("Gender [" + p.getGender() + "]: ",
                s -> p.setGender(s.isEmpty() ? p.getGender() : s));
        while (true) {
            try {
                p.setDateOfBirth(InputHelper.readOptionalDate("Date of birth", p.getDateOfBirth()));
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
        InputHelper.readValid("Contact [" + p.getContact() + "]: ",
                s -> p.setContact(s.isEmpty() ? p.getContact() : s));

        if (!InputHelper.confirm("Save these changes?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(patientDAO.update(p) ? "Patient updated successfully." : "Update failed.");
    }

    private void delete() throws SQLException {
        long id = InputHelper.readId("Enter ID of the patient to delete: ");
        Patient p = patientDAO.findById(id);
        if (p == null) {
            System.out.println("No patient found with ID " + id + ".");
            return;
        }
        // "When permitted": refuse if the patient has clinical records
        int linked = patientDAO.countLinkedRecords(id);
        if (linked > 0) {
            System.out.println("Cannot delete: this patient has " + linked
                    + " medical assessment(s) on record. Clinical history must be kept.");
            return;
        }
        System.out.println(p.getDetails());
        if (!InputHelper.confirm("Are you sure you want to permanently delete this patient?")) {
            System.out.println("Delete cancelled.");
            return;
        }
        try {
            System.out.println(patientDAO.delete(id) ? "Patient deleted." : "Delete failed.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Cannot delete: this patient is referenced by other records.");
        }
    }

    private void printList(List<Patient> patients) {
        if (patients.isEmpty()) {
            System.out.println("No patients found.");
            return;
        }
        for (Patient p : patients) System.out.println(p.getDetails());
    }
}
