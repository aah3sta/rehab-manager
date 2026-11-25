package ui;

import dao.LabTestDAO;
import dao.MedicalAssessmentDAO;
import models.LabTest;
import models.MedicalAssessment;
import ui.common.UiHelper;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lab tests: recorded under an assessment, viewed by assessment or patient, updated when authorized. */
public class LabTestPanel extends ModulePanel<LabTest> {

    private static final String ALL_ASSESSMENTS = "All assessments";
    private static final String[] STATUSES = {"All statuses", "Pending", "Completed"};

    private final LabTestDAO labDAO = new LabTestDAO();
    private final MedicalAssessmentDAO assessmentDAO = new MedicalAssessmentDAO();
    private final Map<Long, MedicalAssessment> assessmentsById = new LinkedHashMap<>();

    // filters
    private final JComboBox<Object> patientFilter = personCombo();
    private final JComboBox<Object> assessmentFilter = assessmentCombo();
    private final JComboBox<String> statusFilter = new JComboBox<>(STATUSES);

    // form
    private final JComboBox<Object> assessmentBox = assessmentCombo();
    private final JTextField testNameField = new JTextField();
    private final JTextField resultField = new JTextField();
    private final JTextArea notesArea = textArea(3);

    public LabTestPanel() {
        super("Laboratory tests", "Tests ordered under a medical assessment, with their results",
                new String[]{"ID", "Date", "Patient", "Assessment", "Test", "Status", "Result"}, null);
        // (cells are set below because they use this panel's lookups)
        model.setCellValue((t, col) -> {
            MedicalAssessment a = assessmentsById.get(t.getAssessmentId());
            switch (col) {
                case 0: return t.getTestId();
                case 1: return t.getTestDate() == null ? "" : t.getTestDate().format(TABLE_DATE);
                case 2: return a == null ? "" : patientName(a.getPatientId());
                case 3: return "#" + t.getAssessmentId() + (a == null ? "" : "  " + a.getDiagnosis());
                case 4: return t.getTestName();
                case 5: return t.getStatus();
                default: return t.getResult() == null ? "" : t.getResult();
            }
        });

        UiHelper.columnWidths(table, 40, 115, 110, 130, 120, 75, 140);

        addTool(patientFilter);
        addTool(assessmentFilter);
        addTool(statusFilter);
        addNewButton("New lab test");
        patientFilter.addActionListener(e -> {
            if (updatingCombos) return;
            updatingCombos = true;
            fillAssessments(assessmentFilter, true, selectedId(patientFilter)); // only that patient's assessments
            updatingCombos = false;
            reload();
        });
        reloadOnChange(assessmentFilter);
        statusFilter.addActionListener(e -> reload());

        addField("Assessment", assessmentBox);
        addField("Test name", testNameField);
        addField("Result (leave empty while pending)", resultField);
        addField("Notes (optional)", UiHelper.textScroll(notesArea));
        finishForm();
    }

    // ---------- data ----------

