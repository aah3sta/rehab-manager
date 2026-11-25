package ui;

import dao.TherapySessionDAO;
import models.Patient;
import models.Staff;
import models.TherapySession;
import ui.common.UiHelper;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Therapy sessions: record, view by patient / staff / date, total therapy time per patient. */
public class TherapyPanel extends ModulePanel<TherapySession> {

    private final TherapySessionDAO sessionDAO = new TherapySessionDAO();

    // filters
    private final JComboBox<Object> patientFilter = personCombo();
    private final JComboBox<Object> staffFilter = personCombo();
    private final JCheckBox dateCheck = new JCheckBox("On date");
    private final JSpinner dateFilter = UiHelper.dateSpinner();

    // form
    private final JComboBox<Object> patientBox = personCombo();
    private final JComboBox<Object> therapistBox = personCombo();
    private final JSpinner dateTimeSpinner = UiHelper.dateTimeSpinner();
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(60, 5, 480, 5));
    private final JTextArea notesArea = textArea(4);

    /** Filled by query() when a patient is selected: SUM from the database. */
    private volatile long totalMinutes = -1;

    public TherapyPanel() {
        super("Therapy sessions", "Rehabilitation sessions, history and total therapy time",
                new String[]{"ID", "Date", "Patient", "Therapist", "Duration", "Notes"}, null);
        model.setCellValue((s, col) -> {
            switch (col) {
                case 0: return s.getSessionId();
                case 1: return s.getSessionDate().format(TABLE_DATE);
                case 2: return patientName(s.getPatientId());
                case 3: return staffName(s.getStaffId());
                case 4: return TherapySession.formatMinutes(s.getDurationMinutes());
                default: return s.getNotes();
            }
        });
        UiHelper.columnWidths(table, 40, 115, 110, 110, 85, 220);

        addTool(patientFilter);
        addTool(staffFilter);
        dateCheck.setOpaque(false);
        addTool(dateCheck);
        addTool(dateFilter);
        addNewButton("New session");
        reloadOnChange(patientFilter);
        reloadOnChange(staffFilter);
        dateFilter.setEnabled(false);
        dateCheck.addActionListener(e -> { dateFilter.setEnabled(dateCheck.isSelected()); reload(); });
        dateFilter.addChangeListener(e -> { if (dateCheck.isSelected()) reload(); });

        addField("Patient", patientBox);
        addField("Therapist", therapistBox);
        addField(" ", UiHelper.pair(UiHelper.labeled("Date and time", dateTimeSpinner),
                UiHelper.labeled("Duration (minutes)", durationSpinner)));
        addField("Session notes", UiHelper.textScroll(notesArea));
        finishForm();
    }

    // ---------- data ----------

    @Override
    protected void lookupsLoaded(Object extra) {
        fillPatients(patientFilter, true);
        fillStaff(staffFilter, true, Staff::canConductTherapy);
        fillPatients(patientBox, false);
        fillStaff(therapistBox, false, Staff::canConductTherapy);
    }

    @Override
    protected UiHelper.DbTask<List<TherapySession>> query() {
        final Long patientId = selectedId(patientFilter);
        final Long staffId = selectedId(staffFilter);
        final LocalDate date = dateCheck.isSelected() ? UiHelper.getDate(dateFilter) : null;
        return () -> {
            totalMinutes = patientId == null ? -1 : sessionDAO.totalMinutes(patientId);
            return sessionDAO.search(patientId, staffId, date);
        };
    }

    @Override
    protected void rowsLoaded(List<TherapySession> rows) {
        Long patientId = selectedId(patientFilter);
        if (patientId != null && totalMinutes >= 0) {
            summaryLabel.setText("Total recorded therapy time for " + patientName(patientId) + ": "
                    + TherapySession.formatMinutes(totalMinutes) + " (" + totalMinutes + " minutes, all sessions)");
        } else {
            summaryLabel.setText(rows.size() + " session(s). Pick a patient to see their total therapy time.");
        }
        UiHelper.status(rows.size() + " therapy session(s)");
    }

    @Override
    protected long idOf(TherapySession s) { return s.getSessionId(); }

    // ---------- form ----------

    /** Recorded sessions are read-only (the spec has no update). */
    @Override
    protected void showInForm(TherapySession s) {
        current = s;
        formTitle.setText("Session #" + s.getSessionId());
        selectById(patientBox, s.getPatientId());
        if (!selectById(therapistBox, s.getStaffId())) therapistBox.setSelectedIndex(-1);
        UiHelper.setDateTime(dateTimeSpinner, s.getSessionDate());
        durationSpinner.setValue(s.getDurationMinutes());
        notesArea.setText(s.getNotes());
        setFormEditable(false);
    }

    @Override
    protected void clearForm() {
        current = null;
        formTitle.setText("New therapy session");
        Long filtered = selectedId(patientFilter);
        if (filtered != null) selectById(patientBox, filtered);
        UiHelper.setDateTime(dateTimeSpinner, LocalDateTime.now().withSecond(0).withNano(0));
        durationSpinner.setValue(60);
        notesArea.setText("");
        setFormEditable(true);
    }

    private void setFormEditable(boolean on) {
        setEditable(on, patientBox, therapistBox, dateTimeSpinner, durationSpinner, notesArea);
        saveButton.setVisible(on);
    }

    @Override
    protected void save() {
        Object p = patientBox.getSelectedItem();
        Object s = therapistBox.getSelectedItem();
        if (!(p instanceof Patient)) { showError("Select a patient."); return; }
        if (!(s instanceof Staff)) { showError("Select the therapist who ran the session."); return; }

        final TherapySession session;
        try {
            session = new TherapySession(0, ((Patient) p).getId(), ((Staff) s).getId(),
                    UiHelper.getDateTime(dateTimeSpinner), (Integer) durationSpinner.getValue(), notesArea.getText());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        UiHelper.runAsync(this, () -> sessionDAO.record(session), id -> {
            current = session;
            UiHelper.status("Therapy session recorded with ID " + id);
            reload();
        });
    }
}
