package menus;

import dao.StaffDAO;
import models.Staff;
import util.InputHelper;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public class StaffMenu {
    private final StaffDAO staffDAO = new StaffDAO();

    public void show() {
        while (true) {
            System.out.println("\n===== STAFF MANAGEMENT =====");
            System.out.println("1. Register a staff member");
            System.out.println("2. Display all staff");
            System.out.println("3. Search staff by ID");
            System.out.println("4. Search staff by name or contact");
            System.out.println("5. Display staff by role");
            System.out.println("6. Update staff record");
            System.out.println("7. Remove staff record");
            System.out.println("8. Show duties of all staff");
            System.out.println("0. Back to main menu");
            int choice = InputHelper.readInt("Choose an option: ", 0, 8);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: register(); break;
                    case 2: displayAll(); break;
                    case 3: searchById(); break;
                    case 4: searchByKeyword(); break;
                    case 5: displayByRole(); break;
                    case 6: update(); break;
                    case 7: delete(); break;
                    case 8: staffDAO.showAllDuties(); break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private void register() throws SQLException {
        System.out.println("\n-- Register Staff Member --");
        Staff temp = new Staff(0, "Temp", "0000000000", "Temp");
        InputHelper.readValid("Full name: ", temp::setFullName);
        InputHelper.readValid("Contact: ", temp::setContact);
        InputHelper.readValid("Role (e.g. Doctor, Therapist, Nurse, Receptionist): ", temp::setRole);

        // Rebuild through the factory so a "Doctor" becomes a Doctor object
        Staff s = StaffDAO.buildStaff(0, temp.getFullName(), temp.getContact(), temp.getRole());
        long id = staffDAO.register(s);
        if (id > 0) System.out.println("Staff member registered successfully with ID " + id + ".");
        else System.out.println("Registration failed.");
    }

    private void displayAll() throws SQLException {
        List<Staff> staff = staffDAO.getAll();
        System.out.println("\n-- All Staff (" + staff.size() + ") --");
        printList(staff);
    }

    private void searchById() throws SQLException {
        long id = InputHelper.readId("Enter staff ID: ");
        Staff s = staffDAO.findById(id);
        System.out.println(s != null ? s.getDetails() : "No staff member found with ID " + id + ".");
    }

    private void searchByKeyword() throws SQLException {
        String keyword = InputHelper.readRequired("Enter name or contact (partial is fine): ");
        List<Staff> results = staffDAO.search(keyword, false);
        System.out.println("\n-- " + results.size() + " result(s) --");
        printList(results);
    }

    private void displayByRole() throws SQLException {
        List<String> roles = staffDAO.getRoles();
        if (!roles.isEmpty()) System.out.println("Roles on record: " + String.join(", ", roles));
        String role = InputHelper.readRequired("Enter role: ");
        List<Staff> results = staffDAO.search(role, true);
        System.out.println("\n-- " + results.size() + " staff with role '" + role + "' --");
        printList(results);
    }

    private void update() throws SQLException {
        long id = InputHelper.readId("Enter ID of the staff member to update: ");
        Staff s = staffDAO.findById(id);
        if (s == null) {
            System.out.println("No staff member found with ID " + id + ".");
            return;
        }
        System.out.println("Current: " + s.getDetails());
        System.out.println("Press Enter to keep the current value.");

        InputHelper.readValid("Full name [" + s.getFullName() + "]: ",
                v -> s.setFullName(v.isEmpty() ? s.getFullName() : v));
        InputHelper.readValid("Contact [" + s.getContact() + "]: ",
                v -> s.setContact(v.isEmpty() ? s.getContact() : v));
        InputHelper.readValid("Role [" + s.getRole() + "]: ",
                v -> s.setRole(v.isEmpty() ? s.getRole() : v));

        if (!InputHelper.confirm("Save these changes?")) {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println(staffDAO.update(s) ? "Staff record updated successfully." : "Update failed.");
    }

    private void delete() throws SQLException {
        long id = InputHelper.readId("Enter ID of the staff member to remove: ");
        Staff s = staffDAO.findById(id);
        if (s == null) {
            System.out.println("No staff member found with ID " + id + ".");
            return;
        }
        int linked = staffDAO.countLinkedRecords(id);
        if (linked > 0) {
            System.out.println("Cannot remove: this staff member conducted " + linked
                    + " medical assessment(s). Those records must keep their author.");
            return;
        }
        System.out.println(s.getDetails());
        if (!InputHelper.confirm("Are you sure you want to remove this staff member?")) {
            System.out.println("Removal cancelled.");
            return;
        }
        try {
            System.out.println(staffDAO.delete(id) ? "Staff member removed." : "Removal failed.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Cannot remove: this staff member is referenced by other records.");
        }
    }

    private void printList(List<Staff> staff) {
        if (staff.isEmpty()) {
            System.out.println("No staff found.");
            return;
        }
        for (Staff s : staff) System.out.println(s.getDetails());
    }
}
