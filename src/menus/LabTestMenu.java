package menus;

import dao.LabTestDAO;
import dao.MedicalAssessmentDAO;
import models.LabTest;
import models.MedicalAssessment;
import models.Patient;
import util.InputHelper;

import java.sql.SQLException;
import java.util.List;

public class LabTestMenu {
    private final LabTestDAO labDAO = new LabTestDAO();
    private final MedicalAssessmentDAO assessmentDAO = new MedicalAssessmentDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== LABORATORY TESTS =====");
            System.out.println("1. Record a lab test under an assessment");
            System.out.println("2. Display a test by test ID");
            System.out.println("3. Display all tests for an assessment");
            System.out.println("4. View a patient's tests (through their assessments)");
            System.out.println("5. Update test result or notes");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 5);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: record(); break;
                    case 2: displayById(); break;
                    case 3: displayByAssessment(); break;
                    case 4: displayByPatient(); break;
                    case 5: update(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void record() throws SQLException {
        System.out.println("\n-- Record Lab Test --");
        MedicalAssessment a = askAssessment();
        if (a == null) return;

        LabTest t = new LabTest(0, a.getAssessmentId(), "pending", null, null, null);
        InputHelper.readValid("Test name (e.g. Full blood count): ", t::setTestName);
        InputHelper.readValid("Result (Enter if still pending): ", t::setResult);
        t.setNotes(InputHelper.readLine("Notes (optional): "));

        long id = labDAO.record(t);
        System.out.println(id > 0 ? "Lab test recorded with ID " + id + " (" + t.getStatus() + ")."
                : "Recording failed.");
    }

    private void displayById() throws SQLException {
        long id = InputHelper.readId("Enter test ID: ");
        LabTest t = labDAO.findById(id);
        System.out.println(t != null ? t.getSummary() : "No lab test found with ID " + id + ".");
    }

    private void displayByAssessment() throws SQLException {
        MedicalAssessment a = askAssessment();
        if (a == null) return;
        MenuSupport.printList("Tests for assessment #" + a.getAssessmentId(),
                labDAO.findByAssessment(a.getAssessmentId()), LabTest::getSummary);
    }

    private void displayByPatient() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        List<LabTest> tests = labDAO.findByPatient(p.getId());
        MenuSupport.printList("Lab tests for " + p.getFullName(), tests, LabTest::getSummary);
        long pending = tests.stream().filter(LabTest::isPending).count();
        if (pending > 0) System.out.println(pending + " test(s) still awaiting results.");
    }

    /** "Update when authorized": the assessment's staff member or a Doctor. */
    private void update() throws SQLException {
        long id = InputHelper.readId("Enter ID of the test to update: ");
        LabTest t = labDAO.findById(id);
        if (t == null) {
            System.out.println("No lab test found with ID " + id + ".");
            return;
        }
        System.out.println(t.getSummary());
        if (!MenuSupport.authorize(labDAO.findOwnerStaffId(id))) return;

        System.out.println("Press Enter to keep the current value.");
        InputHelper.readValid("Result [" + (t.getResult() == null ? "pending" : t.getResult()) + "]: ",
                v -> t.setResult(v.isEmpty() ? t.getResult() : v));
        t.setNotes(InputHelper.readOptional("Notes", t.getNotes()));

        if (!InputHelper.confirm("Save these changes?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(labDAO.update(t) ? "Lab test updated (" + t.getStatus() + ")." : "Update failed.");
    }

    private MedicalAssessment askAssessment() throws SQLException {
        long id = InputHelper.readId("Assessment ID: ");
        MedicalAssessment a = assessmentDAO.findById(id);
        if (a == null) {
            System.out.println("No assessment found with ID " + id + ". Lab tests must belong to an existing assessment.");
            return null;
        }
        System.out.println("  -> " + MenuSupport.patientName(a.getPatientId()) + ", diagnosis: " + a.getDiagnosis());
        return a;
    }
}
