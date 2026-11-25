import database.DatabaseConnection;
import menus.*;
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
        LabTestMenu labTestMenu = new LabTestMenu();
        VitalsMenu vitalsMenu = new VitalsMenu();
        TherapySessionMenu therapyMenu = new TherapySessionMenu();
        MedicationMenu medicationMenu = new MedicationMenu();
        DischargePlanMenu dischargeMenu = new DischargePlanMenu();

        while (true) {
            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Patient Management");
            System.out.println("2. Staff Management");
            System.out.println("3. Medical Assessment");
            System.out.println("4. Laboratory Tests");
            System.out.println("5. Vitals");
            System.out.println("6. Therapy Sessions");
            System.out.println("7. Medication");
            System.out.println("8. Discharge and Aftercare");
            System.out.println("0. Exit");
            int choice = InputHelper.readInt("Choose an option: ", 0, 8);
            switch (choice) {
                case 1: patientMenu.show(); break;
                case 2: staffMenu.show(); break;
                case 3: assessmentMenu.show(); break;
                case 4: labTestMenu.show(); break;
                case 5: vitalsMenu.show(); break;
                case 6: therapyMenu.show(); break;
                case 7: medicationMenu.show(); break;
                case 8: dischargeMenu.show(); break;
                case 0:
                    System.out.println("Goodbye.");
                    return;
            }
        }
    }
}
