package ui.common;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

/** Shared look, layout helpers and background database calls for all screens. */
public final class UiHelper {

    public static final Color TEXT       = new Color(0x1D2939);
    public static final Color MUTED      = new Color(0x667085);
    public static final Color BORDER     = new Color(0xE4E7EC);
    public static final Color SURFACE    = new Color(0xF8F9FB);
    public static final Color ACCENT     = new Color(0x1E5EBF);
    public static final Color ACCENT_BG  = new Color(0xE8F0FC);
    public static final Color DANGER     = new Color(0xB42318);

    private static JLabel statusLabel;

    private UiHelper() { }

    // ---------- background work ----------

    /** A database call that may throw. */
    public interface DbTask<T> {
        T run() throws Exception;
    }

    /**
     * Runs a DAO call on a background thread (so the window never freezes)
     * and hands the result back on the Swing thread. Errors become a dialog.
     */
    public static <T> void runAsync(final Component parent, final DbTask<T> task, final Consumer<T> onSuccess) {
        final Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (window != null) window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                if (window != null) window.setCursor(Cursor.getDefaultCursor());
                try {
                    T result = get();
                    if (onSuccess != null) onSuccess.accept(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(parent, friendlyMessage(e.getCause()));
                }
            }
        }.execute();
    }

    public static String friendlyMessage(Throwable t) {
        if (t instanceof IllegalArgumentException) return t.getMessage();
        if (t instanceof SQLIntegrityConstraintViolationException)
            return "This record is linked to other records, so the change was refused.";
        if (t instanceof SQLException) return "Database error: " + t.getMessage();
        return "Unexpected error: " + t;
    }

    // ---------- messages ----------

    public static void setStatusLabel(JLabel label) { statusLabel = label; }

    public static void status(String message) {
        if (statusLabel != null) statusLabel.setText(message);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ---------- components ----------

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel subheading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    public static JLabel errorLabel() {
        JLabel l = new JLabel(" ");
        l.setForeground(DANGER);
        return l;
    }

    public static Border padding(int all) {
        return BorderFactory.createEmptyBorder(all, all, all, all);
    }

    /** White panel with a hairline border, used for the form and table areas. */
    public static Border card() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), padding(14));
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        return b;
    }

    public static JButton primaryButton(String text) {
        JButton b = button(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = button(text);
        b.setForeground(DANGER);
        return b;
    }

    /** Standard table look: taller rows, single selection, sortable columns. */
    public static JTable table(javax.swing.table.TableModel model) {
        JTable t = new JTable(model) {
            /** Hovering a cell shows its full text, in case the column is too narrow. */
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint()), col = columnAtPoint(e.getPoint());
                if (row < 0 || col < 0) return null;
                Object v = getValueAt(row, col);
                return v == null || v.toString().isEmpty() ? null : v.toString();
            }
        };
        t.setRowHeight(28);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDER);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.setSelectionBackground(ACCENT_BG);
        t.setSelectionForeground(TEXT);
        return t;
    }

    /** Scroll pane that keeps a text area at its full row height inside GridBag forms. */
    public static JScrollPane textScroll(JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        int h = area.getPreferredSize().height + 6;
        sp.setPreferredSize(new Dimension(200, h));
        sp.setMinimumSize(new Dimension(100, h));
        return sp;
    }

    /** Preferred column widths, in order; columns share any extra space in this ratio. */
    public static void columnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Keeps a short numeric ID column narrow. */
    public static void narrowColumn(JTable table, int col) {
        table.getColumnModel().getColumn(col).setMaxWidth(60);
        table.getColumnModel().getColumn(col).setPreferredWidth(50);
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        return sp;
    }

    /** Text field with grey hint text shown while it's empty. */
    public static JTextField hintField(final String hint, int columns) {
        JTextField f = new JTextField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(MUTED);
                    Insets in = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, in.left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { f.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { f.repaint(); }
        });
        return f;
    }

    // ---------- forms ----------

    /** Adds "label above field" to a GridBag form, one per row. */
    public static void addField(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row * 2;
        c.anchor = GridBagConstraints.WEST;
        boolean hasLabel = !label.trim().isEmpty();
        if (hasLabel) {
            c.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 0);
            form.add(muted(label), c);
        }

        c.gridy = row * 2 + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        // Unlabelled rows (title, error line, buttons) just get a small gap
        c.insets = new Insets(hasLabel || row == 0 ? 0 : 10, 0, 0, 0);
        form.add(field, c);
    }

    /** Pushes form rows to the top. */
    public static void addFiller(JPanel form, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row * 2;
        c.weighty = 1;
        form.add(Box.createVerticalGlue(), c);
    }

    // ---------- dates ----------

    public static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        return s;
    }

    public static LocalDate getDate(JSpinner spinner) {
        Date d = (Date) spinner.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner spinner, LocalDate date) {
        spinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public static JSpinner dateTimeSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd HH:mm"));
        return s;
    }

    public static java.time.LocalDateTime getDateTime(JSpinner spinner) {
        Date d = (Date) spinner.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0);
    }

    public static void setDateTime(JSpinner spinner, java.time.LocalDateTime t) {
        spinner.setValue(Date.from(t.atZone(ZoneId.systemDefault()).toInstant()));
    }

    /** Small "label above field" block, for putting two fields side by side. */
    public static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(muted(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    public static JPanel pair(JComponent left, JComponent right) {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0));
        p.setOpaque(false);
        p.add(left);
        p.add(right);
        return p;
    }

    // ---------- combos ----------

    /** Shows each combo item through a function, e.g. a Patient as "#3  Faith Njeri". */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setRenderer(JComboBox combo, final Function<Object, String> toText) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                          boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, toText.apply(value), index, selected, focus);
            }
        });
    }

    /** Wraps long messages so they don't stretch the form sideways. */
    public static String wrap(String message) {
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:230px'>" + safe + "</div></html>";
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
