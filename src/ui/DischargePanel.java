package ui;

import dao.DischargePlanDAO;
import models.DischargePlan;
import models.Patient;
import ui.common.OptionalDatePicker;
import ui.common.UiHelper;

import javax.swing.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Discharge and aftercare plans, discharge / follow-up dates and upcoming follow-ups. */
public class DischargePanel extends ModulePanel<DischargePlan> {

    private final DischargePlanDAO planDAO = new DischargePlanDAO();

    // filters
    private final JComboBox<Object> patientFilter = personCombo();
    private final JCheckBox upcomingOnly = new JCheckBox("Upcoming follow-ups only");

    // form
    private final JComboBox<Object> patientBox = personCombo();
    private final JTextArea goalsArea = textArea(3);
    private final JTextArea aftercareArea = textArea(3);
    private final OptionalDatePicker dischargePicker = new OptionalDatePicker("Set");
    private final OptionalDatePicker followUpPicker = new OptionalDatePicker("Set");
    private final JTextField contactNameField = new JTextField();
    private final JTextField contactPhoneField = new JTextField();

    public DischargePanel() {
        super("Discharge and aftercare", "Recovery goals, aftercare, discharge and follow-up dates",
                new String[]{"ID", "Patient", "Status", "Discharge", "Follow-up", "Emergency relapse contact"}, null);
        model.setCellValue((p, col) -> {
            switch (col) {
                case 0: return p.getPlanId();
                case 1: return patientName(p.getPatientId());
                case 2: return p.getStatus();
                case 3: return p.getDischargeDate() == null ? "not set" : p.getDischargeDate().toString();
                case 4: return followUpText(p.getFollowUpDate());
                default: return p.getEmergencyContactName() + ", " + p.getEmergencyContactPhone();
            }
        });
        UiHelper.columnWidths(table, 40, 110, 115, 90, 160, 200);

        addTool(patientFilter);
        upcomingOnly.setOpaque(false);
        addTool(upcomingOnly);
        addNewButton("New plan");
        reloadOnChange(patientFilter);
        upcomingOnly.addActionListener(e -> reload());

        addField("Patient", patientBox);
        addField("Recovery goals", UiHelper.textScroll(goalsArea));
        addField("Aftercare instructions", UiHelper.textScroll(aftercareArea));
        addField(" ", UiHelper.pair(UiHelper.labeled("Discharge date", dischargePicker),
                UiHelper.labeled("Follow-up date", followUpPicker)));
        addField(" ", UiHelper.pair(UiHelper.labeled("Emergency relapse contact", contactNameField),
                UiHelper.labeled("Contact phone", contactPhoneField)));
        finishForm();
    }

    private static String followUpText(LocalDate d) {
        if (d == null) return "not set";
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (days < 0) return d.toString();
        return d + (days == 0 ? " (today)" : days == 1 ? " (tomorrow)" : " (in " + days + " days)");
    }

    // ---------- data ----------

    @Override
    protected void lookupsLoaded(Object extra) {
        fillPatients(patientFilter, true);
        fillPatients(patientBox, false);
    }

    @Override
    protected UiHelper.DbTask<List<DischargePlan>> query() {
        final Long patientId = selectedId(patientFilter);
        final boolean upcoming = upcomingOnly.isSelected();
        return () -> {
            if (!upcoming) return patientId != null ? planDAO.findByPatient(patientId) : planDAO.getAll();
            List<DischargePlan> list = new ArrayList<>();
            for (DischargePlan p : planDAO.findUpcomingFollowUps()) {   // soonest first
                if (patientId == null || p.getPatientId() == patientId) list.add(p);
            }
            return list;
        };
    }

    @Override
    protected void rowsLoaded(List<DischargePlan> rows) {
        summaryLabel.setText(upcomingOnly.isSelected()
                ? rows.size() + " follow-up(s) from today onwards, soonest first"
                : rows.size() + " plan(s)");
        UiHelper.status(rows.size() + " plan(s)");
    }

    @Override
    protected long idOf(DischargePlan p) { return p.getPlanId(); }

    // ---------- form ----------

    @Override
    protected void showInForm(DischargePlan p) {
        current = p;
        formTitle.setText("Plan #" + p.getPlanId() + "  ·  " + p.getStatus());
        selectById(patientBox, p.getPatientId());
        patientBox.setEnabled(false);
        goalsArea.setText(p.getRecoveryGoals());
        aftercareArea.setText(p.getAftercareInstructions());
        dischargePicker.setDate(p.getDischargeDate());
        followUpPicker.setDate(p.getFollowUpDate());
        contactNameField.setText(p.getEmergencyContactName());
        contactPhoneField.setText(p.getEmergencyContactPhone());
        saveButton.setText("Save changes");
    }

    @Override
    protected void clearForm() {
        current = null;
        formTitle.setText("New discharge and aftercare plan");
        patientBox.setEnabled(true);
        Long filtered = selectedId(patientFilter);
        if (filtered != null) selectById(patientBox, filtered);
        goalsArea.setText("");
        aftercareArea.setText("");
        dischargePicker.setDate(null);
        followUpPicker.setDate(null);
        contactNameField.setText("");
        contactPhoneField.setText("");
        saveButton.setText("Create plan");
    }

    @Override
    protected void save() {
        final DischargePlan p;
        try {
            long patientId;
            if (current != null) {
                patientId = current.getPatientId();
            } else {
                Object item = patientBox.getSelectedItem();
                if (!(item instanceof Patient)) { showError("Select a patient."); return; }
                patientId = ((Patient) item).getId();
            }
            p = new DischargePlan(current == null ? 0 : current.getPlanId(), patientId,
                    goalsArea.getText(), aftercareArea.getText(),
                    dischargePicker.getDate(), followUpPicker.getDate(),
                    contactNameField.getText(), contactPhoneField.getText(),
                    current == null ? null : current.getCreatedDate());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        final boolean isNew = current == null;
        UiHelper.runAsync(this, () -> {
            if (isNew) return planDAO.create(p) > 0;
            return planDAO.updateCarePlan(p) & planDAO.updateDates(p);  // both parts of the plan
        }, ok -> {
            current = p;
            UiHelper.status(isNew ? "Plan created with ID " + p.getPlanId() + " (" + p.getStatus() + ")"
                    : "Plan #" + p.getPlanId() + " updated (" + p.getStatus() + ")");
            reload();
        });
    }
}
