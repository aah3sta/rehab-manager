package ui;

import dao.DashboardDAO;
import dao.MedicalAssessmentDAO;
import dao.PatientDAO;
import dao.StaffDAO;
import dao.DischargePlanDAO;
import models.DischargePlan;
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

/** Home screen: four counts and today's assessments. */
public class HomePanel extends JPanel implements Refreshable {

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final MedicalAssessmentDAO assessmentDAO = new MedicalAssessmentDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final DischargePlanDAO planDAO = new DischargePlanDAO();

    private final JLabel patientsValue = statValue();
    private final JLabel staffValue = statValue();
    private final JLabel assessmentsValue = statValue();
    private final JLabel todayValue = statValue();
    private final JLabel followUpValue = statValue();

    private final ListTableModel<DischargePlan> followUpModel = new ListTableModel<>(
            new String[]{"Follow-up", "Patient", "Emergency relapse contact"},
            (p, col) -> {
                switch (col) {
                    case 0: return p.getFollowUpDate().toString();
                    case 1: return nameOf(p.getPatientId(), true);
                    default: return p.getEmergencyContactName() + ", " + p.getEmergencyContactPhone();
                }
            });

    private final ListTableModel<MedicalAssessment> todayModel = new ListTableModel<>(
            new String[]{"Time", "Patient", "Conducted by", "Diagnosis"},
            (a, col) -> {
                switch (col) {
                    case 0: return a.getAssessmentDate() == null ? ""
                            : a.getAssessmentDate().format(DateTimeFormatter.ofPattern("HH:mm"));
                    case 1: return nameOf(a.getPatientId(), true);
                    case 2: return nameOf(a.getStaffId(), false);
                    default: return a.getDiagnosis();
                }
            });

    private final Map<Long, String> patientNames = new HashMap<>();
    private final Map<Long, String> staffNames = new HashMap<>();

    public HomePanel() {
        super(new BorderLayout(0, 16));
        setBorder(UiHelper.padding(20));

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiHelper.heading("Dashboard"));
        titles.add(UiHelper.muted("Overview for " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))));

        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("Patients", patientsValue));
        stats.add(statCard("Staff", staffValue));
        stats.add(statCard("Assessments", assessmentsValue));
        stats.add(statCard("Assessments today", todayValue));
        stats.add(statCard("Upcoming follow-ups", followUpValue));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(titles, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(1, 2, 16, 0));
        tables.setOpaque(false);
        tables.add(section("Today's assessments", UiHelper.table(todayModel)));
        tables.add(section("Upcoming follow-ups", UiHelper.table(followUpModel)));
        add(tables, BorderLayout.CENTER);
    }

    private static JPanel section(String title, JTable table) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(UiHelper.subheading(title), BorderLayout.NORTH);
        p.add(UiHelper.scroll(table), BorderLayout.CENTER);
        return p;
    }

    private static JLabel statValue() {
        JLabel l = new JLabel("–");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 26f));
        l.setForeground(UiHelper.TEXT);
        return l;
    }

    private static JPanel statCard(String label, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(UiHelper.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiHelper.BORDER), UiHelper.padding(14)));
        card.add(UiHelper.muted(label), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        final LocalDate today = LocalDate.now();
        UiHelper.runAsync(this, () -> {
            Snapshot s = new Snapshot();
            s.patients = dashboardDAO.countPatients();
            s.staff = dashboardDAO.countStaff();
            s.assessments = dashboardDAO.countAssessments();
            s.today = assessmentDAO.search(null, null, today);
            s.patientList = patientDAO.getAll();
            s.staffList = staffDAO.getAll();
            s.followUps = planDAO.findUpcomingFollowUps();
            return s;
        }, s -> {
            patientsValue.setText(String.valueOf(s.patients));
            staffValue.setText(String.valueOf(s.staff));
            assessmentsValue.setText(String.valueOf(s.assessments));
            todayValue.setText(String.valueOf(s.today.size()));
            followUpValue.setText(String.valueOf(s.followUps.size()));
            patientNames.clear();
            for (Patient p : s.patientList) patientNames.put(p.getId(), p.getFullName());
            staffNames.clear();
            for (Staff st : s.staffList) staffNames.put(st.getId(), st.getFullName());
            todayModel.setRows(s.today);
            followUpModel.setRows(s.followUps);
            UiHelper.status("Dashboard updated");
        });
    }

    private static class Snapshot {
        int patients, staff, assessments;
        List<MedicalAssessment> today;
        List<Patient> patientList;
        List<Staff> staffList;
        List<DischargePlan> followUps;
    }

    private String nameOf(long id, boolean patient) {
        String n = (patient ? patientNames : staffNames).get(id);
        return n == null ? "#" + id : n;
    }
}
