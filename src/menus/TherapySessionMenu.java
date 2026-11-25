package menus;

import dao.TherapySessionDAO;
import models.Patient;
import models.Staff;
import models.TherapySession;
import util.InputHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TherapySessionMenu {
    private final TherapySessionDAO sessionDAO = new TherapySessionDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== THERAPY SESSIONS =====");
            System.out.println("1. Record a therapy session");
            System.out.println("2. Display a session by session ID");
            System.out.println("3. Display a patient's therapy history");
            System.out.println("4. Display sessions by staff member");
            System.out.println("5. Display sessions on a date");
            System.out.println("6. Calculate a patient's total therapy time");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 6);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: record(); break;
                    case 2: displayById(); break;
                    case 3: displayByPatient(); break;
                    case 4: displayByStaff(); break;
                    case 5: displayByDate(); break;
                    case 6: totalTime(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void record() throws SQLException {
        System.out.println("\n-- Record Therapy Session --");
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        Staff s = MenuSupport.askStaffWithRole("Therapist ID: ", Staff::canConductTherapy, "a Therapist");
        if (s == null) return;

        TherapySession t = new TherapySession(0, p.getId(), s.getId(),
                java.time.LocalDateTime.now().minusMinutes(1), 60, "pending");
        while (true) {
            try {
                t.setSessionDate(InputHelper.readDateTime("Session date and time"));
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
        InputHelper.readValid("Duration (minutes): ",
                x -> t.setDurationMinutes(InputHelper.parseInt(x, "Duration")));
        InputHelper.readValid("Session notes: ", t::setNotes);

        long id = sessionDAO.record(t);
        System.out.println(id > 0 ? "Therapy session recorded with ID " + id + "." : "Recording failed.");
    }

    private void displayById() throws SQLException {
        long id = InputHelper.readId("Enter session ID: ");
        TherapySession t = sessionDAO.findById(id);
        System.out.println(t != null ? t.getSummary() : "No therapy session found with ID " + id + ".");
    }

    private void displayByPatient() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        List<TherapySession> sessions = sessionDAO.findByPatient(p.getId());
        MenuSupport.printList("Therapy history for " + p.getFullName(), sessions, TherapySession::getSummary);
        if (!sessions.isEmpty()) {
            System.out.println("Total: " + TherapySession.formatMinutes(sessionDAO.totalMinutes(p.getId())));
        }
    }

    private void displayByStaff() throws SQLException {
        Staff s = MenuSupport.askStaff("Enter staff ID: ");
        if (s == null) return;
        MenuSupport.printList("Sessions conducted by " + s.getFullName(), sessionDAO.findByStaff(s.getId()),
                TherapySession::getSummary);
    }

    private void displayByDate() throws SQLException {
        LocalDate date = InputHelper.readDate("Session date");
        MenuSupport.printList("Sessions on " + date, sessionDAO.findByDate(date), TherapySession::getSummary);
    }

    private void totalTime() throws SQLException {
        Patient p = MenuSupport.askPatient();
        if (p == null) return;
        long minutes = sessionDAO.totalMinutes(p.getId());
        int count = sessionDAO.findByPatient(p.getId()).size();
        System.out.println(p.getFullName() + " has " + count + " recorded session(s), totalling "
                + TherapySession.formatMinutes(minutes) + " (" + minutes + " minutes).");
    }
}
