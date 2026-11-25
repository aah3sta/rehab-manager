package ui;

import dao.PatientDAO;
import dao.StaffDAO;
import models.Patient;
import models.Person;
import models.Staff;
import ui.common.UiHelper;
import ui.tables.ListTableModel;
import util.Authorization;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Shared layout and behaviour for the module 4-8 screens:
 * title, filter toolbar, table on the left, form on the right.
 *
 * A subclass, in its constructor:
 *   1. adds filters with addTool(...) and form fields with addField(...)
 *   2. calls finishForm()
 * and implements query(), idOf(), showInForm(), clearForm() and save().
 */
public abstract class ModulePanel<T> extends JPanel implements Refreshable {

    protected static final String ALL_PATIENTS = "All patients";
    protected static final String ALL_STAFF = "All staff";
    protected static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** Shorter date for narrow table columns; the form shows the full date. */
    protected static final DateTimeFormatter TABLE_DATE = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private final PatientDAO patientDAO = new PatientDAO();
    private final StaffDAO staffDAO = new StaffDAO();

    /** Lookups so tables and combos can show names instead of IDs. */
    protected final Map<Long, Patient> patientsById = new LinkedHashMap<>();
    protected final Map<Long, Staff> staffById = new LinkedHashMap<>();

    protected final ListTableModel<T> model;
    protected final JTable table;

    private final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    /** One line under the toolbar for totals and hints. */
    protected final JLabel summaryLabel = UiHelper.muted(" ");

    private final JPanel form = new JPanel(new GridBagLayout());
    private int formRow = 0;
    protected final JLabel formTitle = UiHelper.subheading(" ");
    protected final JLabel errorLabel = UiHelper.errorLabel();
    protected final JButton saveButton = UiHelper.primaryButton("Save");
    protected final JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    /** Record shown in the form; null while creating a new one. */
    protected T current;
    /** True while combos are being refilled, so their listeners don't reload. */
    protected boolean updatingCombos;

