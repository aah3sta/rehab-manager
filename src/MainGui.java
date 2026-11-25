import database.DatabaseConnection;
import ui.DashboardFrame;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;

/** Starts the Swing dashboard. (Main.java still starts the console version.) */
public class MainGui {

    public static void main(String[] args) {
        installLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            String dbName;
            try (Connection conn = DatabaseConnection.getConnection()) {
                dbName = conn.getCatalog() != null ? conn.getCatalog() : "database";
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Couldn't connect to the database.\n\n" + e.getMessage()
                                + "\n\nCheck that MySQL is running, sql/schema.sql has been run,\n"
                                + "and the URL, user and password in DatabaseConnection are correct.",
                        "Database connection", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            DashboardFrame frame = new DashboardFrame(dbName);
            frame.setVisible(true);
            frame.showScreen("Dashboard");
        });
    }

    /** Uses FlatLaf if its jar is in lib/, otherwise Nimbus, otherwise the default. */
    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            return;
        } catch (Exception ignored) { }
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) { }
    }
}
