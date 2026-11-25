package menus;

import dao.VitalsDAO;
import models.Patient;
import models.Staff;
import models.Vitals;
import util.InputHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class VitalsMenu {
    private final VitalsDAO vitalsDAO = new VitalsDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== VITALS =====");
            System.out.println("1. Record vital signs");
            System.out.println("2. Display a vitals record by ID");
            System.out.println("3. Display all vitals for a patient");
            System.out.println("4. Show latest vitals for a patient");
            System.out.println("5. Search vitals by date");
            System.out.println("6. Search vitals by staff member");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 6);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: record(); break;
                    case 2: displayById(); break;
                    case 3: displayByPatient(); break;
                    case 4: displayLatest(); break;
                    case 5: searchByDate(); break;
                    case 6: searchByStaff(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void record() throws SQLException {
        System.out.println("\n-- Record Vital Signs --");
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        Staff s = MenuSupport.askStaffWithRole("ID of the nurse or doctor taking the readings: ",
                Staff::canRecordVitals, "a Doctor or Nurse");
        if (s == null) return;

        // Start from normal values; each setter validates what the user types
        Vitals v = new Vitals(0, p.getId(), s.getId(), null, 36.8, 72, 16, 5.5, 120, 80);
        InputHelper.readValid("Temperature (C): ", x -> v.setTemperature(InputHelper.parseDouble(x, "Temperature")));
        InputHelper.readValid("Pulse (beats/min): ", x -> v.setPulse(InputHelper.parseInt(x, "Pulse")));
        InputHelper.readValid("Breathing rate (breaths/min): ",
                x -> v.setBreathingRate(InputHelper.parseInt(x, "Breathing rate")));
        InputHelper.readValid("Blood glucose (mmol/L): ", x -> v.setGlucose(InputHelper.parseDouble(x, "Glucose")));
        while (true) {
            try {
                int sys = InputHelper.parseInt(InputHelper.readLine("Blood pressure, systolic (top number): "), "Systolic");
                int dia = InputHelper.parseInt(InputHelper.readLine("Blood pressure, diastolic (bottom number): "), "Diastolic");
                v.setBloodPressure(sys, dia);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }

        long id = vitalsDAO.record(v);
        if (id <= 0) {
            System.out.println("Recording failed.");
            return;
        }
        System.out.println("Vitals recorded with ID " + id + ".");
        List<String> alerts = v.getAlerts();
        if (!alerts.isEmpty()) System.out.println("ALERT for " + p.getFullName() + ": " + String.join(", ", alerts));
    }

    private void displayById() throws SQLException {
        long id = InputHelper.readId("Enter vitals ID: ");
        Vitals v = vitalsDAO.findById(id);
        System.out.println(v != null ? v.getSummary() : "No vitals record found with ID " + id + ".");
    }

    private void displayByPatient() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        MenuSupport.printList("Vitals for " + p.getFullName(), vitalsDAO.findByPatient(p.getId()), Vitals::getSummary);
    }

    private void displayLatest() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        Vitals v = vitalsDAO.findLatest(p.getId());
        if (v == null) {
            System.out.println("No vitals recorded for " + p.getFullName() + " yet.");
            return;
        }
        System.out.println("\n-- Latest vitals for " + p.getFullName() + " --");
        System.out.println(v.getSummary());
        System.out.println("   Recorded by: " + MenuSupport.staffName(v.getStaffId()));
    }

    private void searchByDate() throws SQLException {
        LocalDate date = InputHelper.readDate("Date");
        MenuSupport.printList("Vitals recorded on " + date, vitalsDAO.findByDate(date), Vitals::getSummary);
    }

    private void searchByStaff() throws SQLException {
        Staff s = MenuSupport.askStaff("Enter staff ID: ");
        if (s == null) return;
        MenuSupport.printList("Vitals recorded by " + s.getFullName(), vitalsDAO.findByStaff(s.getId()),
                Vitals::getSummary);
    }
}
