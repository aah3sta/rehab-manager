package ui.common;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/** A date that may be "not set": a checkbox switches the date spinner on and off. */
public class OptionalDatePicker extends JPanel {
    private final JCheckBox enabled;
    private final JSpinner spinner = UiHelper.dateSpinner();

    public OptionalDatePicker(String checkboxText) {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        enabled = new JCheckBox(checkboxText);
        enabled.setOpaque(false);
        enabled.addActionListener(e -> spinner.setEnabled(enabled.isSelected()));
        add(enabled, BorderLayout.WEST);
        add(spinner, BorderLayout.CENTER);
        setDate(null);
    }

    /** null when unchecked. */
    public LocalDate getDate() {
        return enabled.isSelected() ? UiHelper.getDate(spinner) : null;
    }

    public void setDate(LocalDate date) {
        enabled.setSelected(date != null);
        spinner.setEnabled(date != null && isEnabled());
        UiHelper.setDate(spinner, date != null ? date : LocalDate.now());
    }

    @Override
    public void setEnabled(boolean on) {
        super.setEnabled(on);
        enabled.setEnabled(on);
        spinner.setEnabled(on && enabled.isSelected());
    }
}
