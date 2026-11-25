package util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Reads console input and keeps asking until the value is valid,
 * so menus never crash on bad input.
 */
public final class InputHelper {
    private static final Scanner SC = new Scanner(System.in);

    private InputHelper() { }

    public static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SC.hasNextLine()) {           // input stream closed (e.g. Ctrl+D)
            System.out.println();
            System.exit(0);
        }
        return SC.nextLine().trim();
    }

    public static String readRequired(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("  ! This field is required.");
        }
    }

    /** Empty input returns the current value (used in "update" screens). */
    public static String readOptional(String prompt, String current) {
        String s = readLine(prompt + " [" + (current == null ? "" : current) + "]: ");
        return s.isEmpty() ? current : s;
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.printf("  ! Enter a whole number from %d to %d.%n", min, max);
        }
    }

    public static long readId(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                long v = Long.parseLong(s);
                if (v > 0) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  ! Enter a valid ID (a positive whole number).");
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            String s = readLine(prompt + " (YYYY-MM-DD): ");
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Use the format YYYY-MM-DD, e.g. 2026-11-24.");
            }
        }
    }

    public static LocalDate readOptionalDate(String prompt, LocalDate current) {
        while (true) {
            String s = readLine(prompt + " (YYYY-MM-DD) [" + current + "]: ");
            if (s.isEmpty()) return current;
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Use the format YYYY-MM-DD, e.g. 2026-11-24.");
            }
        }
    }

    public static boolean confirm(String prompt) {
        while (true) {
            String s = readLine(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            System.out.println("  ! Please answer y or n.");
        }
    }

    /**
     * Ask for a value and pass it to a model setter; if the setter rejects it
     * (IllegalArgumentException), show its message and ask again.
     * This keeps the validation rules in one place: the model classes.
     */
    public static void readValid(String prompt, Consumer<String> setter) {
        while (true) {
            try {
                setter.accept(readLine(prompt));
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    public static void pause() {
        readLine("\nPress Enter to continue...");
    }
}
