package menus;

import dao.DischargePlanDAO;
import models.DischargePlan;
import models.Patient;
import util.InputHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DischargePlanMenu {
    private final DischargePlanDAO planDAO = new DischargePlanDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== DISCHARGE AND AFTERCARE =====");
            System.out.println("1. Create a discharge and aftercare plan");
            System.out.println("2. Display a plan by plan ID");
            System.out.println("3. Display plans for a patient");
            System.out.println("4. Update recovery goals, aftercare instructions or emergency contact");
            System.out.println("5. Record or update discharge and follow-up dates");
            System.out.println("6. View upcoming follow-ups and emergency contacts");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 6);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: create(); break;
                    case 2: displayById(); break;
                    case 3: displayByPatient(); break;
                    case 4: updateCarePlan(); break;
                    case 5: updateDates(); break;
                    case 6: upcomingFollowUps(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void create() throws SQLException {
        System.out.println("\n-- Create Discharge and Aftercare Plan --");
        Patient p = MenuSupport.askPatient();
        if (p == null) return;

        DischargePlan plan = new DischargePlan(0, p.getId(), "x", "x", null, null,
                "Temp", "0000000000", null);
        InputHelper.readValid("Recovery goals: ", plan::setRecoveryGoals);
        InputHelper.readValid("Aftercare instructions: ", plan::setAftercareInstructions);
        readEmergencyContact(plan);
        readDates(plan);

        long id = planDAO.create(plan);
        System.out.println(id > 0 ? "Plan created with ID " + id + " (" + plan.getStatus() + ")." : "Creation failed.");
    }

    private void displayById() throws SQLException {
        long id = InputHelper.readId("Enter plan ID: ");
        DischargePlan plan = planDAO.findById(id);
        if (plan == null) {
            System.out.println("No plan found with ID " + id + ".");
            return;
        }
        System.out.println("Patient: " + MenuSupport.patientName(plan.getPatientId()));
        System.out.println(plan.getSummary());
    }

    private void displayByPatient() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        MenuSupport.printList("Plans for " + p.getFullName(), planDAO.findByPatient(p.getId()),
                DischargePlan::getSummary);
    }

    private void updateCarePlan() throws SQLException {
        DischargePlan plan = askPlan();
        if (plan == null) return;
        System.out.println("Press Enter to keep the current value.");
        InputHelper.readValid("Recovery goals [" + plan.getRecoveryGoals() + "]: ",
                v -> plan.setRecoveryGoals(v.isEmpty() ? plan.getRecoveryGoals() : v));
        InputHelper.readValid("Aftercare instructions [" + plan.getAftercareInstructions() + "]: ",
                v -> plan.setAftercareInstructions(v.isEmpty() ? plan.getAftercareInstructions() : v));
        if (InputHelper.confirm("Change the emergency relapse contact?")) readEmergencyContact(plan);

        if (!InputHelper.confirm("Save these changes?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(planDAO.updateCarePlan(plan) ? "Plan updated." : "Update failed.");
    }

    private void updateDates() throws SQLException {
        DischargePlan plan = askPlan();
        if (plan == null) return;
        readDates(plan);
        if (!InputHelper.confirm("Save these dates?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(planDAO.updateDates(plan) ? "Dates updated (" + plan.getStatus() + ")." : "Update failed.");
    }

    private void upcomingFollowUps() throws SQLException {
        List<DischargePlan> plans = planDAO.findUpcomingFollowUps();
        System.out.println("\n-- Upcoming follow-ups (" + plans.size() + ") --");
        if (plans.isEmpty()) {
            System.out.println("No follow-ups scheduled from today onwards.");
            return;
        }
        LocalDate today = LocalDate.now();
        for (DischargePlan plan : plans) {
            long days = ChronoUnit.DAYS.between(today, plan.getFollowUpDate());
            String when = days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days";
            System.out.printf("%s (%s) | Plan #%d | %s%n   Emergency relapse contact: %s, %s%n",
                    plan.getFollowUpDate(), when, plan.getPlanId(), MenuSupport.patientName(plan.getPatientId()),
                    plan.getEmergencyContactName(), plan.getEmergencyContactPhone());
        }
    }

    // ---- helpers ----

    private DischargePlan askPlan() throws SQLException {
        long id = InputHelper.readId("Enter plan ID: ");
        DischargePlan plan = planDAO.findById(id);
        if (plan == null) {
            System.out.println("No plan found with ID " + id + ".");
            return null;
        }
        System.out.println("Patient: " + MenuSupport.patientName(plan.getPatientId()));
        System.out.println(plan.getSummary());
        return plan;
    }

    private void readEmergencyContact(DischargePlan plan) {
        while (true) {
            try {
                String name = InputHelper.readLine("Emergency relapse contact name: ");
                String phone = InputHelper.readLine("Emergency relapse contact phone: ");
                plan.setEmergencyContact(name, phone);
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private void readDates(DischargePlan plan) {
        while (true) {
            try {
                LocalDate discharge = InputHelper.readNullableDate("Discharge date", plan.getDischargeDate());
                LocalDate followUp = InputHelper.readNullableDate("Follow-up date", plan.getFollowUpDate());
                plan.setDates(discharge, followUp);
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }
}
