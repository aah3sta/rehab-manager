package ui;

import dao.StaffDAO;
import models.Staff;
import ui.common.UiHelper;
import ui.tables.ListTableModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Staff management: search, filter by role, register, update, remove, duties. */
public class StaffPanel extends JPanel implements Refreshable {

    private static final String ALL_ROLES = "All roles";
    private static final String[] COMMON_ROLES = {"Doctor", "Therapist", "Nurse", "Receptionist"};

    private final StaffDAO staffDAO = new StaffDAO();

    private final ListTableModel<Staff> model = new ListTableModel<>(
            new String[]{"ID", "Full name", "Role", "Contact"},
            (s, col) -> {
                switch (col) {
                    case 0: return s.getId();
                    case 1: return s.getFullName();
                    case 2: return s.getRole();
                    default: return s.getContact();
                }
            });
    private final JTable table = UiHelper.table(model);

    private final JTextField searchField = UiHelper.hintField("Search by ID, name or contact", 24);
    private final JComboBox<String> roleFilter = new JComboBox<>(new String[]{ALL_ROLES});
    private boolean updatingRoleFilter;
    private final Timer searchDelay = new Timer(300, e -> refresh());

    // form
    private final JLabel formTitle = UiHelper.subheading("New staff member");
    private final JTextField nameField = new JTextField();
    private final JTextField contactField = new JTextField();
    private final JComboBox<String> roleBox = new JComboBox<>(COMMON_ROLES);
    private final JLabel errorLabel = UiHelper.errorLabel();
    private final JButton saveButton = UiHelper.primaryButton("Register staff");
    private final JButton deleteButton = UiHelper.dangerButton("Remove");

    private Staff current;

