package ui;

import dao.MedicalAssessmentDAO;
import dao.PatientDAO;
import dao.StaffDAO;
import models.MedicalAssessment;
import models.Patient;
import models.Staff;
import ui.common.UiHelper;
import ui.tables.ListTableModel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Medical assessments: record, view, filter by patient/staff/date, authorized update. */
public class AssessmentPanel extends JPanel implements Refreshable {

    private static final String ALL_PATIENTS = "All patients";
    private static final String ALL_STAFF = "All staff";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MedicalAssessmentDAO assessmentDAO = new MedicalAssessmentDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    // lookups so the table can show names instead of IDs
    private final Map<Long, Patient> patientsById = new HashMap<>();
    private final Map<Long, Staff> staffById = new HashMap<>();

    private final ListTableModel<MedicalAssessment> model = new ListTableModel<>(
            new String[]{"ID", "Date", "Patient", "Conducted by", "Diagnosis"},
            (a, col) -> {
                switch (col) {
                    case 0: return a.getAssessmentId();
                    case 1: return a.getAssessmentDate() == null ? "" : a.getAssessmentDate().format(FMT);
                    case 2: return patientName(a.getPatientId());
                    case 3: return staffName(a.getStaffId());
                    default: return a.getDiagnosis();
                }
            });
    private final JTable table = UiHelper.table(model);

    // filters
    private final JComboBox<Object> patientFilter = new JComboBox<>();
    private final JComboBox<Object> staffFilter = new JComboBox<>();
    private final JCheckBox dateCheck = new JCheckBox("On date");
    private final JSpinner dateFilter = UiHelper.dateSpinner();
    private boolean updatingCombos;

    // form
    private final JLabel formTitle = UiHelper.subheading("New assessment");
    private final JComboBox<Object> patientBox = new JComboBox<>();
    private final JComboBox<Object> staffBox = new JComboBox<>();
    private final JLabel dateLabel = new JLabel();
    private final JTextArea historyArea = textArea(2);
    private final JTextArea notesArea = textArea(3);
    private final JTextField diagnosisField = new JTextField();
    private final JLabel errorLabel = UiHelper.errorLabel();
    private final JButton saveButton = UiHelper.primaryButton("Record assessment");

    private MedicalAssessment current;

