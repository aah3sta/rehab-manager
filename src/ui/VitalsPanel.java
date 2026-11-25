package ui;

import dao.VitalsDAO;
import models.Staff;
import models.Vitals;
import ui.common.UiHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Vitals: record readings, view by patient / staff / date, latest reading, alerts. */
public class VitalsPanel extends ModulePanel<Vitals> {

    private static final String NORMAL = "Normal";

    private final VitalsDAO vitalsDAO = new VitalsDAO();

    // filters
    private final JComboBox<Object> patientFilter = personCombo();
    private final JComboBox<Object> staffFilter = personCombo();
    private final JCheckBox dateCheck = new JCheckBox("On date");
    private final JSpinner dateFilter = UiHelper.dateSpinner();

    // form
    private final JComboBox<Object> patientBox = personCombo();
    private final JComboBox<Object> staffBox = personCombo();
    private final JLabel recordedLabel = new JLabel();
    private final JTextField tempField = new JTextField(6);
    private final JTextField pulseField = new JTextField(6);
    private final JTextField breathingField = new JTextField(6);
    private final JTextField glucoseField = new JTextField(6);
    private final JTextField systolicField = new JTextField(6);
    private final JTextField diastolicField = new JTextField(6);
    private final JLabel alertsLabel = new JLabel(" ");

    public VitalsPanel() {
        super("Vitals", "Temperature, pulse, breathing rate, glucose and blood pressure",
                new String[]{"ID", "Recorded", "Patient", "Temp (C)", "Pulse", "Breathing", "Glucose", "BP", "Alerts"},
                null);
        model.setCellValue((v, col) -> {
            switch (col) {
                case 0: return v.getVitalsId();
                case 1: return v.getRecordedAt() == null ? "" : v.getRecordedAt().format(TABLE_DATE);
                case 2: return patientName(v.getPatientId());
                case 3: return v.getTemperature();
                case 4: return v.getPulse();
                case 5: return v.getBreathingRate();
                case 6: return v.getGlucose();
                case 7: return v.getBloodPressure();
                default: return v.getAlerts().isEmpty() ? NORMAL : String.join(", ", v.getAlerts());
            }
        });
        table.getColumnModel().getColumn(8).setCellRenderer(new AlertRenderer());
        UiHelper.columnWidths(table, 40, 115, 110, 60, 50, 70, 60, 65, 220);

        addTool(patientFilter);
        addTool(staffFilter);
        dateCheck.setOpaque(false);
        addTool(dateCheck);
        addTool(dateFilter);
        JButton latestButton = UiHelper.button("Latest for patient");
        latestButton.addActionListener(e -> showLatest());
        addTool(latestButton);
        addNewButton("New reading");
        reloadOnChange(patientFilter);
        reloadOnChange(staffFilter);
        dateFilter.setEnabled(false);
        dateCheck.addActionListener(e -> { dateFilter.setEnabled(dateCheck.isSelected()); reload(); });
        dateFilter.addChangeListener(e -> { if (dateCheck.isSelected()) reload(); });

        addField("Patient", patientBox);
        addField("Taken by (doctor or nurse)", staffBox);
        addField("Recorded", recordedLabel);
        addField(" ", UiHelper.pair(UiHelper.labeled("Temperature (C)", tempField),
                UiHelper.labeled("Pulse (beats/min)", pulseField)));
        addField(" ", UiHelper.pair(UiHelper.labeled("Breathing (breaths/min)", breathingField),
                UiHelper.labeled("Glucose (mmol/L)", glucoseField)));
        addField(" ", UiHelper.pair(UiHelper.labeled("Systolic BP (mmHg)", systolicField),
                UiHelper.labeled("Diastolic BP (mmHg)", diastolicField)));
        alertsLabel.setForeground(UiHelper.DANGER);
        addField(" ", alertsLabel);
        finishForm();
    }

    // ---------- data ----------

    @Override
    protected void lookupsLoaded(Object extra) {
        fillPatients(patientFilter, true);
        fillStaff(staffFilter, true, Staff::canRecordVitals);
        fillPatients(patientBox, false);
        fillStaff(staffBox, false, Staff::canRecordVitals);
    }

    @Override
    protected UiHelper.DbTask<List<Vitals>> query() {
        final Long patientId = selectedId(patientFilter);
        final Long staffId = selectedId(staffFilter);
        final LocalDate date = dateCheck.isSelected() ? UiHelper.getDate(dateFilter) : null;
        return () -> vitalsDAO.search(patientId, staffId, date);
    }

    @Override
    protected void rowsLoaded(List<Vitals> rows) {
        int withAlerts = 0;
        for (Vitals v : rows) if (!v.getAlerts().isEmpty()) withAlerts++;
        summaryLabel.setText(rows.size() + " reading(s), " + withAlerts + " with alerts");
        UiHelper.status(rows.size() + " vitals record(s)");
    }

