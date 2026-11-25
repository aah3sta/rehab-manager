package menus;

import dao.MedicalAssessmentDAO;
import dao.PatientDAO;
import dao.StaffDAO;
import models.MedicalAssessment;
import models.Patient;
import models.Staff;
import util.InputHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MedicalAssessmentMenu {
    private final MedicalAssessmentDAO assessmentDAO = new MedicalAssessmentDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== MEDICAL ASSESSMENT =====");
            System.out.println("1. Record a medical assessment");
            System.out.println("2. Display assessment by ID");
            System.out.println("3. Display all assessments for a patient");
            System.out.println("4. Search assessments by date");
            System.out.println("5. Search assessments by staff member");
            System.out.println("6. Update an assessment");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 6);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: record(); break;
                    case 2: displayById(); break;
                    case 3: displayByPatient(); break;
                    case 4: searchByDate(); break;
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
        System.out.println("\n-- Record Medical Assessment --");
        Patient patient = askForPatient();
        if (patient == null) return;

        Staff staff = askForStaff("ID of the staff member conducting the assessment: ");
        if (staff == null) return;
        if (!staff.canConductAssessment()) {
            System.out.println("Only a Doctor, Therapist or Nurse can conduct an assessment. "
                    + staff.getFullName() + " is a " + staff.getRole() + ".");
            return;
        }

        String history = InputHelper.readLine("Medical history (optional, Enter to skip): ");
        MedicalAssessment a = new MedicalAssessment(0, patient.getId(), staff.getId(),
                null, history, "pending", "pending");
        InputHelper.readValid("Assessment notes: ", a::setAssessmentNotes);
        InputHelper.readValid("Diagnosis: ", a::setDiagnosis);

        long id = assessmentDAO.record(a);
        if (id > 0) System.out.println("Assessment recorded with ID " + id + ".");
        else System.out.println("Recording failed.");
    }

    private void displayById() throws SQLException {
        long id = InputHelper.readId("Enter assessment ID: ");
        MedicalAssessment a = assessmentDAO.findById(id);
        if (a == null) {
            System.out.println("No assessment found with ID " + id + ".");
            return;
        }
        printWithNames(a);
    }

    private void displayByPatient() throws SQLException {
        Patient patient = askForPatient();
        if (patient == null) return;
        List<MedicalAssessment> list = assessmentDAO.findByPatient(patient.getId());
        System.out.println("\n-- Assessments for " + patient.getFullName() + " (" + list.size() + ") --");
        printList(list);
    }

    private void searchByDate() throws SQLException {
        LocalDate date = InputHelper.readDate("Assessment date");
        List<MedicalAssessment> list = assessmentDAO.findByDate(date);
        System.out.println("\n-- Assessments on " + date + " (" + list.size() + ") --");
        printList(list);
    }

    private void searchByStaff() throws SQLException {
        Staff staff = askForStaff("Enter staff ID: ");
        if (staff == null) return;
        List<MedicalAssessment> list = assessmentDAO.findByStaff(staff.getId());
        System.out.println("\n-- Assessments by " + staff.getFullName() + " (" + list.size() + ") --");
        printList(list);
    }

    /**
     * "Update when authorized": only the staff member who conducted the
     * assessment, or any Doctor, may change it.
     */
    private void update() throws SQLException {
        long id = InputHelper.readId("Enter ID of the assessment to update: ");
        MedicalAssessment a = assessmentDAO.findById(id);
        if (a == null) {
            System.out.println("No assessment found with ID " + id + ".");
            return;
        }

        Staff editor = askForStaff("Your staff ID (for authorization): ");
        if (editor == null) return;
        if (!util.Authorization.canEdit(editor, a.getStaffId())) {
            System.out.println("Not authorized: only the staff member who conducted this assessment "
                    + "or a Doctor can update it.");
            return;
        }

        printWithNames(a);
        System.out.println("Press Enter to keep the current value.");
        a.setMedicalHistory(InputHelper.readOptional("Medical history", a.getMedicalHistory()));
        InputHelper.readValid("Assessment notes [" + a.getAssessmentNotes() + "]: ",
                v -> a.setAssessmentNotes(v.isEmpty() ? a.getAssessmentNotes() : v));
        InputHelper.readValid("Diagnosis [" + a.getDiagnosis() + "]: ",
                v -> a.setDiagnosis(v.isEmpty() ? a.getDiagnosis() : v));

        if (!InputHelper.confirm("Save these changes?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(assessmentDAO.update(a) ? "Assessment updated successfully." : "Update failed.");
    }

    // ---- helpers ----

    private Patient askForPatient() throws SQLException {
        long pid = InputHelper.readId("Patient ID: ");
        Patient p = patientDAO.findById(pid);
        if (p == null) System.out.println("No patient found with ID " + pid + ".");
        else System.out.println("  -> " + p.getFullName());
        return p;
    }

    private Staff askForStaff(String prompt) throws SQLException {
        long sid = InputHelper.readId(prompt);
        Staff s = staffDAO.findById(sid);
        if (s == null) System.out.println("No staff member found with ID " + sid + ".");
        else System.out.println("  -> " + s.getFullName() + " (" + s.getRole() + ")");
        return s;
    }

    private void printWithNames(MedicalAssessment a) throws SQLException {
        Patient p = patientDAO.findById(a.getPatientId());
        Staff s = staffDAO.findById(a.getStaffId());
        System.out.println(a.getSummary());
        System.out.println("   Patient:   " + (p != null ? p.getFullName() : "?")
                + " | Conducted by: " + (s != null ? s.getFullName() + " (" + s.getRole() + ")" : "?"));
    }

    private void printList(List<MedicalAssessment> list) {
        if (list.isEmpty()) {
            System.out.println("No assessments found.");
            return;
        }
        for (MedicalAssessment a : list) {
            System.out.println(a.getSummary());
            System.out.println("   ---");
        }
    }
}