    public AssessmentPanel() {
        super(new BorderLayout(0, 12));
        setBorder(UiHelper.padding(20));
        add(buildHeader(), BorderLayout.NORTH);

        // The form scrolls if the window is too short for it
        JScrollPane formScroll = new JScrollPane(buildForm());
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UiHelper.scroll(table), formScroll);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);

        UiHelper.narrowColumn(table, 0);

        UiHelper.setRenderer(patientFilter, this::comboText);
        UiHelper.setRenderer(staffFilter, this::comboText);
        UiHelper.setRenderer(patientBox, this::comboText);
        UiHelper.setRenderer(staffBox, this::comboText);
        patientFilter.setPrototypeDisplayValue("#0000  Xxxxxxxxxxxxxxxxxx");
        staffFilter.setPrototypeDisplayValue("#0000  Xxxxxxxxxxxxxxxxxx");

        patientFilter.addActionListener(e -> { if (!updatingCombos) loadAssessments(); });
        staffFilter.addActionListener(e -> { if (!updatingCombos) loadAssessments(); });
        dateCheck.addActionListener(e -> { dateFilter.setEnabled(dateCheck.isSelected()); loadAssessments(); });
        dateFilter.addChangeListener(e -> { if (dateCheck.isSelected()) loadAssessments(); });
        dateFilter.setEnabled(false);

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
        titles.add(UiHelper.heading("Medical assessments"));
        titles.add(UiHelper.muted("Record assessments and review them by patient, staff member or date"));
        header.add(titles, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setOpaque(false);
        JButton clearFilters = UiHelper.button("Clear filters");
        JButton newButton = UiHelper.button("New assessment");
        clearFilters.addActionListener(e -> {
            updatingCombos = true;
            patientFilter.setSelectedIndex(0);
            staffFilter.setSelectedIndex(0);
            dateCheck.setSelected(false);
            dateFilter.setEnabled(false);
            updatingCombos = false;
            loadAssessments();
        });
        newButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
        });

        toolbar.add(patientFilter);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(staffFilter);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(dateCheck);
        toolbar.add(dateFilter);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(clearFilters);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(newButton);
        header.add(toolbar, BorderLayout.SOUTH);
        return header;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(UiHelper.card());
        form.setBackground(Color.WHITE);
        form.setMinimumSize(new Dimension(300, 0));

        int row = 0;
        UiHelper.addField(form, row++, " ", formTitle);
        UiHelper.addField(form, row++, "Patient", patientBox);
        UiHelper.addField(form, row++, "Conducted by (clinical staff)", staffBox);
        UiHelper.addField(form, row++, "Date", dateLabel);
        UiHelper.addField(form, row++, "Medical history (optional)", UiHelper.textScroll(historyArea));
        UiHelper.addField(form, row++, "Assessment notes", UiHelper.textScroll(notesArea));
        UiHelper.addField(form, row++, "Diagnosis", diagnosisField);
        UiHelper.addField(form, row++, " ", errorLabel);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        UiHelper.addField(form, row++, " ", buttons);
        UiHelper.addFiller(form, row);

        saveButton.addActionListener(e -> save());
        return form;
    }

    private static JTextArea textArea(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    // ---------- data ----------

    /** Reloads patients and staff (for names and combos), then the assessments. */
    @Override
    public void refresh() {
        UiHelper.runAsync(this, () -> new Lookups(patientDAO.getAll(), staffDAO.getAll()), lookups -> {
            patientsById.clear();
            for (Patient p : lookups.patients) patientsById.put(p.getId(), p);
            staffById.clear();
            for (Staff s : lookups.staff) staffById.put(s.getId(), s);
            fillCombos(lookups);
            loadAssessments();
        });
    }

    private static class Lookups {
        final List<Patient> patients;
        final List<Staff> staff;
        Lookups(List<Patient> patients, List<Staff> staff) {
            this.patients = patients;
            this.staff = staff;
        }
    }

    private void fillCombos(Lookups lookups) {
        updatingCombos = true;
        Object pf = patientFilter.getSelectedItem(), sf = staffFilter.getSelectedItem();
        Object pb = patientBox.getSelectedItem(), sb = staffBox.getSelectedItem();

        patientFilter.removeAllItems();
        patientFilter.addItem(ALL_PATIENTS);
        patientBox.removeAllItems();
        for (Patient p : lookups.patients) {
            patientFilter.addItem(p);
            patientBox.addItem(p);
        }

        staffFilter.removeAllItems();
        staffFilter.addItem(ALL_STAFF);
        staffBox.removeAllItems();
        for (Staff s : lookups.staff) {
            staffFilter.addItem(s);
            if (s.canConductAssessment()) staffBox.addItem(s); // only clinical staff can assess
        }

        reselect(patientFilter, pf);
        reselect(staffFilter, sf);
        reselect(patientBox, pb);
        reselect(staffBox, sb);
        updatingCombos = false;
    }

    /** Keeps the same person selected after the combo is rebuilt (matched by ID). */
    private void reselect(JComboBox<Object> combo, Object previous) {
        if (previous == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item.equals(previous) || (item instanceof models.Person && previous instanceof models.Person
                    && ((models.Person) item).getId() == ((models.Person) previous).getId())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadAssessments() {
        final Long patientId = selectedId(patientFilter);
        final Long staffId = selectedId(staffFilter);
        final LocalDate date = dateCheck.isSelected() ? UiHelper.getDate(dateFilter) : null;

        UiHelper.runAsync(this, () -> assessmentDAO.search(patientId, staffId, date), list -> {
            model.setRows(list);
            reselectCurrent();
            StringBuilder msg = new StringBuilder(list.size() + " assessment(s)");
            if (patientId != null) msg.append(" for ").append(patientName(patientId));
            if (staffId != null) msg.append(" by ").append(staffName(staffId));
            if (date != null) msg.append(" on ").append(date);
            UiHelper.status(msg.toString());
        });
    }

    private void reselectCurrent() {
        if (current == null) return;
        List<MedicalAssessment> rows = model.getRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getAssessmentId() == current.getAssessmentId()) {
                int viewRow = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(viewRow, viewRow);
                return;
            }
        }
    }

    private void save() {
        errorLabel.setText(" ");
        if (current == null) recordNew();
        else updateExisting();
    }

    private void recordNew() {
        Object p = patientBox.getSelectedItem();
        Object s = staffBox.getSelectedItem();
        if (!(p instanceof Patient)) { errorLabel.setText("Select a patient."); return; }
        if (!(s instanceof Staff)) { errorLabel.setText(UiHelper.wrap("Select a Doctor, Therapist or Nurse.")); return; }

        final MedicalAssessment a;
        try {
            a = new MedicalAssessment(0, ((Patient) p).getId(), ((Staff) s).getId(), null,
                    historyArea.getText(), notesArea.getText(), diagnosisField.getText());
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(UiHelper.wrap(ex.getMessage()));
            return;
        }
        UiHelper.runAsync(this, () -> assessmentDAO.record(a), id -> {
            if (id <= 0) { UiHelper.showError(this, "The assessment could not be recorded."); return; }
            current = a;
            showInForm(a);
            UiHelper.status("Assessment recorded with ID " + id);
            loadAssessments();
        });
    }

    /** "Update when authorized": only the original assessor or a Doctor may edit. */
    private void updateExisting() {
        final MedicalAssessment a = current;
        final String history = historyArea.getText();
        final String notes = notesArea.getText();
        final String diagnosis = diagnosisField.getText();
        try {
            // validate first, on a copy, so a bad value doesn't change the shown record
            new MedicalAssessment(a.getAssessmentId(), a.getPatientId(), a.getStaffId(),
                    a.getAssessmentDate(), history, notes, diagnosis);
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(UiHelper.wrap(ex.getMessage()));
            return;
        }

        Staff editor = askWhoIsEditing();
        if (editor == null) return;
        if (!util.Authorization.canEdit(editor, a.getStaffId())) {
            UiHelper.showInfo(this, "Not authorized",
                    "Only " + staffName(a.getStaffId()) + " (who conducted this assessment) or a Doctor can update it.");
            return;
        }

        a.setMedicalHistory(history);
        a.setAssessmentNotes(notes);
        a.setDiagnosis(diagnosis);
        UiHelper.runAsync(this, () -> assessmentDAO.update(a), ok -> {
            UiHelper.status(ok ? "Assessment #" + a.getAssessmentId() + " updated by " + editor.getFullName()
                    : "Assessment no longer exists");
            loadAssessments();
        });
    }

    private Staff askWhoIsEditing() {
        JComboBox<Object> who = new JComboBox<>();
        for (Staff s : staffById.values()) who.addItem(s);
        UiHelper.setRenderer(who, this::comboText);
        Object[] message = {"Who is making this change?", who};
        int choice = JOptionPane.showConfirmDialog(this, message, "Confirm your identity",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.OK_OPTION ? (Staff) who.getSelectedItem() : null;
    }

    // ---------- form state ----------

    private void showInForm(MedicalAssessment a) {
        current = a;
        formTitle.setText("Assessment #" + a.getAssessmentId());
        selectById(patientBox, a.getPatientId());
        selectById(staffBox, a.getStaffId());
        patientBox.setEnabled(false);   // patient and assessor can't be changed after recording
        staffBox.setEnabled(false);
        dateLabel.setText(a.getAssessmentDate() == null ? "-" : a.getAssessmentDate().format(FMT));
        historyArea.setText(a.getMedicalHistory() == null ? "" : a.getMedicalHistory());
        notesArea.setText(a.getAssessmentNotes());
        diagnosisField.setText(a.getDiagnosis());
        errorLabel.setText(" ");
        saveButton.setText("Save changes");
    }

    private void clearForm() {
        current = null;
        formTitle.setText("New assessment");
        patientBox.setEnabled(true);
        staffBox.setEnabled(true);
        dateLabel.setText("Now (set when saved)");
        historyArea.setText("");
        notesArea.setText("");
        diagnosisField.setText("");
        errorLabel.setText(" ");
        saveButton.setText("Record assessment");
    }

    private void selectById(JComboBox<Object> combo, long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item instanceof models.Person && ((models.Person) item).getId() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1); // e.g. assessor's role changed since
    }

    // ---------- display helpers ----------

    private Long selectedId(JComboBox<Object> combo) {
        Object item = combo.getSelectedItem();
        return item instanceof models.Person ? ((models.Person) item).getId() : null;
    }

    private String comboText(Object item) {
        if (item instanceof Staff) {
            Staff s = (Staff) item;
            return "#" + s.getId() + "  " + s.getFullName() + " (" + s.getRole() + ")";
        }
        if (item instanceof Patient) {
            Patient p = (Patient) item;
            return "#" + p.getId() + "  " + p.getFullName();
        }
        return item == null ? "" : item.toString();
    }

    private String patientName(long id) {
        Patient p = patientsById.get(id);
        return p == null ? "#" + id : p.getFullName();
    }

    private String staffName(long id) {
        Staff s = staffById.get(id);
        return s == null ? "#" + id : s.getFullName();
    }
}
