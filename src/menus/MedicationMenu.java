package menus;

import dao.MedicationDAO;
import models.MedicationRecord;
import models.Patient;
import models.Staff;
import util.InputHelper;

import java.sql.SQLException;

public class MedicationMenu {
    private final MedicationDAO medicationDAO = new MedicationDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== MEDICATION =====");
            System.out.println("1. Record medication for a patient");
            System.out.println("2. Display a medication record by ID");
            System.out.println("3. Display a patient's medication history");
            System.out.println("4. Search by medication name");
            System.out.println("5. Search by staff member");
            System.out.println("6. Update medication details or instructions");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 6);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: record(); break;
                    case 2: displayById(); break;
                    case 3: displayByPatient(); break;
                    case 4: searchByName(); break;
                    case 5: searchByStaff(); break;
                    case 6: update(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void record() throws SQLException {
        System.out.println("\n-- Record Medication --");
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        Staff s = MenuSupport.askStaffWithRole("Responsible doctor or nurse ID: ",
                Staff::canManageMedication, "a Doctor or Nurse");
        if (s == null) return;

        MedicationRecord m = new MedicationRecord(0, p.getId(), s.getId(), "x", "1", "x", null, null);
        InputHelper.readValid("Medication name: ", m::setMedicationName);
        InputHelper.readValid("Dosage (e.g. 500 mg): ", m::setDosage);
        InputHelper.readValid("Frequency (e.g. Twice daily): ", m::setFrequency);
        m.setInstructions(InputHelper.readLine("Instructions (optional, e.g. Take after meals): "));

        long id = medicationDAO.record(m);
        System.out.println(id > 0 ? "Medication recorded with ID " + id + "." : "Recording failed.");
    }

    private void displayById() throws SQLException {
        long id = InputHelper.readId("Enter medication record ID: ");
        MedicationRecord m = medicationDAO.findById(id);
        System.out.println(m != null ? m.getSummary() : "No medication record found with ID " + id + ".");
    }

    private void displayByPatient() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        MenuSupport.printList("Medication history for " + p.getFullName(),
                medicationDAO.findByPatient(p.getId()), MedicationRecord::getSummary);
    }

    private void searchByName() throws SQLException {
        String name = InputHelper.readRequired("Medication name (partial is fine): ");
        MenuSupport.printList("Records matching \"" + name + "\"", medicationDAO.searchByName(name),
                MedicationRecord::getSummary);
    }

    private void searchByStaff() throws SQLException {
        Staff s = MenuSupport.askStaff("Enter staff ID: ");
        if (s == null) return;
        MenuSupport.printList("Medication recorded by " + s.getFullName(),
                medicationDAO.findByStaff(s.getId()), MedicationRecord::getSummary);
    }

    /** "Update when authorized": the responsible staff member or a Doctor. */
    private void update() throws SQLException {
        long id = InputHelper.readId("Enter ID of the medication record to update: ");
        MedicationRecord m = medicationDAO.findById(id);
        if (m == null) {
            System.out.println("No medication record found with ID " + id + ".");
            return;
        }
        System.out.println(m.getSummary());
        if (!MenuSupport.authorize(m.getStaffId())) return;

        System.out.println("Press Enter to keep the current value.");
        InputHelper.readValid("Medication name [" + m.getMedicationName() + "]: ",
                v -> m.setMedicationName(v.isEmpty() ? m.getMedicationName() : v));
        InputHelper.readValid("Dosage [" + m.getDosage() + "]: ",
                v -> m.setDosage(v.isEmpty() ? m.getDosage() : v));
        InputHelper.readValid("Frequency [" + m.getFrequency() + "]: ",
                v -> m.setFrequency(v.isEmpty() ? m.getFrequency() : v));
        m.setInstructions(InputHelper.readOptional("Instructions", m.getInstructions()));

        if (!InputHelper.confirm("Save these changes?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(medicationDAO.update(m) ? "Medication record updated." : "Update failed.");
    }
}
