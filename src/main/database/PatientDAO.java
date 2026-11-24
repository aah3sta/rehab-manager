package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PatientDAO {

    public void registerPatient(String fullName, String gender,
                                String dateOfBirth, String contact) {

        String sql = "INSERT INTO patient "
                   + "(FULL_NAME, GENDER, DATE_OF_BIRTH, CONTACT) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, gender);
            pstmt.setDate(3, java.sql.Date.valueOf(dateOfBirth));
            pstmt.setString(4, contact);

            pstmt.executeUpdate();

            System.out.println("Patient registered successfully!");

        } catch (SQLException e) {
            System.out.println("Registration failed!");
            e.printStackTrace();
        }

    }

    public void displayPatients() {

    String sql = "SELECT * FROM patient";

    try (Connection conn = DatabaseConnection.getConnection();
         java.sql.Statement stmt = conn.createStatement();
         java.sql.ResultSet rs = stmt.executeQuery(sql)) {

        System.out.println("\n--- REGISTERED PATIENTS ---");

        while (rs.next()) {
            System.out.println("Patient ID: " + rs.getLong("PATIENT_ID"));
            System.out.println("Full Name: " + rs.getString("FULL_NAME"));
            System.out.println("Gender: " + rs.getString("GENDER"));
            System.out.println("Date of Birth: " + rs.getDate("DATE_OF_BIRTH"));
            System.out.println("Contact: " + rs.getString("CONTACT"));
            System.out.println("---------------------------");
        }

    } catch (SQLException e) {
        System.out.println("Could not retrieve patients!");
        e.printStackTrace();}
    }

    public void searchPatient(long patientId) {

    String sql = "SELECT * FROM patient WHERE PATIENT_ID = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, patientId);

        try (java.sql.ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                System.out.println("\n--- PATIENT FOUND ---");
                System.out.println("Patient ID: " + rs.getLong("PATIENT_ID"));
                System.out.println("Full Name: " + rs.getString("FULL_NAME"));
                System.out.println("Gender: " + rs.getString("GENDER"));
                System.out.println("Date of Birth: " + rs.getDate("DATE_OF_BIRTH"));
                System.out.println("Contact: " + rs.getString("CONTACT"));
            } else {
                System.out.println("No patient found with ID: " + patientId);
            }
        }

    } catch (SQLException e) {
        System.out.println("Search failed!");
        e.printStackTrace();}
    }
    public void updatePatient(long patientId, String fullName,
                          String gender, String dateOfBirth,
                          String contact) {

    String sql = "UPDATE patient SET FULL_NAME = ?, GENDER = ?, "
               + "DATE_OF_BIRTH = ?, CONTACT = ? "
               + "WHERE PATIENT_ID = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, fullName);
        pstmt.setString(2, gender);
        pstmt.setDate(3, java.sql.Date.valueOf(dateOfBirth));
        pstmt.setString(4, contact);
        pstmt.setLong(5, patientId);

        int rowsUpdated = pstmt.executeUpdate();

        if (rowsUpdated > 0) {
            System.out.println("Patient updated successfully!");
        } else {
            System.out.println("No patient found with that ID.");
        }

    } catch (SQLException e) {
        System.out.println("Update failed!");
        e.printStackTrace();}
    }
    public void deletePatient(long patientId) {

    String sql = "DELETE FROM patient WHERE PATIENT_ID = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, patientId);

        int rowsDeleted = pstmt.executeUpdate();

        if (rowsDeleted > 0) {
            System.out.println("Patient deleted successfully!");
        } else {
            System.out.println("No patient found with that ID.");
        }

    } catch (SQLException e) {
        System.out.println("Deletion failed!");
        e.printStackTrace();
    }

}
}