    @Override
    protected Object loadExtraLookups() throws Exception {
        return assessmentDAO.getAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void lookupsLoaded(Object extra) {
        assessmentsById.clear();
        for (MedicalAssessment a : (List<MedicalAssessment>) extra) assessmentsById.put(a.getAssessmentId(), a);
        fillPatients(patientFilter, true);
        fillAssessments(assessmentFilter, true, selectedId(patientFilter));
        fillAssessments(assessmentBox, false, null);
    }

    @Override
    protected UiHelper.DbTask<List<LabTest>> query() {
        final Long patientId = selectedId(patientFilter);
        final Long assessmentId = selectedAssessmentId(assessmentFilter);
        final String status = (String) statusFilter.getSelectedItem();
        return () -> {
            List<LabTest> list = assessmentId != null ? labDAO.findByAssessment(assessmentId)
                    : patientId != null ? labDAO.findByPatient(patientId)   // through the patient's assessments
                    : labDAO.getAll();
            if (!STATUSES[0].equals(status)) {
                List<LabTest> filtered = new ArrayList<>();
                for (LabTest t : list) if (t.getStatus().equals(status)) filtered.add(t);
                return filtered;
            }
            return list;
        };
    }

    @Override
    protected void rowsLoaded(List<LabTest> rows) {
        int pending = 0;
        for (LabTest t : rows) if (t.isPending()) pending++;
        summaryLabel.setText(rows.size() + " test(s), " + pending + " awaiting results");
        UiHelper.status(rows.size() + " lab test(s)");
    }

    @Override
    protected long idOf(LabTest t) { return t.getTestId(); }

    // ---------- form ----------

    @Override
    protected void showInForm(LabTest t) {
        current = t;
        formTitle.setText("Lab test #" + t.getTestId() + "  ·  " + t.getStatus());
        selectAssessment(assessmentBox, t.getAssessmentId());
        assessmentBox.setEnabled(false); // a test stays under its assessment
        testNameField.setText(t.getTestName());
        resultField.setText(t.getResult() == null ? "" : t.getResult());
        notesArea.setText(t.getNotes() == null ? "" : t.getNotes());
        saveButton.setText("Save changes");
    }

    @Override
    protected void clearForm() {
        current = null;
        formTitle.setText("New lab test");
        assessmentBox.setEnabled(true);
        // default to the assessment chosen in the filter, if any
        Long filtered = selectedAssessmentId(assessmentFilter);
        if (filtered != null) selectAssessment(assessmentBox, filtered);
        testNameField.setText("");
        resultField.setText("");
        notesArea.setText("");
        saveButton.setText("Record test");
    }

    @Override
    protected void save() {
        if (current == null) recordNew();
        else updateExisting();
    }

    private void recordNew() {
        Long assessmentId = selectedAssessmentId(assessmentBox);
        if (assessmentId == null) {
            showError("Select the assessment this test belongs to.");
            return;
        }
        final LabTest t;
        try {
            t = new LabTest(0, assessmentId, testNameField.getText(), null, resultField.getText(), notesArea.getText());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        UiHelper.runAsync(this, () -> labDAO.record(t), id -> {
            current = t;
            UiHelper.status("Lab test recorded with ID " + id + " (" + t.getStatus() + ")");
            reload();
        });
    }

    /** "Update when authorized": whoever conducted the assessment, or a Doctor. */
    private void updateExisting() {
        final LabTest t = current;
        final LabTest edited;
        try {
            edited = new LabTest(t.getTestId(), t.getAssessmentId(), testNameField.getText(), t.getTestDate(),
                    resultField.getText(), notesArea.getText());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        MedicalAssessment a = assessmentsById.get(t.getAssessmentId());
        if (a == null || !authorize(a.getStaffId())) return;
        UiHelper.runAsync(this, () -> labDAO.update(edited), ok -> {
            current = edited;
            UiHelper.status("Lab test #" + edited.getTestId() + " updated (" + edited.getStatus() + ")");
            reload();
        });
    }

    // ---------- assessment combos ----------

    private JComboBox<Object> assessmentCombo() {
        JComboBox<Object> c = new JComboBox<>();
        UiHelper.setRenderer(c, this::comboText);
        c.setPrototypeDisplayValue("#0000  Xxxxxxxxxxxxx  Xxxxxxxxxxxx");
        return c;
    }

    /** Fills a combo with assessments, optionally only one patient's. */
    private void fillAssessments(JComboBox<Object> combo, boolean withAll, Long patientId) {
        Long keep = selectedAssessmentId(combo);
        combo.removeAllItems();
        if (withAll) combo.addItem(ALL_ASSESSMENTS);
        for (MedicalAssessment a : assessmentsById.values()) {
            if (patientId == null || a.getPatientId() == patientId) combo.addItem(a);
        }
        if (keep == null || !selectAssessment(combo, keep)) {
            if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
        }
    }

    private static Long selectedAssessmentId(JComboBox<Object> combo) {
        Object item = combo.getSelectedItem();
        return item instanceof MedicalAssessment ? ((MedicalAssessment) item).getAssessmentId() : null;
    }

    private static boolean selectAssessment(JComboBox<Object> combo, long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item instanceof MedicalAssessment && ((MedicalAssessment) item).getAssessmentId() == id) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    @Override
    protected String comboText(Object item) {
        if (item instanceof MedicalAssessment) {
            MedicalAssessment a = (MedicalAssessment) item;
            return "#" + a.getAssessmentId() + "  " + patientName(a.getPatientId()) + " – " + a.getDiagnosis();
        }
        return super.comboText(item);
    }
}