    protected ModulePanel(String title, String subtitle, String[] columns, BiFunction<T, Integer, Object> cells) {
        super(new BorderLayout(0, 12));
        setBorder(UiHelper.padding(20));
        setBackground(Color.WHITE);
        model = new ListTableModel<>(columns, cells);
        table = UiHelper.table(model);

        // header: title, subtitle, toolbar, summary line
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiHelper.heading(title));
        titles.add(UiHelper.muted(subtitle));
        header.add(titles, BorderLayout.NORTH);
        toolbar.setOpaque(false);
        JPanel tools = new JPanel(new BorderLayout(0, 8));
        tools.setOpaque(false);
        tools.add(toolbar, BorderLayout.NORTH);
        tools.add(summaryLabel, BorderLayout.SOUTH);
        header.add(tools, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // table | form
        form.setBorder(UiHelper.card());
        form.setBackground(Color.WHITE);
        formButtons.setOpaque(false);
        formButtons.add(saveButton);
        UiHelper.addField(form, formRow++, " ", formTitle);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setMinimumSize(new Dimension(320, 0));
        formScroll.setPreferredSize(new Dimension(380, 0)); // leave the table most of the width
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, UiHelper.scroll(table), formScroll);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);
        UiHelper.narrowColumn(table, 0);

        saveButton.addActionListener(e -> {
            errorLabel.setText(" ");
            save();
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                errorLabel.setText(" ");
                showInForm(model.getRow(table.convertRowIndexToModel(viewRow)));
            }
        });
    }

    // ---------- what each screen provides ----------

    /** Builds the query from the current filters (called on the Swing thread; runs in the background). */
    protected abstract UiHelper.DbTask<List<T>> query();

    protected abstract long idOf(T row);

    protected abstract void showInForm(T row);

    protected abstract void clearForm();

    protected abstract void save();

    /** Extra lookups a screen needs (e.g. assessments), loaded in the background. */
    protected Object loadExtraLookups() throws Exception { return null; }

    /** Called on the Swing thread after lookups load: refill combos here. */
    protected void lookupsLoaded(Object extra) { }

    /** Called after the table reloads: update summaryLabel / status here. */
    protected void rowsLoaded(List<T> rows) {
        UiHelper.status(rows.size() + " record(s)");
    }

    // ---------- loading ----------

    @Override
    public void refresh() {
        UiHelper.runAsync(this, () -> new Lookups(patientDAO.getAll(), staffDAO.getAll(), loadExtraLookups()), l -> {
            patientsById.clear();
            for (Patient p : l.patients) patientsById.put(p.getId(), p);
            staffById.clear();
            for (Staff s : l.staff) staffById.put(s.getId(), s);
            updatingCombos = true;
            try {
                lookupsLoaded(l.extra);
            } finally {
                updatingCombos = false;
            }
            reload();
        });
    }

    /** Re-runs the query with the current filters. */
    protected void reload() {
        UiHelper.runAsync(this, query(), rows -> {
            model.setRows(rows);
            reselectCurrent();
            rowsLoaded(rows);
        });
    }

    private static class Lookups {
        final List<Patient> patients;
        final List<Staff> staff;
        final Object extra;
        Lookups(List<Patient> patients, List<Staff> staff, Object extra) {
            this.patients = patients;
            this.staff = staff;
            this.extra = extra;
        }
    }

    private void reselectCurrent() {
        if (current == null) return;
        long id = idOf(current);
        List<T> rows = model.getRows();
        for (int i = 0; i < rows.size(); i++) {
            if (idOf(rows.get(i)) == id) {
                int viewRow = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(viewRow, viewRow);
                return;
            }
        }
    }

    // ---------- toolbar ----------

    protected void addTool(Component c) {
        if (toolbar.getComponentCount() > 0) toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(c);
    }

    /** "New ..." button: clears the selection and the form. */
    protected void addNewButton(String text) {
        JButton b = UiHelper.button(text);
        b.addActionListener(e -> {
            table.clearSelection();
            errorLabel.setText(" ");
            clearForm();
        });
        addTool(b);
    }

    /** Reload whenever this filter combo changes (but not while it's being refilled). */
    protected void reloadOnChange(JComboBox<?> combo) {
        combo.addActionListener(e -> { if (!updatingCombos) reload(); });
    }

    // ---------- form ----------

    protected void addField(String label, JComponent field) {
        UiHelper.addField(form, formRow++, label, field);
    }

    /** Adds the error line and buttons; call once at the end of the subclass constructor. */
    protected void finishForm() {
        addField(" ", errorLabel);
        addField(" ", formButtons);
        UiHelper.addFiller(form, formRow);
        clearForm();
    }

    protected void showError(String message) {
        errorLabel.setText(UiHelper.wrap(message));
    }

    protected static JTextArea textArea(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    protected static void setEditable(boolean on, JComponent... fields) {
        for (JComponent f : fields) {
            if (f instanceof JTextArea) ((JTextArea) f).setEditable(on);
            else if (f instanceof JTextField) ((JTextField) f).setEditable(on);
            else f.setEnabled(on);
        }
    }

    // ---------- combos of patients / staff ----------

    protected JComboBox<Object> personCombo() {
        JComboBox<Object> c = new JComboBox<>();
        UiHelper.setRenderer(c, this::comboText);
        c.setPrototypeDisplayValue("#0000  Xxxxxxxxxxxxxxxxxxxx");
        return c;
    }

    /** Refills a patient combo, keeping the selected patient. */
    protected void fillPatients(JComboBox<Object> combo, boolean withAll) {
        Long keep = selectedId(combo);
        combo.removeAllItems();
        if (withAll) combo.addItem(ALL_PATIENTS);
        for (Patient p : patientsById.values()) combo.addItem(p);
        restore(combo, keep);
    }

    /** Refills a staff combo with the staff who pass the filter (e.g. Staff::canRecordVitals). */
    protected void fillStaff(JComboBox<Object> combo, boolean withAll, Predicate<Staff> allowed) {
        Long keep = selectedId(combo);
        combo.removeAllItems();
        if (withAll) combo.addItem(ALL_STAFF);
        for (Staff s : staffById.values()) if (allowed.test(s)) combo.addItem(s);
        restore(combo, keep);
    }

    private void restore(JComboBox<Object> combo, Long id) {
        if (id != null && selectById(combo, id)) return;
        if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
    }

    protected static Long selectedId(JComboBox<Object> combo) {
        Object item = combo.getSelectedItem();
        return item instanceof Person ? ((Person) item).getId() : null;
    }

    protected static boolean selectById(JComboBox<Object> combo, long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item instanceof Person && ((Person) item).getId() == id) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /** How combo items are shown; subclasses can extend it for their own types. */
    protected String comboText(Object item) {
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

    protected String patientName(long id) {
        Patient p = patientsById.get(id);
        return p == null ? "#" + id : p.getFullName();
    }

    protected String staffName(long id) {
        Staff s = staffById.get(id);
        return s == null ? "#" + id : s.getFullName();
    }

    // ---------- "update when authorized" ----------

    /** Asks who is editing; true if they own the record or are a Doctor. */
    protected boolean authorize(long ownerStaffId) {
        JComboBox<Object> who = personCombo();
        for (Staff s : staffById.values()) who.addItem(s);
        Object[] message = {"Who is making this change?", who};
        int choice = JOptionPane.showConfirmDialog(this, message, "Confirm your identity",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return false;
        if (!Authorization.canEdit((Staff) who.getSelectedItem(), ownerStaffId)) {
            UiHelper.showInfo(this, "Not authorized", Authorization.deniedMessage(staffName(ownerStaffId)));
            return false;
        }
        return true;
    }

    /** Copies a list so lambdas can filter it safely. */
    protected static <E> List<E> copy(List<E> list) {
        return new ArrayList<>(list);
    }
}
