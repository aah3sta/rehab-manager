package menus;

import dao.PatientDAO;
import dao.StaffDAO;
import models.Patient;
import models.Staff;
import util.Authorization;
import util.InputHelper;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Prompts shared by the module 4-8 menus. */
final class MenuSupport {
    private static final PatientDAO PATIENTS = new PatientDAO();
    private static final StaffDAO STAFF = new StaffDAO();

    private MenuSupport() { }

    /** Asks for a patient ID and confirms the name; null if not found. */
    static Patient askPatient() throws SQLException {
        long id = InputHelper.readId("Patient ID: ");
        Patient p = PATIENTS.findById(id);
        if (p == null) System.out.println("No patient found with ID " + id + ".");
        else System.out.println("  -> " + p.getFullName());
        return p;
    }

    static Staff askStaff(String prompt) throws SQLException {
        long id = InputHelper.readId(prompt);
        Staff s = STAFF.findById(id);
        if (s == null) System.out.println("No staff member found with ID " + id + ".");
        else System.out.println("  -> " + s.getFullName() + " (" + s.getRole() + ")");
        return s;
    }

    /**
     * Asks for a staff member and checks their role is allowed to do this.
     * e.g. askStaffWithRole("Nurse or doctor ID: ", Staff::canRecordVitals, "a Doctor or Nurse")
     */
    static Staff askStaffWithRole(String prompt, Predicate<Staff> allowed, String who) throws SQLException {
        Staff s = askStaff(prompt);
        if (s != null && !allowed.test(s)) {
            System.out.println("Only " + who + " can do this. " + s.getFullName() + " is a " + s.getRole() + ".");
            return null;
        }
        return s;
    }

    /** "Update when authorized": the record's owner or a Doctor. Returns false if refused. */
    static boolean authorize(long ownerStaffId) throws SQLException {
        Staff editor = askStaff("Your staff ID (for authorization): ");
        if (editor == null) return false;
        if (!Authorization.canEdit(editor, ownerStaffId)) {
            System.out.println(Authorization.deniedMessage(staffName(ownerStaffId)));
            return false;
        }
        return true;
    }

    static String patientName(long id) throws SQLException {
        Patient p = PATIENTS.findById(id);
        return p == null ? "Patient #" + id : p.getFullName();
    }

    static String staffName(long id) throws SQLException {
        Staff s = STAFF.findById(id);
        return s == null ? "Staff #" + id : s.getFullName();
    }

    static <T> void printList(String title, List<T> items, Function<T, String> toText) {
        System.out.println("\n-- " + title + " (" + items.size() + ") --");
        if (items.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (T item : items) {
            System.out.println(toText.apply(item));
            System.out.println("   ---");
        }
    }
}
