package ui.tables;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * One reusable table model for every screen.
 * Each screen gives it the column names and a function that reads
 * column N from a row object (Patient, Staff, MedicalAssessment).
 */
public class ListTableModel<T> extends AbstractTableModel {
    private final String[] columns;
    private final BiFunction<T, Integer, Object> cellValue;
    private List<T> rows = new ArrayList<>();

    public ListTableModel(String[] columns, BiFunction<T, Integer, Object> cellValue) {
        this.columns = columns;
        this.cellValue = cellValue;
    }

    public void setRows(List<T> newRows) {
        this.rows = new ArrayList<>(newRows);
        fireTableDataChanged();
    }

    public T getRow(int modelIndex) { return rows.get(modelIndex); }

    public List<T> getRows() { return rows; }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int col) { return columns[col]; }
    @Override public boolean isCellEditable(int row, int col) { return false; }

    @Override
    public Object getValueAt(int row, int col) {
        return cellValue.apply(rows.get(row), col);
    }

    /** Lets numbers sort as numbers (2 before 10), not as text. */
    @Override
    public Class<?> getColumnClass(int col) {
        if (rows.isEmpty()) return Object.class;
        Object v = getValueAt(0, col);
        return v == null ? Object.class : v.getClass();
    }
}
