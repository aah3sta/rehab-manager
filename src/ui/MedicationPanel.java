package ui;

import dao.MedicationDAO;
import models.MedicationRecord;
import models.Patient;
import models.Staff;
import ui.common.UiHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;

/** Medication: record, history per patient, search by name or staff, authorized updates. */
public class MedicationPanel extends ModulePanel<MedicationRecord> {

    private final MedicationDAO medicationDAO = new MedicationDAO();

    // filters
    private final JTextField nameSearch = UiHelper.hintField("Search medication name", 18);
    private final JComboBox<Object> patientFilter = personCombo();
    private final JComboBox<Object> staffFilter = personCombo();
    private final Timer searchDelay = new Timer(300, e -> reload());

    // form
    private final JComboBox<Object> patientBox = personCombo();
    private final JComboBox<Object> staffBox = personCombo();
    private final JTextField nameField = new JTextField();
    private final JTextField dosageField = new JTextField();
    private final JTextField frequencyField = new JTextField();
    private final JTextArea instructionsArea = textArea(3);

    public MedicationPanel() {
        super("Medication", "Medication, dosage and instructions for each patient",
                new String[]{"ID", "Date", "Patient", "Medication", "Dosage", "Frequency", "Responsible"}, null);
        model.setCellValue((m, col) -> {
            switch (col) {
                case 0: return m.getRecordId();
                case 1: return m.getRecordedDate() == null ? "" : m.getRecordedDate().format(TABLE_DATE);
                case 2: return patientName(m.getPatientId());
                case 3: return m.getMedicationName();
                case 4: return m.getDosage();
                case 5: return m.getFrequency();
                default: return staffName(m.getStaffId());
            }
        });

        UiHelper.columnWidths(table, 40, 115, 110, 110, 70, 120, 110);

        addTool(nameSearch);
        addTool(patientFilter);
        addTool(staffFilter);
        addNewButton("New medication");
        reloadOnChange(patientFilter);
        reloadOnChange(staffFilter);
        searchDelay.setRepeats(false);
        nameSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchDelay.restart(); }
        });

        addField("Patient", patientBox);
        addField("Responsible (doctor or nurse)", staffBox);
        addField("Medication name", nameField);
        addField(" ", UiHelper.pair(UiHelper.labeled("Dosage (e.g. 500 mg)", dosageField),
                UiHelper.labeled("Frequency (e.g. Twice daily)", frequencyField)));
        addField("Instructions (optional)", UiHelper.textScroll(instructionsArea));
        finishForm();
    }

    // ---------- data ----------

    @Override
    protected void lookupsLoaded(Object extra) {
        fillPatients(patientFilter, true);
        fillStaff(staffFilter, true, Staff::canManageMedication);
        fillPatients(patientBox, false);
        fillStaff(staffBox, false, Staff::canManageMedication);
    }

    @Override
    protected UiHelper.DbTask<List<MedicationRecord>> query() {
        final Long patientId = selectedId(patientFilter);
        final Long staffId = selectedId(staffFilter);
        final String name = nameSearch.getText();
        return () -> medicationDAO.search(patientId, staffId, name);
    }

    @Override
    protected void rowsLoaded(List<MedicationRecord> rows) {
        Long patientId = selectedId(patientFilter);
        summaryLabel.setText(patientId != null
                ? "Medication history for " + patientName(patientId) + ": " + rows.size() + " record(s)"
                : rows.size() + " medication record(s)");
        UiHelper.status(rows.size() + " medication record(s)");
    }

    @Override
    protected long idOf(MedicationRecord m) { return m.getRecordId(); }

    // ---------- form ----------

    @Override
    protected void showInForm(MedicationRecord m) {
        current = m;
        formTitle.setText("Medication #" + m.getRecordId());
        selectById(patientBox, m.getPatientId());
        if (!selectById(staffBox, m.getStaffId())) staffBox.setSelectedIndex(-1);
        patientBox.setEnabled(false);  // patient and responsible staff stay fixed after recording
        staffBox.setEnabled(false);
        nameField.setText(m.getMedicationName());
        dosageField.setText(m.getDosage());
        frequencyField.setText(m.getFrequency());
        instructionsArea.setText(m.getInstructions() == null ? "" : m.getInstructions());
        saveButton.setText("Save changes");
    }

    @Override
    protected void clearForm() {
        current = null;
        formTitle.setText("New medication record");
        patientBox.setEnabled(true);
        staffBox.setEnabled(true);
        Long filtered = selectedId(patientFilter);
        if (filtered != null) selectById(patientBox, filtered);
        nameField.setText("");
        dosageField.setText("");
        frequencyField.setText("");
        instructionsArea.setText("");
        saveButton.setText("Record medication");
    }

    @Override
    protected void save() {
        if (current == null) recordNew();
        else updateExisting();
    }

    private void recordNew() {
        Object p = patientBox.getSelectedItem();
        Object s = staffBox.getSelectedItem();
        if (!(p instanceof Patient)) { showError("Select a patient."); return; }
        if (!(s instanceof Staff)) { showError("Select the responsible doctor or nurse."); return; }
        final MedicationRecord m;
        try {
            m = new MedicationRecord(0, ((Patient) p).getId(), ((Staff) s).getId(), nameField.getText(),
                    dosageField.getText(), frequencyField.getText(), instructionsArea.getText(), null);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        UiHelper.runAsync(this, () -> medicationDAO.record(m), id -> {
            current = m;
            UiHelper.status("Medication recorded with ID " + id);
            reload();
        });
    }

    /** "Update when authorized": the responsible staff member or a Doctor. */
    private void updateExisting() {
        final MedicationRecord old = current;
        final MedicationRecord edited;
        try {
            edited = new MedicationRecord(old.getRecordId(), old.getPatientId(), old.getStaffId(),
                    nameField.getText(), dosageField.getText(), frequencyField.getText(),
                    instructionsArea.getText(), old.getRecordedDate());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        if (!authorize(old.getStaffId())) return;
        UiHelper.runAsync(this, () -> medicationDAO.update(edited), ok -> {
            current = edited;
            UiHelper.status("Medication record #" + edited.getRecordId() + " updated");
            reload();
        });
    }
}
