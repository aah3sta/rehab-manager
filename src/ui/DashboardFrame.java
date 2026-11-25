package ui;

import ui.common.UiHelper;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: header, sidebar navigation, screen area (CardLayout) and status bar. */
public class DashboardFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Refreshable> screens = new LinkedHashMap<>();
    private final Map<String, JToggleButton> navItems = new LinkedHashMap<>();

    /** Section heading shown above the first item of each group. */
    private static final Map<String, String> SECTIONS = new LinkedHashMap<>();
    static {
        SECTIONS.put("Dashboard", "Overview");
        SECTIONS.put("Patients", "People");
        SECTIONS.put("Assessments", "Clinical care");
        SECTIONS.put("Discharge", "Aftercare");
    }

    public DashboardFrame(String databaseName) {
        super("Rehabilitation Center Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildContent(databaseName));
        setSize(1280, 780);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
    }

    /** Built separately from the frame so it can also be tested without a window. */
    public JPanel buildContent(String databaseName) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        root.add(buildHeader(databaseName), BorderLayout.NORTH);

        content.setBackground(Color.WHITE);
        addScreen("Dashboard", new HomePanel());
        addScreen("Patients", new PatientPanel());
        addScreen("Staff", new StaffPanel());
        addScreen("Assessments", new AssessmentPanel());
        addScreen("Lab tests", new LabTestPanel());
        addScreen("Vitals", new VitalsPanel());
        addScreen("Therapy", new TherapyPanel());
        addScreen("Medication", new MedicationPanel());
        addScreen("Discharge", new DischargePanel());

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    private void addScreen(String name, JPanel screen) {
        screen.setBackground(Color.WHITE);
        content.add(screen, name);
        screens.put(name, (Refreshable) screen);
    }

    private JComponent buildHeader(String databaseName) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiHelper.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        JLabel title = new JLabel("Rehabilitation center management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(UiHelper.TEXT);
        header.add(title, BorderLayout.WEST);
        header.add(UiHelper.muted("Connected to " + databaseName), BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UiHelper.BORDER),
                BorderFactory.createEmptyBorder(14, 10, 14, 10)));
        sidebar.setPreferredSize(new Dimension(180, 0));

        ButtonGroup group = new ButtonGroup();
        boolean first = true;
        for (final String name : screens.keySet()) {
            String section = SECTIONS.get(name);
            if (section != null) {
                JLabel heading = UiHelper.muted(section.toUpperCase());
                heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
                heading.setBorder(BorderFactory.createEmptyBorder(first ? 0 : 12, 12, 4, 0));
                heading.setAlignmentX(Component.LEFT_ALIGNMENT);
                sidebar.add(heading);
            }
            final JToggleButton item = navButton(name);
            group.add(item);
            navItems.put(name, item);
            item.addActionListener(e -> showScreen(name));
            sidebar.add(item);
            sidebar.add(Box.createVerticalStrut(4));
            if (first) { item.setSelected(true); first = false; }
        }
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    /** Flat sidebar item; selected item gets the accent tint. */
    private JToggleButton navButton(String text) {
        final JToggleButton b = new JToggleButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(b.getFont().deriveFont(14f));
        Runnable restyle = () -> {
            b.setBackground(b.isSelected() ? UiHelper.ACCENT_BG : Color.WHITE);
            b.setForeground(b.isSelected() ? UiHelper.ACCENT : UiHelper.TEXT);
        };
        b.addItemListener(e -> restyle.run());
        restyle.run();
        return b;
    }

    private JComponent buildStatusBar() {
        JLabel status = UiHelper.muted("Ready");
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiHelper.BORDER),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)));
        status.setOpaque(true);
        status.setBackground(UiHelper.SURFACE);
        UiHelper.setStatusLabel(status);
        return status;
    }

    public void showScreen(String name) {
        navItems.get(name).setSelected(true);
        cards.show(content, name);
        screens.get(name).refresh();
    }
}