    @Override
    protected long idOf(Vitals v) { return v.getVitalsId(); }

    /** "Show the latest recorded vitals for a patient". */
    private void showLatest() {
        final Long patientId = selectedId(patientFilter);
        if (patientId == null) {
            UiHelper.showInfo(this, "Latest vitals", "Choose a patient in the filter first.");
            return;
        }
        UiHelper.runAsync(this, () -> vitalsDAO.findLatest(patientId), v -> {
            if (v == null) {
                UiHelper.showInfo(this, "Latest vitals", "No vitals recorded for " + patientName(patientId) + " yet.");
                return;
            }
            showInForm(v);
            selectRow(v.getVitalsId());
            UiHelper.showInfo(this, "Latest vitals for " + patientName(patientId),
                    v.getSummary() + "\n   Recorded by: " + staffName(v.getStaffId()));
        });
    }

    private void selectRow(long id) {
        List<Vitals> rows = model.getRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getVitalsId() == id) {
                int viewRow = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                return;
            }
        }
    }

    // ---------- form ----------

    /** Recorded vitals are read-only (the spec has no update); "New reading" starts a fresh form. */
    @Override
    protected void showInForm(Vitals v) {
        current = v;
        formTitle.setText("Vitals #" + v.getVitalsId());
        selectById(patientBox, v.getPatientId());
        if (!selectById(staffBox, v.getStaffId())) staffBox.setSelectedIndex(-1);
        recordedLabel.setText(v.getRecordedAt() == null ? "-" : v.getRecordedAt().format(DATE_TIME)
                + "  by " + staffName(v.getStaffId()));
        tempField.setText(String.valueOf(v.getTemperature()));
        pulseField.setText(String.valueOf(v.getPulse()));
        breathingField.setText(String.valueOf(v.getBreathingRate()));
        glucoseField.setText(String.valueOf(v.getGlucose()));
        systolicField.setText(String.valueOf(v.getSystolic()));
        diastolicField.setText(String.valueOf(v.getDiastolic()));
        List<String> alerts = v.getAlerts();
        alertsLabel.setText(alerts.isEmpty() ? " " : UiHelper.wrap("Alerts: " + String.join(", ", alerts)));
        setFormEditable(false);
    }

    @Override
    protected void clearForm() {
        current = null;
        formTitle.setText("New vitals reading");
        Long filtered = selectedId(patientFilter);
        if (filtered != null) selectById(patientBox, filtered);
        recordedLabel.setText("Now (set when saved)");
        for (JTextField f : new JTextField[]{tempField, pulseField, breathingField, glucoseField,
                systolicField, diastolicField}) f.setText("");
        alertsLabel.setText(" ");
        setFormEditable(true);
    }

    private void setFormEditable(boolean on) {
        setEditable(on, patientBox, staffBox, tempField, pulseField, breathingField, glucoseField,
                systolicField, diastolicField);
        saveButton.setVisible(on);
    }

    @Override
    protected void save() {
        Object p = patientBox.getSelectedItem();
        Object s = staffBox.getSelectedItem();
        if (!(p instanceof models.Patient)) { showError("Select a patient."); return; }
        if (!(s instanceof Staff)) { showError("Select the doctor or nurse who took the readings."); return; }

        final Vitals v;
        try {
            v = new Vitals(0, ((models.Patient) p).getId(), ((Staff) s).getId(), null,
                    number(tempField, "Temperature"), whole(pulseField, "Pulse"),
                    whole(breathingField, "Breathing rate"), number(glucoseField, "Glucose"),
                    whole(systolicField, "Systolic pressure"), whole(diastolicField, "Diastolic pressure"));
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        UiHelper.runAsync(this, () -> vitalsDAO.record(v), id -> {
            current = v;
            List<String> alerts = v.getAlerts();
            UiHelper.status("Vitals recorded with ID " + id + (alerts.isEmpty() ? "" : " – " + alerts.size() + " alert(s)"));
            if (!alerts.isEmpty()) {
                UiHelper.showInfo(this, "Readings need attention",
                        patientName(v.getPatientId()) + ":\n• " + String.join("\n• ", alerts));
            }
            reload();
        });
    }

    private static double number(JTextField f, String field) {
        return util.InputHelper.parseDouble(f.getText(), field);
    }

    private static int whole(JTextField f, String field) {
        return util.InputHelper.parseInt(f.getText(), field);
    }

    /** Red text for readings outside the normal range. */
    private static class AlertRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            c.setForeground(NORMAL.equals(value) ? UiHelper.MUTED : UiHelper.DANGER);
            return c;
        }
    }
}
