package main;

import main.database.PatientDAO;
import java.util.Scanner;

import main.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import main.models.Patient;
import main.models.Person;
import main.models.Staff;

public class Main {
    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {

    System.out.println("Database connected successfully!");

} catch (SQLException e) {

    System.out.println("Database connection failed!");
    e.printStackTrace();

}
        Patient patient = new Patient(1, "Jane Doe", "0712345678",
                "FEMALE", LocalDate.of(1995, 5, 20), LocalDateTime.now());
        Staff staff = new Staff(1, "Dr. John Smith", "0798765432", "Doctor");

        System.out.println("Patient: " + patient.getFullName() + " | " + patient.getGender());
        System.out.println("Staff: " + staff.getFullName() + " | " + staff.getRole());

    Scanner scanner = new Scanner(System.in);
PatientDAO patientDAO = new PatientDAO();

int choice;

do {
    System.out.println("\n===== REHAB MANAGEMENT SYSTEM =====");
    System.out.println("1. Register Patient");
    System.out.println("2. View All Patients");
    System.out.println("3. Search Patient");
    System.out.println("4. Update Patient");
    System.out.println("5. Delete Patient");
    System.out.println("6. Exit");
    System.out.print("Choose an option: ");

    choice = scanner.nextInt();
    scanner.nextLine(); // Clear the input buffer

    switch (choice) {

        case 1:
            System.out.println("\n--- REGISTER PATIENT ---");

            System.out.print("Enter full name: ");
            String name = scanner.nextLine();

            System.out.print("Enter gender (MALE/FEMALE): ");
            String gender = scanner.nextLine().toUpperCase();

            System.out.print("Enter date of birth (YYYY-MM-DD): ");
            String dob = scanner.nextLine();

            System.out.print("Enter contact: ");
            String contact = scanner.nextLine();

            patientDAO.registerPatient(name, gender, dob, contact);
            break;

        case 2:
            patientDAO.displayPatients();
            break;

       case 3:
    System.out.print("Enter Patient ID: ");
    long patientId = scanner.nextLong();
    scanner.nextLine();

    patientDAO.searchPatient(patientId);
    break;

case 4:
    System.out.print("Enter Patient ID to update: ");
    long updateId = scanner.nextLong();
    scanner.nextLine();

    System.out.print("Enter new full name: ");
    String newName = scanner.nextLine();

    System.out.print("Enter new gender: ");
    String newGender = scanner.nextLine().toUpperCase();

    System.out.print("Enter new date of birth (YYYY-MM-DD): ");
    String newDob = scanner.nextLine();

    System.out.print("Enter new contact: ");
    String newContact = scanner.nextLine();

    patientDAO.updatePatient(updateId, newName, newGender,
                             newDob, newContact);
    break;

case 5:
    System.out.print("Enter Patient ID to delete: ");
    long deleteId = scanner.nextLong();
    scanner.nextLine();

    System.out.print("Are you sure you want to delete this patient? (yes/no): ");
    String confirmation = scanner.nextLine();

    if (confirmation.equalsIgnoreCase("yes")) {
        patientDAO.deletePatient(deleteId);
    } else {
        System.out.println("Deletion cancelled.");
    }
    break;

case 6:
    System.out.println("Thank you for using Rehab Management System!");
    break;
    }

} while (choice != 6);
    }

}

