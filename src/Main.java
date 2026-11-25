import database.DatabaseConnection;
import menus.MedicalAssessmentMenu;
import menus.PatientMenu;
import menus.StaffMenu;
import util.InputHelper;

public class Main {
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  REHABILITATION CENTER MANAGEMENT SYSTEM");
        System.out.println("==============================================");

        if (!DatabaseConnection.testConnection()) {
            return;
        }

        PatientMenu patientMenu = new PatientMenu();
        StaffMenu staffMenu = new StaffMenu();
        MedicalAssessmentMenu assessmentMenu = new MedicalAssessmentMenu();

        while (true) {
            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Patient Management");
            System.out.println("2. Staff Management");
            System.out.println("3. Medical Assessment");
            System.out.println("0. Exit");
            int choice = InputHelper.readInt("Choose an option: ", 0, 3);
            switch (choice) {
                case 1: patientMenu.show(); break;
                case 2: staffMenu.show(); break;
                case 3: assessmentMenu.show(); break;
                case 0:
                    System.out.println("Goodbye.");
                    return;
            }
        }
    }
}
