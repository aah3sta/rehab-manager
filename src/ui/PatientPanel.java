package ui;

import dao.PatientDAO;
import models.Patient;
import ui.common.UiHelper;
import ui.tables.ListTableModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Patient management: search, list, register, update and delete. */
public class PatientPanel extends JPanel implements Refreshable {

    private final PatientDAO patientDAO = new PatientDAO();

    private final ListTableModel<Patient> model = new ListTableModel<>(
            new String[]{"ID", "Full name", "Gender", "Age", "Contact", "Registered"},
            (p, col) -> {
                switch (col) {
                    case 0: return p.getId();
                    case 1: return p.getFullName();
                    case 2: return UiHelper.capitalize(p.getGender());
                    case 3: return p.calculateAge();
                    case 4: return p.getContact();
                    default: return p.getRegistrationDate() == null ? ""
                            : p.getRegistrationDate().toLocalDate().toString();
                }
            });
    private final JTable table = UiHelper.table(model);

    private final JTextField searchField = UiHelper.hintField("Search by ID, name or contact", 28);
    private final Timer searchDelay = new Timer(300, e -> refresh());

    // form
    private final JLabel formTitle = UiHelper.subheading("New patient");
    private final JTextField nameField = new JTextField();
    private final JComboBox<String> genderBox = new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER"});
    private final JSpinner dobSpinner = UiHelper.dateSpinner();
    private final JTextField contactField = new JTextField();
    private final JLabel errorLabel = UiHelper.errorLabel();
    private final JButton saveButton = UiHelper.primaryButton("Register patient");
    private final JButton deleteButton = UiHelper.dangerButton("Delete");

    /** The patient shown in the form; null while registering a new one. */
    private Patient current;

    public PatientPanel() {
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

        searchDelay.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchDelay.restart(); }
        });

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
        titles.add(UiHelper.heading("Patients"));
        titles.add(UiHelper.muted("Register, find and update patient records"));
        header.add(titles, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setOpaque(false);
        JButton newButton = UiHelper.button("New patient");
        JButton refreshButton = UiHelper.button("Refresh");
        newButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
            nameField.requestFocusInWindow();
        });
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(searchField);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(newButton);
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
        UiHelper.addField(form, row++, "Gender", genderBox);
        UiHelper.addField(form, row++, "Date of birth", dobSpinner);
        UiHelper.addField(form, row++, "Contact", contactField);
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
        UiHelper.runAsync(this, () -> {
            if (text.isEmpty()) return patientDAO.getAll();
            // Short numbers are treated as an ID and also matched against contacts
            Map<Long, Patient> found = new LinkedHashMap<>();
            if (text.matches("\\d{1,9}")) {
                Patient byId = patientDAO.findById(Long.parseLong(text));
                if (byId != null) found.put(byId.getId(), byId);
            }
            for (Patient p : patientDAO.search(text)) found.put(p.getId(), p);
            return new ArrayList<>(found.values());
        }, list -> {
            model.setRows(list);
            reselectCurrent();
            UiHelper.status(list.size() + (text.isEmpty() ? " patients" : " patients match \"" + text + "\""));
        });
    }

    private void reselectCurrent() {
        if (current == null) return;
        List<Patient> rows = model.getRows();
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
        final Patient p;
        try {
            // The model's setters do all the validation
            p = new Patient(current == null ? 0 : current.getId(),
                    nameField.getText(), (String) genderBox.getSelectedItem(),
                    UiHelper.getDate(dobSpinner), contactField.getText(),
                    current == null ? null : current.getRegistrationDate());
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(UiHelper.wrap(ex.getMessage()));
            return;
        }

        final boolean isNew = current == null;
        UiHelper.runAsync(this, () -> isNew ? patientDAO.register(p) > 0 : patientDAO.update(p), ok -> {
            if (!ok) {
                UiHelper.showError(this, isNew ? "The patient could not be registered." : "The patient no longer exists.");
                return;
            }
            current = p;
            showInForm(p);
            UiHelper.status(isNew ? "Patient registered with ID " + p.getId() : "Patient #" + p.getId() + " updated");
            refresh();
        });
    }

    /** "Delete when permitted": refused if the patient has medical assessments. */
    private void delete() {
        if (current == null) return;
        final Patient p = current;
        UiHelper.runAsync(this, () -> patientDAO.countLinkedRecords(p.getId()), linked -> {
            if (linked > 0) {
                UiHelper.showInfo(this, "Can't delete patient",
                        p.getFullName() + " has " + linked + " clinical record(s) on record\n(assessments, vitals, therapy, medication or plans).\n"
                                + "Clinical history must be kept, so this patient can't be deleted.");
                return;
            }
            if (!UiHelper.confirm(this, "Delete patient",
                    "Permanently delete " + p.getFullName() + " (#" + p.getId() + ")?")) return;
            UiHelper.runAsync(this, () -> patientDAO.delete(p.getId()), ok -> {
                clearForm();
                UiHelper.status(ok ? "Patient #" + p.getId() + " deleted" : "Patient was already removed");
                refresh();
            });
        });
    }

    // ---------- form state ----------

    private void showInForm(Patient p) {
        current = p;
        formTitle.setText("Patient #" + p.getId());
        nameField.setText(p.getFullName());
        genderBox.setSelectedItem(p.getGender());
        UiHelper.setDate(dobSpinner, p.getDateOfBirth());
        contactField.setText(p.getContact());
        errorLabel.setText(" ");
        saveButton.setText("Save changes");
        deleteButton.setVisible(true);
    }

    private void clearForm() {
        current = null;
        formTitle.setText("New patient");
        nameField.setText("");
        genderBox.setSelectedIndex(0);
        UiHelper.setDate(dobSpinner, LocalDate.now().minusYears(30));
        contactField.setText("");
        errorLabel.setText(" ");
        saveButton.setText("Register patient");
        deleteButton.setVisible(false);
    }
}