    public StaffPanel() {
        super(new BorderLayout(0, 12));
        setBorder(UiHelper.padding(20));
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UiHelper.scroll(table), buildForm());
        split.setResizeWeight(0.65);
        split.setBorder(null);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);
        UiHelper.narrowColumn(table, 0);

        roleBox.setEditable(true); // other roles can be typed in
        roleFilter.setPrototypeDisplayValue("Receptionist     ");

        searchDelay.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchDelay.restart(); }
        });
        roleFilter.addActionListener(e -> { if (!updatingRoleFilter) refresh(); });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) showInForm(model.getRow(table.convertRowIndexToModel(viewRow)));
        });

        clearForm();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiHelper.heading("Staff"));
        titles.add(UiHelper.muted("Doctors, therapists, nurses and support staff"));
        header.add(titles, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setOpaque(false);
        JButton newButton = UiHelper.button("New staff member");
        JButton dutiesButton = UiHelper.button("Show duties");
        JButton refreshButton = UiHelper.button("Refresh");
        newButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
            nameField.requestFocusInWindow();
        });
        dutiesButton.addActionListener(e -> showDuties());
        refreshButton.addActionListener(e -> refresh());

        toolbar.add(searchField);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(roleFilter);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(newButton);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(dutiesButton);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(refreshButton);
        header.add(toolbar, BorderLayout.SOUTH);
        return header;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(UiHelper.card());
        form.setBackground(Color.WHITE);
        form.setMinimumSize(new Dimension(280, 0));

        int row = 0;
        UiHelper.addField(form, row++, " ", formTitle);
        UiHelper.addField(form, row++, "Full name", nameField);
        UiHelper.addField(form, row++, "Contact", contactField);
        UiHelper.addField(form, row++, "Role (pick or type)", roleBox);
        UiHelper.addField(form, row++, " ", errorLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(deleteButton);
        UiHelper.addField(form, row++, " ", buttons);
        UiHelper.addFiller(form, row);

        saveButton.addActionListener(e -> save());
        deleteButton.addActionListener(e -> delete());
        return form;
    }

    // ---------- data ----------

    @Override
    public void refresh() {
        final String text = searchField.getText().trim();
        final String role = ALL_ROLES.equals(roleFilter.getSelectedItem()) ? null : (String) roleFilter.getSelectedItem();

        UiHelper.runAsync(this, () -> {
            Result r = new Result();
            r.roles = staffDAO.getRoles();

            Map<Long, Staff> found = new LinkedHashMap<>();
            if (text.isEmpty()) {
                for (Staff s : staffDAO.getAll()) found.put(s.getId(), s);
            } else {
                if (text.matches("\\d{1,9}")) {
                    Staff byId = staffDAO.findById(Long.parseLong(text));
                    if (byId != null) found.put(byId.getId(), byId);
                }
                for (Staff s : staffDAO.search(text, false)) found.put(s.getId(), s);
            }
            // "Display staff by role" combined with the text search
            r.staff = new ArrayList<>();
            for (Staff s : found.values()) {
                if (role == null || s.getRole().equalsIgnoreCase(role)) r.staff.add(s);
            }
            return r;
        }, r -> {
            updateRoleFilter(r.roles, role);
            model.setRows(r.staff);
            reselectCurrent();
            UiHelper.status(r.staff.size() + " staff" + (role == null ? "" : " with role " + role));
        });
    }

    private static class Result {
        List<String> roles;
        List<Staff> staff;
    }

    private void updateRoleFilter(List<String> roles, String selected) {
        updatingRoleFilter = true;
        roleFilter.removeAllItems();
        roleFilter.addItem(ALL_ROLES);
        for (String r : roles) roleFilter.addItem(r);
        roleFilter.setSelectedItem(selected == null ? ALL_ROLES : selected);
        updatingRoleFilter = false;
    }

    private void reselectCurrent() {
        if (current == null) return;
        List<Staff> rows = model.getRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId() == current.getId()) {
                int viewRow = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(viewRow, viewRow);
                return;
            }
        }
    }

    private void save() {
        errorLabel.setText(" ");
        final Staff s;
        try {
            Object role = roleBox.getEditor().getItem();
            // Factory picks Doctor / Therapist / Nurse / Staff from the role
            s = StaffDAO.buildStaff(current == null ? 0 : current.getId(),
                    nameField.getText(), contactField.getText(), role == null ? "" : role.toString());
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(UiHelper.wrap(ex.getMessage()));
            return;
        }

        final boolean isNew = current == null;
        UiHelper.runAsync(this, () -> isNew ? staffDAO.register(s) > 0 : staffDAO.update(s), ok -> {
            if (!ok) {
                UiHelper.showError(this, isNew ? "The staff member could not be registered." : "This staff member no longer exists.");
                return;
            }
            current = s;
            showInForm(s);
            UiHelper.status(isNew ? "Staff member registered with ID " + s.getId() : "Staff #" + s.getId() + " updated");
            refresh();
        });
    }

    /** "Remove when permitted": refused if they conducted assessments. */
    private void delete() {
        if (current == null) return;
        final Staff s = current;
        UiHelper.runAsync(this, () -> staffDAO.countLinkedRecords(s.getId()), linked -> {
            if (linked > 0) {
                UiHelper.showInfo(this, "Can't remove staff member",
                        s.getFullName() + " authored " + linked + " clinical record(s).\n"
                                + "Those records must keep their author, so this staff member can't be removed.");
                return;
            }
            if (!UiHelper.confirm(this, "Remove staff member",
                    "Remove " + s.getFullName() + " (#" + s.getId() + ") from the system?")) return;
            UiHelper.runAsync(this, () -> staffDAO.delete(s.getId()), ok -> {
                clearForm();
                UiHelper.status(ok ? "Staff #" + s.getId() + " removed" : "Staff member was already removed");
                refresh();
            });
        });
    }

    /** Array + runtime polymorphism: each object describes its own duties. */
    private void showDuties() {
        UiHelper.runAsync(this, staffDAO::getDutiesReport, lines -> {
            JTextArea area = new JTextArea(lines.isEmpty() ? "No staff registered." : String.join("\n", lines));
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(UiHelper.padding(8));
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(480, 260));
            JOptionPane.showMessageDialog(this, sp, "Staff duties", JOptionPane.PLAIN_MESSAGE);
        });
    }

    // ---------- form state ----------

    private void showInForm(Staff s) {
        current = s;
        formTitle.setText("Staff #" + s.getId());
        nameField.setText(s.getFullName());
        contactField.setText(s.getContact());
        roleBox.setSelectedItem(s.getRole());
        errorLabel.setText(" ");
        saveButton.setText("Save changes");
        deleteButton.setVisible(true);
    }

    private void clearForm() {
        current = null;
        formTitle.setText("New staff member");
        nameField.setText("");
        contactField.setText("");
        roleBox.setSelectedIndex(0);
        errorLabel.setText(" ");
        saveButton.setText("Register staff");
        deleteButton.setVisible(false);
    }
}
