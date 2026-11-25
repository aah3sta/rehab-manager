package ui;

/** Every screen reloads its data from the database when it's opened. */
public interface Refreshable {
    void refresh();
}
